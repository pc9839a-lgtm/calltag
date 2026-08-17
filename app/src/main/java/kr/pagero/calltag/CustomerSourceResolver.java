package kr.pagero.calltag;

import java.util.Locale;

/** 화면에는 실제 페이지로 유입 고객만 출처 배지로 표시한다. */
public final class CustomerSourceResolver {
    public static final String PAGERO = "페이지로";

    private CustomerSourceResolver() {}

    public static String label(android.content.Context context, Customer customer) {
        return isPagero(customer) ? PAGERO : "";
    }

    public static boolean isPagero(Customer customer) {
        if (customer == null) return false;
        String source = safe(customer.source).toLowerCase(Locale.ROOT);
        return "페이지로".equals(source)
                || "pagero".equals(source)
                || "pagero_lead".equals(source)
                || source.startsWith("pagero:")
                || source.startsWith("페이지로:");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
