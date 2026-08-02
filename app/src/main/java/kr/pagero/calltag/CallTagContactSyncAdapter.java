package kr.pagero.calltag;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

/** No-op adapter. Contact rows are maintained immediately by ContactNameSyncManager. */
public final class CallTagContactSyncAdapter extends AbstractThreadedSyncAdapter {
    public CallTagContactSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        ContactNameSyncManager.requestSyncAll(getContext());
    }
}
