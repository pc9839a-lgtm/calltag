package kr.pagero.calltag;

import android.content.Context;
import android.database.Cursor;

import java.util.Locale;

/** 고객의 실제 문의 유입 출처를 화면 배지용 라벨로 정규화한다. */
public final class CustomerSourceResolver {
    public static final String PAGERO = "페이지로";
    private static final int MAX_BADGE_LABEL_LENGTH = 28;

    private CustomerSourceResolver() {}

    public static String label(Context context, Customer customer) {
        if (customer == null) return "";
        String source = safe(customer.source);

        // 기존 Customer read 경로가 source를 비워 반환하는 버전과도 호환한다.
        // 실제 값은 customers.source 컬럼에서 owner 로컬 DB 범위로만 보완한다.
        if (source.isEmpty() && context != null && customer.id > 0L) {
            source = storedSource(context, customer.id);
        }
        if (source.isEmpty()) return "";
        if (isPageroSource(source)) return PAGERO;
        return source.length() <= MAX_BADGE_LABEL_LENGTH
                ? source
                : source.substring(0, MAX_BADGE_LABEL_LENGTH) + "…";
    }

    public static boolean isPagero(Customer customer) {
        return customer != null && isPageroSource(customer.source);
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
