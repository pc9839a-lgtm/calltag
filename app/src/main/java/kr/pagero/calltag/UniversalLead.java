package kr.pagero.calltag;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** CallTag Universal Lead API에서 내려오는 정규화된 문의 1건. */
public final class UniversalLead {
    public final long id;
    public final String eventId;
    public final String externalId;
    public final String customerId;
    public final String connectionId;
    public final String sourceType;
    public final String sourceName;
    public final String sourceProvider;
    public final String customerName;
    public final String phone;
    public final String normalizedPhone;
    public final String email;
    public final String inquiryContent;
    public final List<InquiryField> inquiryFields;
    public final long submittedAt;
    public final String metadataJson;

    private UniversalLead(
            long id,
            String eventId,
            String externalId,
            String customerId,
            String connectionId,
            String sourceType,
            String sourceName,
            String sourceProvider,
            String customerName,
            String phone,
            String normalizedPhone,
            String email,
            String inquiryContent,
            List<InquiryField> inquiryFields,
            long submittedAt,
            String metadataJson) {
        this.id = id;
        this.eventId = safe(eventId);
        this.externalId = safe(externalId);
        this.customerId = safe(customerId);
        this.connectionId = safe(connectionId);
        this.sourceType = safe(sourceType);
        this.sourceName = safe(sourceName);
        this.sourceProvider = safe(sourceProvider);
        this.customerName = safe(customerName).isEmpty() ? "이름 없는 고객" : safe(customerName);
        this.phone = safe(phone);
        this.normalizedPhone = safe(normalizedPhone);
        this.email = safe(email);
        this.inquiryContent = safe(inquiryContent);
        this.inquiryFields = inquiryFields == null ? new ArrayList<>() : new ArrayList<>(inquiryFields);
        this.submittedAt = submittedAt > 0L ? submittedAt : System.currentTimeMillis();
        this.metadataJson = safe(metadataJson);
    }

    public static UniversalLead fromJson(JSONObject value) {
        if (value == null) throw new IllegalArgumentException("외부 문의 데이터가 없습니다.");
        JSONObject source = value.optJSONObject("source");
        JSONObject customer = value.optJSONObject("customer");
        JSONObject inquiry = value.optJSONObject("inquiry");
        JSONObject metadata = value.optJSONObject("metadata");
        if (source == null) source = new JSONObject();
        if (customer == null) customer = new JSONObject();
        if (inquiry == null) inquiry = new JSONObject();
        if (metadata == null) metadata = new JSONObject();

        long id = value.optLong("id", 0L);
        String eventId = value.optString("eventId", "");
        String phone = customer.optString("phone", "");
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (id <= 0L || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("외부 문의 식별자가 없습니다.");
        }
        if (normalized.length() < 8) {
            throw new IllegalArgumentException("외부 문의 고객 전화번호가 올바르지 않습니다.");
        }

        List<InquiryField> fields = new ArrayList<>();
        JSONArray rawFields = inquiry.optJSONArray("fields");
        if (rawFields != null) {
            for (int index = 0; index < rawFields.length(); index++) {
                JSONObject raw = rawFields.optJSONObject(index);
                if (raw == null) continue;
                String key = first(raw, "key", "id", "name");
                String label = first(raw, "label", "question", "title", "name", "key", "id");
                String fieldValue = jsonValue(raw.opt("value"));
                if (fieldValue.isEmpty()) continue;
                int order = raw.has("order") ? raw.optInt("order", index + 1) : index + 1;
                fields.add(new InquiryField(key, label, fieldValue, order));
            }
        }

        return new UniversalLead(
                id,
                eventId,
                value.optString("externalId", ""),
                value.optString("customerId", ""),
                value.optString("connectionId", ""),
                source.optString("type", ""),
                source.optString("name", ""),
                source.optString("provider", ""),
                customer.optString("name", ""),
                phone,
                normalized,
                customer.optString("email", ""),
                inquiry.optString("content", ""),
                fields,
                value.optLong("submittedAt", System.currentTimeMillis()),
                metadata.toString());
    }

    public String sourceLabel() {
        if (!sourceName.isEmpty()) return sourceName;
        if (!sourceProvider.isEmpty()) return sourceProvider;
        if (!sourceType.isEmpty()) return sourceType;
        return "외부 문의";
    }

    public String memoLine() {
        StringBuilder out = new StringBuilder("[").append(sourceLabel()).append(" 문의]");
        if (!customerName.isEmpty()) out.append("\n고객명: ").append(customerName);
        if (!phone.isEmpty()) out.append("\n연락처: ").append(phone);
        if (!email.isEmpty()) out.append("\n이메일: ").append(email);
        if (!inquiryFields.isEmpty()) {
            out.append("\n\n문의 항목");
            for (InquiryField field : inquiryFields) {
                if (field.value.isEmpty()) continue;
                out.append("\n- ").append(field.label).append(": ").append(field.value);
            }
        }
        if (!inquiryContent.isEmpty()) out.append("\n\n문의내용: ").append(inquiryContent);
        out.append("\n접수: ").append(submittedLabel());
        if (!sourceProvider.isEmpty() && !sourceProvider.equalsIgnoreCase(sourceLabel())) {
            out.append("\nProvider: ").append(sourceProvider);
        }
        return out.toString();
    }

    public String interactionNote() {
        StringBuilder out = new StringBuilder();
        for (InquiryField field : inquiryFields) {
            if (field.value.isEmpty()) continue;
            if (out.length() > 0) out.append('\n');
            out.append(field.label).append(": ").append(field.value);
        }
        if (!inquiryContent.isEmpty()) {
            if (out.length() > 0) out.append("\n\n");
            out.append("문의내용: ").append(inquiryContent);
        }
        if (out.length() == 0) out.append(sourceLabel()).append(" 문의 접수");
        return out.toString();
    }

    public String submittedLabel() {
        return new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(submittedAt);
    }

    private static String first(JSONObject value, String... keys) {
        for (String key : keys) {
            String text = safe(value.optString(key, ""));
            if (!text.isEmpty()) return text;
        }
        return "";
    }

    private static String jsonValue(Object value) {
        if (value == null || value == JSONObject.NULL) return "";
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                String item = jsonValue(array.opt(i));
                if (item.isEmpty()) continue;
                if (out.length() > 0) out.append(", ");
                out.append(item);
            }
            return out.toString();
        }
        if (value instanceof JSONObject) return value.toString();
        return safe(String.valueOf(value));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class InquiryField {
        public final String key;
        public final String label;
        public final String value;
        public final int order;

        InquiryField(String key, String label, String value, int order) {
            this.key = safe(key);
            this.label = safe(label).isEmpty() ? "추가 문의정보" : safe(label);
            this.value = safe(value);
            this.order = order;
        }
    }
}
