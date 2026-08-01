package kr.pagero.calltag;

import android.content.Context;

import java.util.List;
import java.util.Locale;

/** 고객 source와 실제 통화 interaction을 함께 사용해 화면용 유입 경로를 판별한다. */
public final class CustomerSourceResolver {
    public static final String PAGERO = "페이지로 유입";
    public static final String CALL = "전화 유입";
    public static final String DIRECT = "직접 등록";

    private CustomerSourceResolver() {}

    public static String label(Context context, Customer customer) {
        if (customer == null) return DIRECT;
        String source = safe(customer.source).toLowerCase(Locale.KOREA);
        if (source.contains("pagero")
                || source.contains("페이지로")
                || source.contains("landing")
                || source.contains("lead_form")) {
            return PAGERO;
        }
        if (source.contains("call") || source.contains("phone") || hasCallInteraction(context, customer.id)) {
            return CALL;
        }
        return DIRECT;
    }

    private static boolean hasCallInteraction(Context context, long customerId) {
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            List<InteractionRecord> rows = db.listInteractionsForCustomer(customerId);
            for (InteractionRecord row : rows) {
                String type = safe(row.type).toUpperCase(Locale.US);
                if (type.contains("CALL") || type.contains("PHONE")) return true;
            }
            return false;
        } finally {
            db.close();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
