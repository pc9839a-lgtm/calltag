package kr.pagero.calltag;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public final class MmsComposeActivity extends Activity {
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_OWNER_SCOPE = "owner_scope";
    public static final String EXTRA_WORK_EPOCH = "work_epoch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1L);
        boolean opened = false;
        try {
            opened = AccountDataScope.fingerprint(AccountDataScope.requireOwner(this))
                    .equals(getIntent().getStringExtra(EXTRA_OWNER_SCOPE))
                    && AccountDataScope.workEpoch(this)
                    .equals(getIntent().getStringExtra(EXTRA_WORK_EPOCH))
                    && MmsComposer.openComposer(this, messageId);
        } catch (IllegalStateException ignored) {
            // A stale MMS notification may belong to another authenticated owner.
        }
        if (!opened) {
            Toast.makeText(this, "이미지 문자 작성창을 열지 못했습니다.",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
