package kr.pagero.calltag;

import java.util.Locale;

/** 화면에는 페이지로에서 들어온 고객만 유입 배지로 표시한다. */
public final class CustomerSourceResolver {
    public static final String PAGERO = "페이지로";

    private CustomerSourceResolver() {}

    public static String label(android.content.Context context, Customer customer) {
        return isPagero(customer) ? PAGERO : "";
    }

    public static boolean isPagero(Customer customer) {
        if (customer == null) return false;
        String source = safe(customer.source).toLowerCase(Locale.KOREA);
        String memo = safe(customer.memo).toLowerCase(Locale.KOREA);
        return source.contains("pagero")
                || source.contains("페이지로")
                || source.contains("landing")
                || source.contains("lead_form")
                || memo.contains("[페이지로]")
                || memo.contains("pagero");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
