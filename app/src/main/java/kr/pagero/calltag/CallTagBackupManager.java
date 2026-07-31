package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Creates and restores CallTag-only encrypted backup packages.
 * This is intentionally not a CSV/XLSX/general-purpose data export.
 */
public final class CallTagBackupManager {
    private static final byte[] MAGIC = new byte[]{'C', 'T', 'B', 'K'};
    private static final int FORMAT_VERSION = 1;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final long MAX_EXPANDED_BYTES = 1024L * 1024L * 1024L;
    private static final int BUFFER_SIZE = 32 * 1024;

    private static final String STATUS_PREFS = "calltag_backup_status";
    private static final String STATUS_SUMMARY = "last_summary";
    private static final String STATUS_TIME = "last_time";

    private static final String[] BACKUP_PREFS = new String[]{
            "calltag_settings",
            "calltag_message_automation",
            "calltag_message_templates_v1",
            "calltag_message_exclusions",
            "calltag_task_message_links_v1"
    };

    private static final Object LOCK = new Object();

    private CallTagBackupManager() {}

    public static BackupResult createBackup(Context context, Uri target, char[] password)
            throws Exception {
        requirePassword(password);
        if (context == null || target == null) {
            throw new IllegalArgumentException("백업 파일 위치를 선택해주세요.");
        }
        synchronized (LOCK) {
            Context app = context.getApplicationContext();
            ensureNoSending(app, "발송 중인 문자가 있어 백업을 시작할 수 없습니다.");
            boolean monitorEnabled = SettingsStore.isMonitorEnabled(app);
            app.stopService(new Intent(app, CallMonitorService.class));
            File stage = new File(app.getCacheDir(), "calltag-backup-stage-" + UUID.randomUUID());
            try {
                SnapshotStats stats = snapshotCurrentData(app, stage);
                JSONObject manifest = buildManifest(app, stage, stats);
                writeUtf8(new File(stage, "manifest.json"), manifest.toString());
                encryptDirectory(app, stage, target, password);
                BackupResult result = new BackupResult(
                        manifest.optLong("createdAt", System.currentTimeMillis()),
                        stats.databaseCount, stats.preferenceCount, stats.imageCount,
                        manifest.optInt("entryCount", 0));
                saveStatus(app, "백업 완료 · DB " + result.databaseCount
                        + "개 · 설정 " + result.preferenceCount
                        + "개 · 이미지 " + result.imageCount + "개");
                return result;
            } catch (Exception error) {
                saveStatus(app, "백업 실패 · " + safeError(error));
                throw error;
            } finally {
                deleteRecursively(stage);
                if (monitorEnabled) startMonitor(app);
            }
        }
    }

    public static RestoreResult restoreBackup(Context context, Uri source, char[] password)
            throws Exception {
        requirePassword(password);
        if (context == null || source == null) {
            throw new IllegalArgumentException("복원할 백업 파일을 선택해주세요.");
        }
        synchronized (LOCK) {
            Context app = context.getApplicationContext();
            File extracted = new File(app.getCacheDir(), "calltag-restore-stage-" + UUID.randomUUID());
            File rollback = new File(app.getNoBackupFilesDir(), "calltag-restore-rollback-" + UUID.randomUUID());
            boolean monitorEnabledBefore = SettingsStore.isMonitorEnabled(app);
            try {
                JSONObject manifest = decryptAndExtract(app, source, password, extracted);
                validateManifest(app, extracted, manifest);
                ensureNoSending(app, "발송 중인 문자가 있어 복원할 수 없습니다. 발송 결과를 확인한 뒤 다시 시도해주세요.");

                app.stopService(new Intent(app, CallMonitorService.class));
                snapshotCurrentData(app, rollback);
                cancelAllKnownMessageAlarms(app);

                try {
                    replaceFromSnapshot(app, extracted);
                    runDatabaseMigrations(app);
                    quickCheckAllDatabases(app);
                    MessageAutomationStore.ensureDefaults(app);
                    MessageTemplateStore.ensureDefaults(app);
                    int missingImages = countMissingTemplateImages(app);
                    DataIntegrityManager.Result integrity = DataIntegrityManager.recoverNow(
                            app, DataIntegrityManager.TRIGGER_MANUAL);
                    MessageRecoveryManager.Result recovery = MessageRecoveryManager.recoverNow(
                            app, MessageRecoveryManager.TRIGGER_MANUAL);
                    RestoreResult result = new RestoreResult(
                            manifest.optLong("createdAt", 0L),
                            manifest.optString("appVersion", ""),
                            countFiles(new File(extracted, "databases")),
                            countFiles(new File(extracted, "preferences")),
                            countFiles(new File(extracted, "files/message_images")),
                            missingImages,
                            integrity == null ? "" : integrity.compactSummary(),
                            recovery == null ? "" : recovery.compactSummary());
                    deleteRecursively(rollback);
                    saveStatus(app, "복원 완료 · DB " + result.databaseCount
                            + "개 · 설정 " + result.preferenceCount
                            + "개 · 이미지 " + result.imageCount
                            + "개 · 이미지 누락 " + result.missingImageCount + "개");
                    if (SettingsStore.isMonitorEnabled(app)) startMonitor(app);
                    return result;
                } catch (Exception restoreError) {
                    Exception rollbackError = null;
                    try {
                        cancelAllKnownMessageAlarms(app);
                        replaceFromSnapshot(app, rollback);
                        runDatabaseMigrations(app);
                        quickCheckAllDatabases(app);
                        MessageAutomationStore.ensureDefaults(app);
                        MessageTemplateStore.ensureDefaults(app);
                        DataIntegrityManager.recoverNow(app, DataIntegrityManager.TRIGGER_MANUAL);
                        MessageRecoveryManager.recoverNow(app, MessageRecoveryManager.TRIGGER_MANUAL);
                    } catch (Exception error) {
                        rollbackError = error;
                    }
                    if (monitorEnabledBefore) startMonitor(app);
                    if (rollbackError != null) {
                        throw new IOException("복원과 자동 롤백에 모두 실패했습니다. 앱 상태 진단을 확인해주세요. 복원 오류: "
                                + safeError(restoreError) + " · 롤백 오류: " + safeError(rollbackError), rollbackError);
                    }
                    throw new IOException("백업 복원에 실패해 기존 데이터로 자동 복구했습니다. "
                            + safeError(restoreError), restoreError);
                }
            } catch (Exception error) {
                saveStatus(app, "복원 실패 · " + safeError(error));
                throw friendlyCryptoError(error);
            } finally {
                deleteRecursively(extracted);
                deleteRecursively(rollback);
            }
        }
    }

    public static String lastSummary(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE);
        String summary = prefs.getString(STATUS_SUMMARY, "");
        long time = prefs.getLong(STATUS_TIME, 0L);
        if (summary == null || summary.trim().isEmpty()) {
            return "아직 백업·복원 기록이 없습니다.";
        }
        String label = time <= 0L ? "시각 없음"
                : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
                .format(new Date(time));
        return label + "\n" + summary;
    }

    private static SnapshotStats snapshotCurrentData(Context context, File stage) throws Exception {
        deleteRecursively(stage);
        ensureDirectory(stage);
        File databaseDir = new File(stage, "databases");
        File preferenceDir = new File(stage, "preferences");
        ensureDirectory(databaseDir);
        ensureDirectory(preferenceDir);

        SnapshotStats stats = new SnapshotStats();
        List<String> databases = new ArrayList<>();
        for (String name : context.databaseList()) {
            if (isCallTagDatabaseName(name)) databases.add(name);
        }
        Collections.sort(databases);
        for (String name : databases) {
            File source = context.getDatabasePath(name);
            if (!source.exists()) continue;
            checkpointDatabase(source);
            copyFile(source, new File(databaseDir, name));
            stats.databaseCount++;
        }

        for (String preferenceName : BACKUP_PREFS) {
            JSONObject object = serializePreferences(
                    context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE));
            writeUtf8(new File(preferenceDir, preferenceName + ".json"), object.toString());
            stats.preferenceCount++;
        }

        File imageSource = new File(context.getFilesDir(), "message_images");
        File imageTarget = new File(stage, "files/message_images");
        if (imageSource.exists()) copyDirectory(imageSource, imageTarget);
        stats.imageCount = countFiles(imageTarget);
        return stats;
    }

    private static JSONObject buildManifest(Context context, File stage, SnapshotStats stats)
            throws Exception {
        JSONArray entries = new JSONArray();
        List<File> files = new ArrayList<>();
        collectFiles(stage, files);
        Collections.sort(files, (left, right) -> relativePath(stage, left)
                .compareTo(relativePath(stage, right)));
        for (File file : files) {
            String path = relativePath(stage, file);
            if ("manifest.json".equals(path)) continue;
            JSONObject entry = new JSONObject();
            entry.put("path", path);
            entry.put("size", file.length());
            entry.put("sha256", sha256(file));
            entries.put(entry);
        }

        JSONObject manifest = new JSONObject();
        manifest.put("formatVersion", FORMAT_VERSION);
        manifest.put("packageName", context.getPackageName());
        manifest.put("createdAt", System.currentTimeMillis());
        manifest.put("appVersion", appVersionName(context));
        manifest.put("appVersionCode", appVersionCode(context));
        manifest.put("androidApi", Build.VERSION.SDK_INT);
        manifest.put("databaseCount", stats.databaseCount);
        manifest.put("preferenceCount", stats.preferenceCount);
        manifest.put("imageCount", stats.imageCount);
        manifest.put("entryCount", entries.length());
        manifest.put("entries", entries);
        return manifest;
    }

    private static void encryptDirectory(Context context, File stage, Uri target, char[] password)
            throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTES];
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(salt);
        random.nextBytes(iv);
        SecretKey key = deriveKey(password, salt, PBKDF2_ITERATIONS);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

        OutputStream opened = context.getContentResolver().openOutputStream(target, "w");
        if (opened == null) throw new IOException("백업 파일을 열지 못했습니다.");
        try (DataOutputStream header = new DataOutputStream(new BufferedOutputStream(opened))) {
            header.write(MAGIC);
            header.writeInt(FORMAT_VERSION);
            header.writeInt(PBKDF2_ITERATIONS);
            header.writeInt(salt.length);
            header.writeInt(iv.length);
            header.write(salt);
            header.write(iv);
            header.flush();
            try (CipherOutputStream encrypted = new CipherOutputStream(header, cipher);
                 ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(encrypted))) {
                zipDirectory(stage, stage, zip);
            }
        }
    }

    private static JSONObject decryptAndExtract(Context context, Uri source, char[] password,
                                                File outputDirectory) throws Exception {
        deleteRecursively(outputDirectory);
        ensureDirectory(outputDirectory);
        InputStream opened = context.getContentResolver().openInputStream(source);
        if (opened == null) throw new IOException("백업 파일을 열지 못했습니다.");

        try (DataInputStream header = new DataInputStream(new BufferedInputStream(opened))) {
            byte[] magic = new byte[MAGIC.length];
            header.readFully(magic);
            if (!Arrays.equals(MAGIC, magic)) {
                throw new IOException("콜태그 백업 파일 형식이 아닙니다.");
            }
            int format = header.readInt();
            int iterations = header.readInt();
            int saltLength = header.readInt();
            int ivLength = header.readInt();
            if (format < 1 || format > FORMAT_VERSION
                    || iterations < 100_000 || iterations > 1_000_000
                    || saltLength < 12 || saltLength > 64
                    || ivLength < 12 || ivLength > 32) {
                throw new IOException("지원하지 않는 백업 파일 형식입니다.");
            }
            byte[] salt = new byte[saltLength];
            byte[] iv = new byte[ivLength];
            header.readFully(salt);
            header.readFully(iv);

            SecretKey key = deriveKey(password, salt, iterations);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            long expanded = 0L;
            Set<String> seen = new HashSet<>();
            try (CipherInputStream decrypted = new CipherInputStream(header, cipher);
                 ZipInputStream zip = new ZipInputStream(new BufferedInputStream(decrypted))) {
                ZipEntry entry;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((entry = zip.getNextEntry()) != null) {
                    String path = normalizeEntryPath(entry.getName());
                    if (path.isEmpty() || !seen.add(path) || !isAllowedBackupPath(path)) {
                        throw new IOException("허용되지 않은 백업 내부 경로입니다.");
                    }
                    File target = safeChild(outputDirectory, path);
                    if (entry.isDirectory()) {
                        ensureDirectory(target);
                        zip.closeEntry();
                        continue;
                    }
                    ensureDirectory(target.getParentFile());
                    try (OutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
                        int count;
                        while ((count = zip.read(buffer)) >= 0) {
                            expanded += count;
                            if (expanded > MAX_EXPANDED_BYTES) {
                                throw new IOException("백업 파일의 압축 해제 크기가 너무 큽니다.");
                            }
                            output.write(buffer, 0, count);
                        }
                    }
                    zip.closeEntry();
                }
            }
        }

        File manifestFile = new File(outputDirectory, "manifest.json");
        if (!manifestFile.exists()) throw new IOException("백업 manifest가 없습니다.");
        return new JSONObject(readUtf8(manifestFile));
    }

    private static void validateManifest(Context context, File root, JSONObject manifest)
            throws Exception {
        if (manifest.optInt("formatVersion", 0) != FORMAT_VERSION) {
            throw new IOException("지원하지 않는 백업 형식 버전입니다.");
        }
        if (!context.getPackageName().equals(manifest.optString("packageName", ""))) {
            throw new IOException("다른 앱에서 만든 백업 파일입니다.");
        }
        long backupCode = manifest.optLong("appVersionCode", 0L);
        if (backupCode > appVersionCode(context)) {
            throw new IOException("현재 앱보다 새 버전에서 만든 백업입니다. 콜태그를 업데이트한 뒤 복원해주세요.");
        }
        JSONArray entries = manifest.optJSONArray("entries");
        if (entries == null || entries.length() == 0) {
            throw new IOException("백업 데이터 목록이 비어 있습니다.");
        }

        Map<String, JSONObject> expected = new HashMap<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry == null) throw new IOException("백업 데이터 목록이 손상되었습니다.");
            String path = normalizeEntryPath(entry.optString("path", ""));
            if (path.isEmpty() || !isAllowedDataPath(path) || expected.put(path, entry) != null) {
                throw new IOException("백업 데이터 경로가 올바르지 않습니다.");
            }
        }
        if (!expected.containsKey("databases/calltag.db")) {
            throw new IOException("고객 데이터베이스가 없는 백업 파일입니다.");
        }

        List<File> actualFiles = new ArrayList<>();
        collectFiles(root, actualFiles);
        int validated = 0;
        for (File file : actualFiles) {
            String path = relativePath(root, file);
            if ("manifest.json".equals(path)) continue;
            JSONObject entry = expected.get(path);
            if (entry == null) throw new IOException("manifest에 없는 파일이 포함돼 있습니다.");
            if (file.length() != entry.optLong("size", -1L)) {
                throw new IOException("백업 파일 크기 검증에 실패했습니다: " + path);
            }
            if (!sha256(file).equalsIgnoreCase(entry.optString("sha256", ""))) {
                throw new IOException("백업 파일 무결성 검증에 실패했습니다: " + path);
            }
            validated++;
        }
        if (validated != expected.size()) {
            throw new IOException("백업 파일 일부가 누락되었습니다.");
        }
    }

    private static void replaceFromSnapshot(Context context, File snapshot) throws Exception {
        File databaseSource = new File(snapshot, "databases");
        Set<String> names = new HashSet<>();
        for (String name : context.databaseList()) {
            if (isCallTagDatabaseName(name)) names.add(name);
        }
        File[] backupDatabases = databaseSource.listFiles();
        if (backupDatabases != null) {
            for (File file : backupDatabases) {
                if (file.isFile() && isCallTagDatabaseName(file.getName())) names.add(file.getName());
            }
        }
        for (String name : names) {
            context.deleteDatabase(name);
            deleteIfExists(new File(context.getDatabasePath(name).getPath() + "-wal"));
            deleteIfExists(new File(context.getDatabasePath(name).getPath() + "-shm"));
            deleteIfExists(new File(context.getDatabasePath(name).getPath() + "-journal"));
        }
        if (backupDatabases != null) {
            for (File source : backupDatabases) {
                if (!source.isFile() || !isCallTagDatabaseName(source.getName())) continue;
                File target = context.getDatabasePath(source.getName());
                ensureDirectory(target.getParentFile());
                copyFile(source, target);
            }
        }

        File preferenceSource = new File(snapshot, "preferences");
        for (String preferenceName : BACKUP_PREFS) {
            SharedPreferences preferences = context.getSharedPreferences(
                    preferenceName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit().clear();
            File source = new File(preferenceSource, preferenceName + ".json");
            if (source.exists()) restorePreferences(editor, new JSONObject(readUtf8(source)));
            if (!editor.commit()) {
                throw new IOException("설정 복원에 실패했습니다: " + preferenceName);
            }
        }

        File currentImages = new File(context.getFilesDir(), "message_images");
        deleteRecursively(currentImages);
        File backupImages = new File(snapshot, "files/message_images");
        if (backupImages.exists()) copyDirectory(backupImages, currentImages);
    }

    private static void runDatabaseMigrations(Context context) {
        CallTagDbHelper crm = new CallTagDbHelper(context);
        MessageLogStore messages = new MessageLogStore(context);
        MessageGroupStore groups = new MessageGroupStore(context);
        CampaignStore campaigns = new CampaignStore(context);
        TaskTypeStore taskTypes = new TaskTypeStore(context);
        PendingCallStore pendingCalls = new PendingCallStore(context);
        try {
            crm.getWritableDatabase();
            messages.getWritableDatabase();
            groups.getWritableDatabase();
            campaigns.getWritableDatabase();
            taskTypes.getWritableDatabase();
            pendingCalls.getWritableDatabase();
        } finally {
            pendingCalls.close();
            taskTypes.close();
            campaigns.close();
            groups.close();
            messages.close();
            crm.close();
        }
    }

    private static void quickCheckAllDatabases(Context context) throws Exception {
        List<String> names = new ArrayList<>();
        for (String name : context.databaseList()) {
            if (isCallTagDatabaseName(name)) names.add(name);
        }
        Collections.sort(names);
        for (String name : names) {
            File file = context.getDatabasePath(name);
            if (!file.exists()) continue;
            SQLiteDatabase db = null;
            try {
                db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                        SQLiteDatabase.OPEN_READONLY);
                try (Cursor cursor = db.rawQuery("PRAGMA quick_check", null)) {
                    if (!cursor.moveToFirst() || !"ok".equalsIgnoreCase(cursor.getString(0))) {
                        throw new IOException("데이터베이스 무결성 검사에 실패했습니다: " + name);
                    }
                }
            } finally {
                if (db != null) db.close();
            }
        }
    }

    private static int countMissingTemplateImages(Context context) {
        int count = 0;
        for (MessageTemplateStore.Template template : MessageTemplateStore.list(context, "", "")) {
            String ref = template.imageRef == null ? "" : template.imageRef.trim();
            if (!ref.isEmpty() && !MessageAttachmentStore.exists(context, ref)) count++;
        }
        return count;
    }

    private static void cancelAllKnownMessageAlarms(Context context) {
        File database = context.getDatabasePath("calltag_messages.db");
        if (!database.exists()) return;
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(database.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READONLY);
            try (Cursor cursor = db.rawQuery("SELECT id FROM message_jobs", null)) {
                while (cursor.moveToNext()) MessageScheduler.cancel(context, cursor.getLong(0));
            }
        } catch (RuntimeException ignored) {
        } finally {
            if (db != null) db.close();
        }
    }

    private static void ensureNoSending(Context context, String message) {
        MessageLogStore store = new MessageLogStore(context);
        try {
            if (store.countByStatus(MessageLogStore.STATUS_SENDING) > 0) {
                throw new IllegalStateException(message);
            }
        } finally {
            store.close();
        }
    }

    private static void checkpointDatabase(File file) throws Exception {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READWRITE);
            try (Cursor cursor = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)) {
                if (cursor.moveToFirst() && cursor.getInt(0) != 0) {
                    throw new IOException("데이터베이스가 사용 중이라 안전한 백업을 만들 수 없습니다: "
                            + file.getName());
                }
            }
        } finally {
            if (db != null) db.close();
        }
    }

    private static JSONObject serializePreferences(SharedPreferences preferences)
            throws JSONException {
        JSONObject result = new JSONObject();
        List<String> keys = new ArrayList<>(preferences.getAll().keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Object value = preferences.getAll().get(key);
            if (value == null) continue;
            JSONObject item = new JSONObject();
            if (value instanceof String) {
                item.put("type", "string");
                item.put("value", value);
            } else if (value instanceof Integer) {
                item.put("type", "int");
                item.put("value", value);
            } else if (value instanceof Long) {
                item.put("type", "long");
                item.put("value", value);
            } else if (value instanceof Float) {
                item.put("type", "float");
                item.put("value", ((Float) value).doubleValue());
            } else if (value instanceof Boolean) {
                item.put("type", "boolean");
                item.put("value", value);
            } else if (value instanceof Set) {
                item.put("type", "string_set");
                JSONArray array = new JSONArray();
                List<String> values = new ArrayList<>();
                for (Object member : (Set<?>) value) values.add(String.valueOf(member));
                Collections.sort(values);
                for (String member : values) array.put(member);
                item.put("value", array);
            } else {
                continue;
            }
            result.put(key, item);
        }
        return result;
    }

    private static void restorePreferences(SharedPreferences.Editor editor, JSONObject object)
            throws JSONException {
        JSONArray names = object.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            String key = names.getString(i);
            JSONObject item = object.optJSONObject(key);
            if (item == null) continue;
            String type = item.optString("type", "");
            if ("string".equals(type)) editor.putString(key, item.optString("value", ""));
            else if ("int".equals(type)) editor.putInt(key, item.optInt("value", 0));
            else if ("long".equals(type)) editor.putLong(key, item.optLong("value", 0L));
            else if ("float".equals(type)) editor.putFloat(key, (float) item.optDouble("value", 0d));
            else if ("boolean".equals(type)) editor.putBoolean(key, item.optBoolean("value", false));
            else if ("string_set".equals(type)) {
                Set<String> values = new HashSet<>();
                JSONArray array = item.optJSONArray("value");
                if (array != null) {
                    for (int j = 0; j < array.length(); j++) values.add(array.optString(j, ""));
                }
                editor.putStringSet(key, values);
            }
        }
    }

    private static SecretKey deriveKey(char[] password, byte[] salt, int iterations)
            throws Exception {
        KeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
        byte[] encoded;
        try {
            encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } finally {
            if (spec instanceof PBEKeySpec) ((PBEKeySpec) spec).clearPassword();
        }
        return new SecretKeySpec(encoded, "AES");
    }

    private static void zipDirectory(File root, File current, ZipOutputStream zip)
            throws IOException {
        File[] children = current.listFiles();
        if (children == null) return;
        Arrays.sort(children, (left, right) -> left.getName().compareTo(right.getName()));
        byte[] buffer = new byte[BUFFER_SIZE];
        for (File child : children) {
            String path = relativePath(root, child);
            if (child.isDirectory()) {
                zip.putNextEntry(new ZipEntry(path + "/"));
                zip.closeEntry();
                zipDirectory(root, child, zip);
            } else {
                zip.putNextEntry(new ZipEntry(path));
                try (InputStream input = new BufferedInputStream(new FileInputStream(child))) {
                    int count;
                    while ((count = input.read(buffer)) >= 0) zip.write(buffer, 0, count);
                }
                zip.closeEntry();
            }
        }
    }

    private static boolean isAllowedBackupPath(String path) {
        return "manifest.json".equals(path) || isAllowedDataPath(path)
                || path.equals("databases") || path.equals("preferences")
                || path.equals("files") || path.equals("files/message_images");
    }

    private static boolean isAllowedDataPath(String path) {
        if (path.startsWith("databases/")) {
            String name = path.substring("databases/".length());
            return isCallTagDatabaseName(name) && !name.contains("/");
        }
        if (path.startsWith("preferences/")) {
            String name = path.substring("preferences/".length());
            if (!name.endsWith(".json") || name.contains("/")) return false;
            String preferenceName = name.substring(0, name.length() - 5);
            return Arrays.asList(BACKUP_PREFS).contains(preferenceName);
        }
        return path.startsWith("files/message_images/")
                && !path.substring("files/message_images/".length()).contains("/");
    }

    private static boolean isCallTagDatabaseName(String name) {
        return name != null && name.startsWith("calltag") && name.endsWith(".db")
                && !name.contains("/") && !name.contains("\\");
    }

    private static String normalizeEntryPath(String value) {
        String path = value == null ? "" : value.replace('\\', '/').trim();
        while (path.startsWith("/")) path = path.substring(1);
        while (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        if (path.equals(".") || path.contains("../") || path.contains("/..")
                || path.contains(":")) return "";
        return path;
    }

    private static File safeChild(File root, String path) throws IOException {
        File target = new File(root, path);
        String rootPath = root.getCanonicalPath() + File.separator;
        if (!target.getCanonicalPath().startsWith(rootPath)) {
            throw new IOException("백업 내부 경로가 안전하지 않습니다.");
        }
        return target;
    }

    private static String relativePath(File root, File file) {
        String base = root.getAbsolutePath();
        String path = file.getAbsolutePath();
        if (path.startsWith(base)) path = path.substring(base.length());
        while (path.startsWith(File.separator)) path = path.substring(1);
        return path.replace(File.separatorChar, '/');
    }

    private static void collectFiles(File directory, List<File> output) {
        File[] children = directory.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) collectFiles(child, output);
            else output.add(child);
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            int count;
            while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
        }
        StringBuilder value = new StringBuilder();
        for (byte current : digest.digest()) value.append(String.format(Locale.US, "%02x", current));
        return value.toString();
    }

    private static void copyDirectory(File source, File target) throws IOException {
        if (!source.exists()) return;
        ensureDirectory(target);
        File[] children = source.listFiles();
        if (children == null) return;
        for (File child : children) {
            File destination = new File(target, child.getName());
            if (child.isDirectory()) copyDirectory(child, destination);
            else copyFile(child, destination);
        }
    }

    private static void copyFile(File source, File target) throws IOException {
        ensureDirectory(target.getParentFile());
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = new BufferedInputStream(new FileInputStream(source));
             OutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        }
    }

    private static int countFiles(File directory) {
        if (directory == null || !directory.exists()) return 0;
        if (directory.isFile()) return 1;
        int count = 0;
        File[] children = directory.listFiles();
        if (children != null) {
            for (File child : children) count += countFiles(child);
        }
        return count;
    }

    private static void ensureDirectory(File directory) throws IOException {
        if (directory == null) return;
        if (directory.exists()) {
            if (!directory.isDirectory()) throw new IOException("디렉터리 경로가 올바르지 않습니다.");
            return;
        }
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("임시 저장 공간을 만들지 못했습니다.");
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static void deleteIfExists(File file) {
        if (file != null && file.exists()) file.delete();
    }

    private static void writeUtf8(File file, String value) throws IOException {
        ensureDirectory(file.getParentFile());
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
            output.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String readUtf8(File file) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (output.size() + count > 16 * 1024 * 1024) {
                    throw new IOException("설정 파일 크기가 너무 큽니다.");
                }
                output.write(buffer, 0, count);
            }
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static String appVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName == null ? "" : info.versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    private static long appVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? info.getLongVersionCode() : info.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            return 0L;
        }
    }

    private static void requirePassword(char[] password) {
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("백업 암호는 8자 이상 입력해주세요.");
        }
    }

    private static void startMonitor(Context context) {
        if (!SettingsStore.isMonitorEnabled(context)) return;
        Intent service = new Intent(context, CallMonitorService.class)
                .setAction(CallMonitorService.ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
            else context.startService(service);
        } catch (RuntimeException ignored) {
        }
    }

    private static Exception friendlyCryptoError(Exception error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof AEADBadTagException) {
                return new IllegalArgumentException("암호가 틀리거나 백업 파일이 손상되었습니다.", error);
            }
            current = current.getCause();
        }
        String message = safeError(error);
        if (message.toLowerCase(Locale.US).contains("tag mismatch")
                || message.toLowerCase(Locale.US).contains("mac check")) {
            return new IllegalArgumentException("암호가 틀리거나 백업 파일이 손상되었습니다.", error);
        }
        return error;
    }

    private static void saveStatus(Context context, String summary) {
        context.getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE).edit()
                .putString(STATUS_SUMMARY, summary == null ? "" : summary)
                .putLong(STATUS_TIME, System.currentTimeMillis())
                .apply();
    }

    private static String safeError(Throwable error) {
        String value = error == null ? "" : error.getMessage();
        if (value == null || value.trim().isEmpty()) value = "알 수 없는 오류";
        value = value.replace('\n', ' ').replace('\r', ' ').trim();
        return value.length() > 180 ? value.substring(0, 180) : value;
    }

    private static final class SnapshotStats {
        int databaseCount;
        int preferenceCount;
        int imageCount;
    }

    public static final class BackupResult {
        public final long createdAt;
        public final int databaseCount;
        public final int preferenceCount;
        public final int imageCount;
        public final int entryCount;

        BackupResult(long createdAt, int databaseCount, int preferenceCount,
                     int imageCount, int entryCount) {
            this.createdAt = createdAt;
            this.databaseCount = databaseCount;
            this.preferenceCount = preferenceCount;
            this.imageCount = imageCount;
            this.entryCount = entryCount;
        }

        public String summary() {
            return "DB " + databaseCount + "개 · 설정 " + preferenceCount
                    + "개 · 이미지 " + imageCount + "개";
        }
    }

    public static final class RestoreResult {
        public final long backupCreatedAt;
        public final String backupAppVersion;
        public final int databaseCount;
        public final int preferenceCount;
        public final int imageCount;
        public final int missingImageCount;
        public final String integritySummary;
        public final String recoverySummary;

        RestoreResult(long backupCreatedAt, String backupAppVersion,
                      int databaseCount, int preferenceCount, int imageCount,
                      int missingImageCount, String integritySummary,
                      String recoverySummary) {
            this.backupCreatedAt = backupCreatedAt;
            this.backupAppVersion = backupAppVersion == null ? "" : backupAppVersion;
            this.databaseCount = databaseCount;
            this.preferenceCount = preferenceCount;
            this.imageCount = imageCount;
            this.missingImageCount = missingImageCount;
            this.integritySummary = integritySummary == null ? "" : integritySummary;
            this.recoverySummary = recoverySummary == null ? "" : recoverySummary;
        }

        public String summary() {
            String value = "DB " + databaseCount + "개 · 설정 " + preferenceCount
                    + "개 · 이미지 " + imageCount + "개";
            if (missingImageCount > 0) value += " · 이미지 누락 " + missingImageCount + "개";
            return value;
        }
    }
}
