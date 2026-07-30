package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 콜태그 단일 APK 안에서 전화관리와 문자자동화 기능 권한을 분리한다.
 * 실제 스토어 결제 연동 전 개발 빌드는 통합 이용 권한을 기본값으로 사용한다.
 */
public final class FeatureEntitlementStore {
    public static final String PLAN_PHONE = "PHONE_1900";
    public static final String PLAN_MESSAGE = "MESSAGE_990";
    public static final String PLAN_BUNDLE = "BUNDLE_2500";

    public static final int PHONE_PRICE_KRW = 1900;
    public static final int MESSAGE_PRICE_KRW = 990;
    public static final int BUNDLE_PRICE_KRW = 2500;

    private static final String PREFS = "calltag_entitlements";
    private static final String KEY_PLAN = "active_plan";

    private FeatureEntitlementStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String activePlan(Context context) {
        String value = prefs(context).getString(KEY_PLAN, PLAN_BUNDLE);
        if (PLAN_PHONE.equals(value) || PLAN_MESSAGE.equals(value) || PLAN_BUNDLE.equals(value)) {
            return value;
        }
        return PLAN_BUNDLE;
    }

    public static void setActivePlanForDevelopment(Context context, String plan) {
        if (!PLAN_PHONE.equals(plan) && !PLAN_MESSAGE.equals(plan) && !PLAN_BUNDLE.equals(plan)) {
            throw new IllegalArgumentException("지원하지 않는 구독 상품입니다.");
        }
        prefs(context).edit().putString(KEY_PLAN, plan).apply();
    }

    public static boolean hasPhoneAccess(Context context) {
        String plan = activePlan(context);
        return PLAN_PHONE.equals(plan) || PLAN_BUNDLE.equals(plan);
    }

    public static boolean hasMessageAccess(Context context) {
        String plan = activePlan(context);
        return PLAN_MESSAGE.equals(plan) || PLAN_BUNDLE.equals(plan);
    }

    public static String planLabel(Context context) {
        String plan = activePlan(context);
        if (PLAN_PHONE.equals(plan)) return "전화관리 · 월 1,900원";
        if (PLAN_MESSAGE.equals(plan)) return "문자자동화 · 월 990원";
        return "통합 이용 · 월 2,500원";
    }
}
