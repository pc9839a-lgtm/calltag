package kr.pagero.calltag;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Legacy cleanup only.
 *
 * v0.40.3부터 콜태그는 고객을 시스템 연락처로 생성하거나 연락처 이름을 바꾸지 않는다.
 * 기존 버전이 만든 콜태그 전용 RawContact와 계정만 제거한다.
 */
public final class ContactNameSyncManager {
    private static final String PREFS = "calltag_contact_name_sync";
    private static final String SOURCE_PREFIX = "calltag:";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private ContactNameSyncManager() {}

    public static boolean hasPermissions(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Kept for old call sites. It now disables sync and restores contacts instead. */
    public static void enable(Context context) {
        disableAndRestore(context);
    }

    /** Kept for old call sites. Contact creation and alias synchronization are permanently disabled. */
    public static void requestSyncAll(Context context) {
        SettingsStore.setContactNameSyncEnabled(context, false);
        SettingsStore.setContactNameSyncStatus(context,
                "연락처 동기화는 사용하지 않습니다. 전화 화면 오버레이로 메모를 표시합니다.");
    }

    public static void disableAndRestore(Context context) {
        Context app = context.getApplicationContext();
        SettingsStore.setContactNameSyncEnabled(app, false);
        EXECUTOR.execute(() -> restoreAllNow(app));
    }

    private static void restoreAllNow(Context context) {
        try {
            if (hasPermissions(context)) {
                ContentResolver resolver = context.getContentResolver();
                resolver.delete(syncUri(ContactsContract.RawContacts.CONTENT_URI),
                        ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND "
                                + ContactsContract.RawContacts.ACCOUNT_NAME + "=?",
                        new String[]{CallTagContactsAccount.TYPE, CallTagContactsAccount.NAME});
                cleanupLegacyLocalRows(resolver);
            }
            CallTagContactsAccount.remove(context);
            prefs(context).edit().clear().apply();
            SettingsStore.setContactNameSyncStatus(context,
                    "연락처 생성·이름 변경을 중지하고 기존 콜태그 연락처를 정리했습니다.");
        } catch (RuntimeException error) {
            SettingsStore.setContactNameSyncStatus(context,
                    "기존 콜태그 연락처 정리에 실패했습니다. 연락처 수정 권한이 이미 허용된 기기에서 다시 실행해주세요.");
        }
    }

    private static void cleanupLegacyLocalRows(ContentResolver resolver) {
        try {
            resolver.delete(syncUri(ContactsContract.RawContacts.CONTENT_URI),
                    ContactsContract.RawContacts.SOURCE_ID + " LIKE ? AND ("
                            + ContactsContract.RawContacts.ACCOUNT_TYPE + " IS NULL OR "
                            + ContactsContract.RawContacts.ACCOUNT_TYPE + "<>?)",
                    new String[]{SOURCE_PREFIX + "%", CallTagContactsAccount.TYPE});
        } catch (RuntimeException ignored) {
            // Existing rows can be retried on the next app launch.
        }
    }

    private static Uri syncUri(Uri base) {
        return base.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME,
                        CallTagContactsAccount.NAME)
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE,
                        CallTagContactsAccount.TYPE)
                .build();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
