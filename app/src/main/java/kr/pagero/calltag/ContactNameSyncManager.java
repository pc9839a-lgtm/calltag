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
 * CallTag no longer creates, merges, or renames contacts. This class remains only to remove
 * RawContacts created by older builds. New memo display is handled by CallLogMemoSyncManager.
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

    /** Kept for binary/source compatibility. It now only disables legacy contact sync. */
    public static void enable(Context context) {
        disableAndRestore(context);
    }

    /** Kept for old call sites. Contact creation and alias synchronization are disabled. */
    public static void requestSyncAll(Context context) {
        SettingsStore.setContactNameSyncEnabled(context, false);
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
                    "기존 콜태그 연락처를 정리했습니다. 연락처 이름은 더 이상 변경하지 않습니다.");
        } catch (RuntimeException error) {
            SettingsStore.setContactNameSyncStatus(context,
                    "기존 콜태그 연락처 정리를 다음 실행에서 다시 시도합니다.");
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
