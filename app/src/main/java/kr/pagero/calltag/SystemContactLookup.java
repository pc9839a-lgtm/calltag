package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

/** Reads an existing system contact name without creating or modifying contacts. */
public final class SystemContactLookup {
    private SystemContactLookup() {}

    public static String displayName(Context context, String phone) {
        if (context == null || phone == null || phone.trim().isEmpty()) return "";
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return "";
        }

        Uri lookup = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone));
        try (Cursor cursor = context.getContentResolver().query(
                lookup,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null,
                null,
                null)) {
            if (cursor == null || !cursor.moveToFirst()) return "";
            String name = cursor.getString(0);
            return name == null ? "" : name.trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
