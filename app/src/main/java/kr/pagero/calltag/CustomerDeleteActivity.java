package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

/** 고객목록 삭제 액션의 확인 전용 화면. 문자 발송 로그 DB는 감사 이력으로 유지한다. */
public final class CustomerDeleteActivity extends Activity {
    public static final String EXTRA_CUSTOMER_ID = "customer_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, 0L);
        if (customerId <= 0L) {
            finish();
            return;
        }
        CallTagDbHelper db = new CallTagDbHelper(this);
        Customer customer;
        try {
            customer = db.findCustomerById(customerId);
        } finally {
            db.close();
        }
        if (customer == null) {
            finish();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("고객 삭제")
                .setMessage(customer.displayName
                        + " 고객과 연결된 상담·할 일 기록을 콜태그에서 삭제합니다.\n\n문자 발송 이력은 안전 기록으로 유지됩니다.")
                .setNegativeButton("취소", (d, w) -> finish())
                .setPositiveButton("삭제", (d, w) -> delete(customerId))
                .setOnCancelListener(d -> finish())
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private void delete(long customerId) {
        CallTagDbHelper db = new CallTagDbHelper(this);
        int removed;
        try {
            // opportunities/interactions/follow_up_tasks는 FK ON DELETE CASCADE로 함께 정리된다.
            removed = db.getWritableDatabase().delete(
                    "customers", "id=?", new String[]{String.valueOf(customerId)});
        } finally {
            db.close();
        }
        Toast.makeText(this, removed > 0 ? "고객을 삭제했습니다." : "이미 삭제된 고객입니다.",
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
