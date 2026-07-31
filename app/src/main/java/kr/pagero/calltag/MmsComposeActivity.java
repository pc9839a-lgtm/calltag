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
            Toast.makeText(this, "이미지 문자 작성창을 열지 못했습니다.",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
