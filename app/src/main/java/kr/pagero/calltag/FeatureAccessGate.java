package kr.pagero.calltag;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

/** 만료 후 기록 열람은 유지하고 신규 자동화 실행만 차단한다. */
public final class FeatureAccessGate {
    public static final String MESSAGE = "message";
    public static final String PHONE = "phone";

    private FeatureAccessGate() {}

    public static void open(Context context, Class<?> destination, String feature) {
        if (allowed(context, feature)) {
            context.startActivity(new Intent(context, destination));
            return;
        }
        new AlertDialog.Builder(context, R.style.Theme_CallTag_Dialog)
                .setTitle("이용권이 필요합니다")
                .setMessage(message(feature))
                .setNegativeButton("취소", null)
                .setPositiveButton("이용권 확인", (dialog, which) -> context.startActivity(
                        new Intent(context, BillingEntitlementActivity.class)))
                .show();
    }

    public static boolean allowed(Context context, String feature) {
        if (MESSAGE.equals(feature)) return FeatureEntitlementStore.hasMessageAccess(context);
        if (PHONE.equals(feature)) return FeatureEntitlementStore.hasPhoneAccess(context);
        return true;
    }

    private static String message(String feature) {
        if (MESSAGE.equals(feature)) {
            return "고객·발송 기록은 그대로 확인할 수 있습니다. 새 자동문자와 단체문자 발송은 이용권을 시작하면 다시 사용할 수 있습니다.";
        }
        return "고객·상담 기록은 그대로 확인할 수 있습니다. 통화 후 자동 정리는 이용권을 시작하면 다시 사용할 수 있습니다.";
    }
}
