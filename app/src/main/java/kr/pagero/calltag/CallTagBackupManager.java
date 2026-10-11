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
    private static final int FORMAT_VERSION = 2;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final long MAX_EXPANDED_BYTES = 1024L * 1024L * 1024L;
    private static final int BUFFER_SIZE = 32 * 1024;

    private static final String STATUS_PREFS = "calltag_backup_status";
    private static final String STATUS_SUMMARY = "last_summary";
    private static final String STATUS_TIME = "last_time";

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
            String owner = AccountDataScope.requireOwner(app);
            String session = AuthSessionStore.session(app);
            if (!CallTagSyncManager.beginMaintenance()) {
                throw new IllegalStateException("다른 데이터 작업이 진행 중입니다.");
            }
            try {
            if (PageroLeadSyncManager.isRunning() || UniversalLeadSyncManager.isRunning()) {
                throw new IllegalStateException("문의 수신 종료 후 백업을 다시 시도해주세요.");
            }
            requireSameAccount(app, owner, session);
            ensureNoSending(app, "발송 중인 문자가 있어 백업을 시작할 수 없습니다.");
            boolean monitorEnabled = SettingsStore.isMonitorEnabled(app);
            app.stopService(new Intent(app, CallMonitorService.class));
            File stage = new File(app.getCacheDir(), "calltag-backup-stage-" + UUID.randomUUID());
            try {
                runDatabaseMigrations(app);
                SnapshotStats stats = snapshotCurrentData(app, stage, owner, session);
                JSONObject manifest = buildManifest(app, stage, stats);
                writeUtf8(new File(stage, "manifest.json"), manifest.toString());
                requireSameAccount(app, owner, session);
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
                if (monitorEnabled && owner.equals(AuthSessionStore.ownerId(app))) startMonitor(app);
            }
            } finally {
                CallTagSyncManager.endMaintenance();
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
            String owner = AccountDataScope.requireOwner(app);
            String session = AuthSessionStore.session(app);
            if (!CallTagSyncManager.beginMaintenance()) {
                throw new IllegalStateException("다른 데이터 작업이 진행 중입니다.");
            }
            try {
            if (PageroLeadSyncManager.isRunning() || UniversalLeadSyncManager.isRunning()) {
                throw new IllegalStateException("문의 수신 종료 후 복원을 다시 시도해주세요.");
            }
            File extracted = new File(app.getCacheDir(), "calltag-restore-stage-" + UUID.randomUUID());
            File rollback = new File(app.getNoBackupFilesDir(), "calltag-restore-rollback-" + UUID.randomUUID());
            boolean monitorEnabledBefore = SettingsStore.isMonitorEnabled(app);
            try {
                JSONObject manifest = decryptAndExtract(app, source, password, extracted);
                requireSameAccount(app, owner, session);
                validateManifest(app, extracted, manifest);
                ensureNoSending(app, "발송 중인 문자가 있어 복원할 수 없습니다. 발송 결과를 확인한 뒤 다시 시도해주세요.");

                app.stopService(new Intent(app, CallMonitorService.class));
                snapshotCurrentData(app, rollback, owner, session);
                cancelAllKnownMessageAlarms(app);
                // Old carrier callbacks and scheduled alarms can refer to the
                // same numeric job IDs after restoring a historical snapshot.
                AccountDataScope.rotateWorkEpoch(app);

                try {
                    replaceFromSnapshot(app, extracted, owner, session);
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
                    // Rollback staging must remain available until EVERY
                    // post-restore step, including cloud-map invalidation, succeeds.
                    invalidateOwnerSyncMappings(app);
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
                        replaceFromSnapshot(app, rollback, owner, session);
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
            } finally {
                CallTagSyncManager.endMaintenance();
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

    private static SnapshotStats snapshotCurrentData(Context context, File stage,
                                                     String owner, String session) throws Exception {
        deleteRecursively(stage);
        ensureDirectory(stage);
        File databaseDir = new File(stage, "databases");
        File preferenceDir = new File(stage, "preferences");
        ensureDirectory(databaseDir);
        ensureDirectory(preferenceDir);

        SnapshotStats stats = new SnapshotStats();
        List<String> databases = AccountDataScope.currentAccountDatabases(context);
        Collections.sort(databases);
        for (String name : databases) {
            requireSameAccount(context, owner, session);
            File source = context.getDatabasePath(name);
            if (!source.exists()) continue;
            checkpointDatabase(source);
            copyFile(source, new File(databaseDir, name));
            stats.databaseCount++;
        }

        for (String preferenceName : AccountDataScope.currentAccountPreferences(context)) {
            requireSameAccount(context, owner, session);
            JSONObject object = serializePreferences(
                    context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE));
            writeUtf8(new File(preferenceDir, preferenceName + ".json"), object.toString());
            stats.preferenceCount++;
        }

        // The old global image directory is not owner tagged: never archive it.
        stats.imageCount = 0;
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
        manifest.put("ownerFingerprint", AccountDataScope.fingerprint(
                AccountDataScope.requireOwner(context)));
        manifest.put("scope", "owner-sqlite-v2");
        manifest.put("imagesIncluded", false);
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
            throw new IOException("기존 공용 백업(v1)은 계정 소유 정보가 없어 직접 복원할 수 없습니다.");
        }
        if (!"owner-sqlite-v2".equals(manifest.optString("scope", ""))
                || !AccountDataScope.fingerprint(AccountDataScope.requireOwner(context))
                    .equals(manifest.optString("ownerFingerprint", ""))) {
            throw new IOException("다른 계정에서 생성된 백업은 복원할 수 없습니다.");
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
            if (path.isEmpty() || !isAllowedOwnerPath(context, path)
                    || expected.put(path, entry) != null) {
                throw new IOException("백업 데이터 경로가 올바르지 않습니다.");
            }
        }
        // v2 backups must be complete. Do not accept a reduced archive that
        // could silently erase the active owner's missing database on restore.
        for (String name : AccountDataScope.currentAccountDatabases(context)) {
            if (!expected.containsKey("databases/" + name)) {
                throw new IOException("계정별 DB가 누락된 백업입니다: " + name);
            }
        }
        for (String preference : AccountDataScope.currentAccountPreferences(context)) {
            if (!expected.containsKey("preferences/" + preference + ".json")) {
                throw new IOException("계정별 설정 파일이 누락된 백업입니다.");
            }
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

    private static void replaceFromSnapshot(Context context, File snapshot,
                                            String owner, String session) throws Exception {
        File databaseSource = new File(snapshot, "databases");
        for (String name : AccountDataScope.currentAccountDatabases(context)) {
            requireSameAccount(context, owner, session);
            context.deleteDatabase(name);
            deleteIfExists(new File(context.getDatabasePath(name).getPath() + "-wal"));
            deleteIfExists(new File(context.getDatabasePath(name).getPath() + "-shm"));
            deleteIfExists(new File(context.getDatabasePath(name).getPath() + "-journal"));
            File source = new File(databaseSource, name);
            if (source.isFile()) {
                File target = context.getDatabasePath(name);
                ensureDirectory(target.getParentFile());
                copyFile(source, target);
            }
        }
        File preferenceSource = new File(snapshot, "preferences");
        for (String preferenceName : AccountDataScope.currentAccountPreferences(context)) {
            requireSameAccount(context, owner, session);
            SharedPreferences preferences = context.getSharedPreferences(
                    preferenceName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit().clear();
            File source = new File(preferenceSource, preferenceName + ".json");
            if (source.exists()) restorePreferences(editor, new JSONObject(readUtf8(source)));
            if (!editor.commit()) throw new IOException("설정 복원 실패: " + preferenceName);
        }
        // Global message_images remains untouched: may contain another account's files.
    }

    private static void runDatabaseMigrations(Context context) {
        CallTagDbHelper crm = new CallTagDbHelper(context);
        MessageLogStore messages = new MessageLogStore(context);
        MessageGroupStore groups = new MessageGroupStore(context);
        CampaignStore campaigns = new CampaignStore(context);
        TaskTypeStore taskTypes = new TaskTypeStore(context);
        PendingCallStore pendingCalls = new PendingCallStore(context);
        PageroLeadReceiptStore receipts = new PageroLeadReceiptStore(context);
        try {
            crm.getWritableDatabase();
            messages.getWritableDatabase();
            groups.getWritableDatabase();
            campaigns.getWritableDatabase();
            taskTypes.getWritableDatabase();
            pendingCalls.getWritableDatabase();
            receipts.getWritableDatabase();
        } finally {
            receipts.close();
            pendingCalls.close();
            taskTypes.close();
            campaigns.close();
            groups.close();
            messages.close();
            crm.close();
        }
    }

    private static void quickCheckAllDatabases(Context context) throws Exception {
        List<String> names = AccountDataScope.currentAccountDatabases(context);
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
        File database = context.getDatabasePath(
                AccountDataScope.name(context, "calltag_messages.db"));
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
                || path.equals("databases") || path.equals("preferences");
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
            return preferenceName.matches(
                    "calltag[a-zA-Z0-9_-]*-owner-[0-9a-f]{40}");
        }
        return false;
    }

    private static boolean isCallTagDatabaseName(String name) {
        return name != null && name.matches(
                "calltag[a-zA-Z0-9_-]*-owner-[0-9a-f]{40}\\.db");
    }

    private static boolean isAllowedOwnerPath(Context context, String path) {
        if (path.startsWith("databases/")) {
            return AccountDataScope.currentAccountDatabases(context).contains(
                    path.substring("databases/".length()));
        }
        if (path.startsWith("preferences/") && path.endsWith(".json")) {
            return AccountDataScope.currentAccountPreferences(context).contains(
                    path.substring("preferences/".length(), path.length() - 5));
        }
        return false;
    }

    private static void requireSameAccount(Context context, String owner, String session) {
        if (!owner.equals(AccountDataScope.requireOwner(context))
                || !session.equals(AuthSessionStore.session(context))) {
            throw new IllegalStateException("백업·복원 중 로그인 계정이 바뀌었습니다.");
        }
    }

    private static void invalidateOwnerSyncMappings(Context context) {
        CallTagSyncPreferenceStore.setEnabled(context, false);
        try (CallTagSyncLocalStore store = new CallTagSyncLocalStore(context)) {
            String key = store.accountKey();
            if (!key.isEmpty()) {
                SQLiteDatabase db = store.getWritableDatabase();
                db.beginTransaction();
                try {
                    db.delete("entity_map", "account_key=?", new String[]{key});
                    db.delete("sync_meta", "account_key=?", new String[]{key});
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        }
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
