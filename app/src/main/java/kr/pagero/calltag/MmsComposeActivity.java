package kr.pagero.calltag;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public final class MmsComposeActivity extends Activity {
    public static final String EXTRA_MESSAGE_ID = "message_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long messageId = getIntent().getLongExtra(EXTRA_MESSAGE_ID, -1L);
        boolean opened = MmsComposer.openComposer(this, messageId);
        if (!opened) {
            Toast.makeText(this, "메시지 앱을 열지 못했습니다. 발송 내역에서 상태를 확인해주세요.",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
