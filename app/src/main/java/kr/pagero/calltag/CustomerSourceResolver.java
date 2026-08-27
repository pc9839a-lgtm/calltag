package kr.pagero.calltag;

import android.content.Context;
import android.database.Cursor;

import java.util.Locale;

/** 고객의 실제 문의 유입 출처를 화면 배지/필터용 라벨로 정규화한다. */
public final class CustomerSourceResolver {
    public static final String PAGERO = "페이지로";
    public static final String META = "Meta 광고";
    public static final String GOOGLE_FORMS = "Google Forms";
    public static final String WEBHOOK = "Webhook";
    public static final String DIRECT_API = "Direct API";
    private static final int MAX_BADGE_LABEL_LENGTH = 28;

    private CustomerSourceResolver() {}

    public static String label(Context context, Customer customer) {
        return normalizedLabel(rawSource(context, customer));
    }

    /** 사용자가 직접 등록한 고객은 source가 비어 있으므로, source가 있는 고객을 외부 유입으로 본다. */
    public static boolean isExternal(Context context, Customer customer) {
        return !rawSource(context, customer).isEmpty();
    }

    public static boolean isPagero(Customer customer) {
        return customer != null && isPageroSource(customer.source);
    }

    public static String rawSource(Context context, Customer customer) {
        if (customer == null) return "";
        String source = safe(customer.source);

        // source 복원이 누락된 구버전 로컬 DB/read 경로도 화면에서 안전하게 보완한다.
        if (source.isEmpty() && context != null && customer.id > 0L) {
            source = storedSource(context, customer.id);
        }
        return source;
    }

    public static String normalizedLabel(String value) {
        String source = safe(value);
        if (source.isEmpty()) return "";
        String lower = source.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        if (isPageroSource(source)) return PAGERO;
        if (lower.equals("meta") || lower.equals("facebook")
                || lower.equals("meta_lead_ads") || lower.equals("meta_lead_ad")
                || lower.equals("facebook_lead_ads") || lower.equals("facebook_lead_ad")) {
            return META;
        }
        if (lower.equals("google_forms") || lower.equals("google_form")
                || lower.equals("googleforms")) {
            return GOOGLE_FORMS;
        }
        if (lower.equals("generic_webhook") || lower.equals("webhook")
                || lower.equals("generic_web_hook")) {
            return WEBHOOK;
        }
        if (lower.equals("direct_api") || lower.equals("directapi")
                || lower.equals("api_direct")) {
            return DIRECT_API;
        }
        return source.length() <= MAX_BADGE_LABEL_LENGTH
                ? source
                : source.substring(0, MAX_BADGE_LABEL_LENGTH) + "…";
    }

    private static boolean isPageroSource(String value) {
        String source = safe(value).toLowerCase(Locale.ROOT);
        return "페이지로".equals(source)
                || "pagero".equals(source)
                || "pagero_lead".equals(source)
                || source.startsWith("pagero:")
                || source.startsWith("페이지로:");
    }

    private static String storedSource(Context context, long customerId) {
        try (CallTagDbHelper db = new CallTagDbHelper(context.getApplicationContext());
             Cursor cursor = db.getReadableDatabase().query(
                     "customers",
                     new String[]{"source"},
                     "id=?",
                     new String[]{String.valueOf(customerId)},
                     null,
                     null,
                     null,
                     "1")) {
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
