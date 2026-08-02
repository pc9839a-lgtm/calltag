package kr.pagero.calltag;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.ContactsContract;

/** App-owned contact account. Removing the account removes only CallTag raw contacts. */
public final class CallTagContactsAccount {
    public static final String TYPE = "kr.pagero.calltag.contacts";
    public static final String NAME = "콜태그 메모";

    private CallTagContactsAccount() {}

    public static Account value() {
        return new Account(NAME, TYPE);
    }

    public static boolean ensure(Context context) {
        AccountManager manager = AccountManager.get(context.getApplicationContext());
        for (Account account : manager.getAccountsByType(TYPE)) {
            if (NAME.equals(account.name)) {
                configure(account);
                return true;
            }
        }
        Account account = value();
        boolean added;
        try {
            added = manager.addAccountExplicitly(account, null, null);
        } catch (RuntimeException error) {
            return false;
        }
        if (!added) {
            for (Account existing : manager.getAccountsByType(TYPE)) {
                if (NAME.equals(existing.name)) {
                    configure(existing);
                    return true;
                }
            }
            return false;
        }
        configure(account);
        return true;
    }

    public static void remove(Context context) {
        AccountManager manager = AccountManager.get(context.getApplicationContext());
        for (Account account : manager.getAccountsByType(TYPE)) {
            try {
                manager.removeAccountExplicitly(account);
            } catch (RuntimeException ignored) {
                // The contact rows are also explicitly deleted by the caller.
            }
        }
    }

    private static void configure(Account account) {
        try {
            ContentResolver.setIsSyncable(account, ContactsContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, false);
        } catch (RuntimeException ignored) {
            // Contact creation still reports its own failure state.
        }
    }
}
