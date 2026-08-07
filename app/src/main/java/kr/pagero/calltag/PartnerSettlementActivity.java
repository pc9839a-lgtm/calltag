package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/** 앱에서 콜태그×페이지로 파트너 정산 웹으로 바로 연결한다. */
public final class PartnerSettlementActivity extends Activity {
    public static final String SETTLEMENT_URL = "https://calltag.pagero.kr/web/settlement";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SETTLEMENT_URL)));
        } catch (RuntimeException error) {
            Toast.makeText(this, "정산 페이지를 열지 못했습니다.", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
