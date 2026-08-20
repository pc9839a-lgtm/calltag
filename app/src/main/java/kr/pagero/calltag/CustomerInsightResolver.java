package kr.pagero.calltag;

import java.util.List;

public final class CustomerInsightResolver {
    private CustomerInsightResolver() {}

    public static String latestMemo(CallTagDbHelper db, Customer customer) {
        if (db == null || customer == null) return "";
        List<InteractionRecord> records = db.listInteractionsForCustomer(customer.id);
        for (InteractionRecord record : records) {
            if (record == null) continue;
            if ("MEMO_EDIT".equals(record.type)) {
                return compact(record.note == null ? "" : record.note.trim());
            }
            if (record.note == null) continue;
            String note = record.note.trim();
            if (note.isEmpty()) continue;
            if (record.type != null && record.type.endsWith("_CALL")) return compact(note);
        }
        return compact(customer.memo == null ? "" : customer.memo.trim());
    }

    public static String compact(String value) {
        if (value == null) return "";
        String compact = value.trim().replaceAll("\\s+", " ");
        if (compact.length() <= 80) return compact;
        return compact.substring(0, 77) + "…";
    }
}
