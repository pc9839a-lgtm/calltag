package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;

/** 만료 후 기록 열람은 유지하고 신규 자동화 실행만 차단한다. Android 권한과 요금제를 구분한다. */
public final class FeatureAccessGate {
    public static final String MESSAGE = "message";
    public static final String PHONE = "phone";

    private FeatureAccessGate() {}

    public static void open(Context context, Class<?> destination, String feature) {
        if (!allowed(context, feature)) {
            showPlanRequired(context, feature);
            return;
        }
        if (MESSAGE.equals(feature) && !SetupRequirements.hasSms(context)) {
            if (context instanceof Activity) {
                context.startActivity(FeaturePermissionActivity.intent(
                        (Activity) context, FeaturePermissionActivity.KIND_SMS, destination));
            } else {
                context.startActivity(new Intent(context, destination)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return;
        }
        if (PHONE.equals(feature) && !SetupRequirements.hasCoreRuntimePermissions(context)) {
            Intent setup = SetupRequirements.requiredSetupIntent(context);
            if (!(context instanceof Activity)) setup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(setup);
            return;
        }
        context.startActivity(new Intent(context, destination));
    }

    public static boolean require(Context context, String feature) {
        if (!allowed(context, feature)) {
            showPlanRequired(context, feature);
            return false;
        }
        if (MESSAGE.equals(feature) && !SetupRequirements.hasSms(context)) {
            if (context instanceof Activity) {
                context.startActivity(FeaturePermissionActivity.intent(
                        (Activity) context, FeaturePermissionActivity.KIND_SMS, null));
            }
            return false;
        }
        if (PHONE.equals(feature) && !SetupRequirements.hasCoreRuntimePermissions(context)) {
            Intent setup = SetupRequirements.requiredSetupIntent(context);
            if (!(context instanceof Activity)) setup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(setup);
            return false;
        }
        return true;
    }

    public static boolean allowed(Context context, String feature) {
        if (MESSAGE.equals(feature)) return FeatureEntitlementStore.hasMessageAccess(context);
        if (PHONE.equals(feature)) return FeatureEntitlementStore.hasPhoneAccess(context);
        return true;
    }

    public static void showPlanRequired(Context context, String feature) {
        new AlertDialog.Builder(context, R.style.Theme_CallTag_Dialog)
                .setTitle("요금제가 필요합니다")
                .setMessage(message(feature))
                .setNegativeButton("취소", null)
                .setPositiveButton("요금제 보기", (dialog, which) -> context.startActivity(
                        new Intent(context, BillingEntitlementActivity.class)))
                .show();
    }

    private static String message(String feature) {
        if (MESSAGE.equals(feature)) {
            return "문자 기능을 이용할 수 있는 요금제가 필요합니다. 기존 고객·발송 기록은 계속 확인할 수 있습니다.";
        }
        return "전화관리 기능을 이용할 수 있는 요금제가 필요합니다. 기존 고객·상담 기록은 계속 확인할 수 있습니다.";
    }
}
