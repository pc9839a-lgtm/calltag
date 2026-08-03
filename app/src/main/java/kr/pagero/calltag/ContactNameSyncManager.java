package kr.pagero.calltag;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 전화 앱의 기본 연락처 이름에 "고객명 · 최근 메모"가 보이도록 콜태그 전용
 * 연락처 계정에 RawContact를 만든다. Google·삼성 등 원본 연락처는 수정하지 않는다.
 * 기능 해제 시 콜태그 계정과 콜태그가 만든 RawContact만 제거한다.
 */
public final class ContactNameSyncManager {
    private static final String PREFS = "calltag_contact_name_sync";
    private static final String SOURCE_PREFIX = "calltag:";
    private static final String KEY_LAST_ALIAS_PREFIX = "last_alias_";
    private static final int MAX_BASE_LENGTH = 14;
    private static final int MAX_MEMO_LENGTH = 16;
    private static final int MAX_ALIAS_LENGTH = 32;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean SYNCING = new AtomicBoolean(false);
    private static final AtomicBoolean RESYNC_REQUESTED = new AtomicBoolean(false);

    private ContactNameSyncManager() {}

    public static boolean hasPermissions(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void enable(Context context) {
        SettingsStore.setContactNameSyncEnabled(context, true);
        requestSyncAll(context);
    }

    public static void disableAndRestore(Context context) {
        Context app = context.getApplicationContext();
        SettingsStore.setContactNameSyncEnabled(app, false);
        EXECUTOR.execute(() -> restoreAllNow(app));
    }

    public static void requestSyncAll(Context context) {
        Context app = context.getApplicationContext();
        if (!SettingsStore.isContactNameSyncEnabled(app) || !hasPermissions(app)) return;
        if (!SYNCING.compareAndSet(false, true)) {
            RESYNC_REQUESTED.set(true);
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                do {
                    RESYNC_REQUESTED.set(false);
                    syncAllNow(app);
                } while (RESYNC_REQUESTED.get());
            } finally {
                SYNCING.set(false);
            }
        });
    }

    private static void syncAllNow(Context context) {
        if (!CallTagContactsAccount.ensure(context)) {
            SettingsStore.setContactNameSyncStatus(context,
                    "콜태그 연락처 계정을 만들지 못했습니다.");
            return;
        }
        cleanupLegacyLocalRows(context.getContentResolver());

        CallTagDbHelper db = new CallTagDbHelper(context);
        int synced = 0;
        try {
            for (Customer customer : db.listCustomers(null)) {
                if (syncCustomerNow(context, db, customer)) synced++;
            }
            SettingsStore.setContactNameSyncStatus(context,
                    synced + "명의 연락처 이름에 최근 메모를 반영했습니다.");
        } catch (RuntimeException error) {
            SettingsStore.setContactNameSyncStatus(context,
                    "연락처 이름 동기화 중 오류가 발생했습니다.");
        } finally {
            db.close();
        }
    }

    private static boolean syncCustomerNow(Context context, CallTagDbHelper db, Customer customer) {
        if (customer == null || customer.id <= 0L) return false;
        String normalized = PhoneNumberNormalizer.normalize(customer.primaryPhone);
        if (normalized.length() < 8) return false;

        String alias = buildAlias(db, customer);
        SharedPreferences prefs = prefs(context);
        String lastAlias = prefs.getString(KEY_LAST_ALIAS_PREFIX + customer.id, "");
        ContentResolver resolver = context.getContentResolver();
        long appRawId = findAppRawContact(resolver, customer.id);

        if (appRawId <= 0L) {
            appRawId = createAppRawContact(resolver, customer, alias);
            if (appRawId <= 0L) return false;
        } else if (!alias.equals(lastAlias)) {
            updateAppRawContact(resolver, appRawId, customer.primaryPhone, alias);
        }

        long originalRawId = findOriginalRawContact(resolver, customer.primaryPhone, appRawId);
        if (originalRawId > 0L) keepTogether(resolver, originalRawId, appRawId);

        prefs.edit().putString(KEY_LAST_ALIAS_PREFIX + customer.id, alias).apply();
        return true;
    }

    private static String buildAlias(CallTagDbHelper db, Customer customer) {
        String base = clean(customer.displayName);
        if (base.isEmpty() || "이름없는고객".equals(base) || "이름 없음".equals(base)) {
            String digits = PhoneNumberNormalizer.normalize(customer.primaryPhone);
            String suffix = digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits;
            base = suffix.isEmpty() ? "고객" : "고객 " + suffix;
        }
        base = shorten(base, MAX_BASE_LENGTH);

        String memo = clean(CustomerInsightResolver.latestMemo(db, customer));
        if (memo.isEmpty()) return base;
        memo = shorten(memo, MAX_MEMO_LENGTH);
        return shorten(base + " · " + memo, MAX_ALIAS_LENGTH);
    }

    private static long createAppRawContact(ContentResolver resolver, Customer customer,
                                            String alias) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        operations.add(ContentProviderOperation.newInsert(syncUri(ContactsContract.RawContacts.CONTENT_URI))
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, CallTagContactsAccount.TYPE)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, CallTagContactsAccount.NAME)
                .withValue(ContactsContract.RawContacts.SOURCE_ID, SOURCE_PREFIX + customer.id)
                .withValue(ContactsContract.RawContacts.AGGREGATION_MODE,
                        ContactsContract.RawContacts.AGGREGATION_MODE_DEFAULT)
                .build());
        operations.add(ContentProviderOperation.newInsert(syncUri(ContactsContract.Data.CONTENT_URI))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, alias)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, alias)
                .withValue(ContactsContract.Data.IS_PRIMARY, 1)
                .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                .build());
        operations.add(ContentProviderOperation.newInsert(syncUri(ContactsContract.Data.CONTENT_URI))
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, customer.primaryPhone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .withValue(ContactsContract.Data.IS_PRIMARY, 1)
                .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                .build());
        try {
            ContentProviderResult[] results = resolver.applyBatch(
                    ContactsContract.AUTHORITY, operations);
            Uri uri = results.length > 0 ? results[0].uri : null;
            return uri == null ? -1L : Long.parseLong(uri.getLastPathSegment());
        } catch (RemoteException | OperationApplicationException | RuntimeException error) {
            return -1L;
        }
    }

    private static void updateAppRawContact(ContentResolver resolver, long rawId,
                                            String phone, String alias) {
        ContentValues name = new ContentValues();
        name.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, alias);
        name.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, alias);
        name.put(ContactsContract.Data.IS_PRIMARY, 1);
        name.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
        int updated = resolver.update(syncUri(ContactsContract.Data.CONTENT_URI), name,
                ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
                        + ContactsContract.Data.MIMETYPE + "=?",
                new String[]{String.valueOf(rawId),
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE});
        if (updated == 0) {
            name.put(ContactsContract.Data.RAW_CONTACT_ID, rawId);
            name.put(ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
            resolver.insert(syncUri(ContactsContract.Data.CONTENT_URI), name);
        }

        ContentValues phoneValues = new ContentValues();
        phoneValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone);
        phoneValues.put(ContactsContract.Data.IS_PRIMARY, 1);
        phoneValues.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
        resolver.update(syncUri(ContactsContract.Data.CONTENT_URI), phoneValues,
                ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
                        + ContactsContract.Data.MIMETYPE + "=?",
                new String[]{String.valueOf(rawId),
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE});
    }

    private static long findAppRawContact(ContentResolver resolver, long customerId) {
        try (Cursor cursor = resolver.query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND "
                        + ContactsContract.RawContacts.ACCOUNT_NAME + "=? AND "
                        + ContactsContract.RawContacts.SOURCE_ID + "=? AND "
                        + ContactsContract.RawContacts.DELETED + "=0",
                new String[]{CallTagContactsAccount.TYPE, CallTagContactsAccount.NAME,
                        SOURCE_PREFIX + customerId}, null)) {
            return cursor != null && cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        } catch (RuntimeException error) {
            return -1L;
        }
    }

    private static long findOriginalRawContact(ContentResolver resolver, String phone,
                                               long appRawId) {
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone));
        long contactId = -1L;
        try (Cursor cursor = resolver.query(lookupUri,
                new String[]{ContactsContract.PhoneLookup.CONTACT_ID}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) contactId = cursor.getLong(0);
        } catch (RuntimeException error) {
            return -1L;
        }
        if (contactId <= 0L) return -1L;

        try (Cursor cursor = resolver.query(ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.CONTACT_ID + "=? AND "
                        + ContactsContract.RawContacts._ID + "<>? AND "
                        + "(" + ContactsContract.RawContacts.ACCOUNT_TYPE + " IS NULL OR "
                        + ContactsContract.RawContacts.ACCOUNT_TYPE + "<>?) AND "
                        + ContactsContract.RawContacts.DELETED + "=0",
                new String[]{String.valueOf(contactId), String.valueOf(appRawId),
                        CallTagContactsAccount.TYPE},
                ContactsContract.RawContacts._ID + " ASC")) {
            return cursor != null && cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        } catch (RuntimeException error) {
            return -1L;
        }
    }

    private static void keepTogether(ContentResolver resolver, long firstRawId,
                                     long secondRawId) {
        if (firstRawId <= 0L || secondRawId <= 0L || firstRawId == secondRawId) return;
        ContentValues values = new ContentValues();
        values.put(ContactsContract.AggregationExceptions.TYPE,
                ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER);
        values.put(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, firstRawId);
        values.put(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, secondRawId);
        try {
            resolver.update(ContactsContract.AggregationExceptions.CONTENT_URI,
                    values, null, null);
        } catch (RuntimeException ignored) {
            // 동일 번호 자동 결합도 함께 동작한다.
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
            // 다음 동기화에서 다시 정리한다.
        }
    }

    private static void restoreAllNow(Context context) {
        if (!hasPermissions(context)) {
            CallTagContactsAccount.remove(context);
            prefs(context).edit().clear().apply();
            return;
        }
        ContentResolver resolver = context.getContentResolver();
        try {
            resolver.delete(syncUri(ContactsContract.RawContacts.CONTENT_URI),
                    ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND "
                            + ContactsContract.RawContacts.ACCOUNT_NAME + "=?",
                    new String[]{CallTagContactsAccount.TYPE, CallTagContactsAccount.NAME});
            cleanupLegacyLocalRows(resolver);
            CallTagContactsAccount.remove(context);
            prefs(context).edit().clear().apply();
            SettingsStore.setContactNameSyncStatus(context,
                    "콜태그 메모 연락처를 제거하고 원래 연락처로 복원했습니다.");
        } catch (RuntimeException error) {
            SettingsStore.setContactNameSyncStatus(context,
                    "연락처 이름 복원에 실패했습니다.");
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

    private static String clean(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .replace("·", " ")
                .trim();
    }

    private static String shorten(String value, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() <= maxLength) return clean;
        return clean.substring(0, Math.max(1, maxLength - 1)).trim() + "…";
    }
}
