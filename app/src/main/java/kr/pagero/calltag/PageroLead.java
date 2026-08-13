package kr.pagero.calltag;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 페이지로 문의 1건. 서버 payload를 가능한 한 손실 없이 앱 모델로 보존한다. */
public final class PageroLead {
    public final long id;
    public final String eventId;
    public final String siteId;
    public final String pageTitle;
    public final String customerName;
    public final String phone;
    public final String normalizedPhone;
    public final String email;
    public final String inquiryContent;
    public final List<InquiryField> inquiryFields;
    public final String sourceUrl;
    public final String campaign;
    public final long submittedAt;
    public final String metadataJson;

    private PageroLead(
            long id,
            String eventId,
            String siteId,
            String pageTitle,
            String customerName,
            String phone,
            String normalizedPhone,
            String email,
            String inquiryContent,
            List<InquiryField> inquiryFields,
            String sourceUrl,
            String campaign,
            long submittedAt,
            String metadataJson) {
        this.id = id;
        this.eventId = safe(eventId);
        this.siteId = safe(siteId);
        this.pageTitle = safe(pageTitle);
        this.customerName = safe(customerName).isEmpty() ? "이름 없는 고객" : safe(customerName);
        this.phone = safe(phone);
        this.normalizedPhone = safe(normalizedPhone);
        this.email = safe(email);
        this.inquiryContent = safe(inquiryContent);
        this.inquiryFields = inquiryFields == null ? new ArrayList<>() : new ArrayList<>(inquiryFields);
        this.sourceUrl = safe(sourceUrl);
        this.campaign = safe(campaign);
        this.submittedAt = submittedAt > 0L ? submittedAt : System.currentTimeMillis();
        this.metadataJson = safe(metadataJson);
    }

    public static PageroLead fromJson(JSONObject value) {
        if (value == null) throw new IllegalArgumentException("페이지로 문의 데이터가 없습니다.");
        JSONObject customer = value.optJSONObject("customer");
        JSONObject inquiry = value.optJSONObject("inquiry");
        JSONObject metadata = value.optJSONObject("metadata");
        if (customer == null) customer = new JSONObject();
        if (inquiry == null) inquiry = new JSONObject();
        if (metadata == null) metadata = new JSONObject();

        long id = value.optLong("id", 0L);
        String eventId = value.optString("eventId", "");
        String phone = customer.optString("phone", "");
        String normalized = customer.optString("normalizedPhone", "");
        if (normalized.isEmpty()) normalized = PhoneNumberNormalizer.normalize(phone);
        if (id <= 0L || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("페이지로 문의 식별자가 없습니다.");
        }
        if (normalized.length() < 8) {
            throw new IllegalArgumentException("페이지로 고객 전화번호가 올바르지 않습니다.");
        }

        List<InquiryField> fields = extractInquiryFields(metadata);
        return new PageroLead(
                id,
                eventId,
                value.optString("siteId", ""),
                metadata.optString("pageTitle", ""),
                customer.optString("name", ""),
                phone,
                normalized,
                customer.optString("email", ""),
                inquiry.optString("content", ""),
                fields,
                inquiry.optString("sourceUrl", ""),
                inquiry.optString("campaign", ""),
                value.optLong("submittedAt", System.currentTimeMillis()),
                metadata.toString());
    }

    /** 고객이 실제로 입력한 문의 내용을 우선해서 보여주는 CRM 메모. */
    public String memoLine() {
        StringBuilder out = new StringBuilder("[페이지로 문의]");
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
        if (!inquiryContent.isEmpty()) {
            out.append("\n\n문의내용: ").append(inquiryContent);
        }
        out.append("\n접수: ").append(submittedLabel());
        if (!pageName().isEmpty()) out.append("\n페이지: ").append(pageName());
        if (!campaign.isEmpty()) out.append("\n캠페인: ").append(campaign);
        if (!sourceUrl.isEmpty()) out.append("\n접수 URL: ").append(sourceUrl);
        return out.toString();
    }

    /** 상담이력도 축약본이 아니라 문의 원문/문답을 그대로 남긴다. */
    public String interactionNote() {
        StringBuilder out = new StringBuilder();
        if (!inquiryFields.isEmpty()) {
            for (InquiryField field : inquiryFields) {
                if (field.value.isEmpty()) continue;
                if (out.length() > 0) out.append('\n');
                out.append(field.label).append(": ").append(field.value);
            }
        }
        if (!inquiryContent.isEmpty()) {
            if (out.length() > 0) out.append("\n\n");
            out.append("문의내용: ").append(inquiryContent);
        }
        if (out.length() == 0) out.append("페이지로 문의 접수");
        return out.toString();
    }

    public String pageName() {
        return !pageTitle.isEmpty() ? pageTitle : siteId;
    }

    public String submittedLabel() {
        return new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(submittedAt);
    }

    public String fieldValue(String label) {
        String wanted = safe(label);
        if (wanted.isEmpty()) return "";
        for (InquiryField field : inquiryFields) {
            if (wanted.equalsIgnoreCase(field.label) || wanted.equalsIgnoreCase(field.id)) {
                return field.value;
            }
        }
        return "";
    }

    private static List<InquiryField> extractInquiryFields(JSONObject metadata) {
        List<InquiryField> result = new ArrayList<>();
        Set<String> consumed = new HashSet<>();

        JSONArray answers = metadata.optJSONArray("answers");
        if (answers != null) {
            for (int index = 0; index < answers.length(); index++) {
                Object raw = answers.opt(index);
                if (!(raw instanceof JSONObject)) continue;
                JSONObject answer = (JSONObject) raw;
                String id = first(answer, "id", "fieldId", "key", "name");
                String label = first(answer, "label", "question", "title", "name", "id", "key");
                String answerValue = jsonValue(firstObject(answer, "value", "answer", "text", "rawValue"));
                if (label.isEmpty() || answerValue.isEmpty()) continue;
                int order = answer.has("order") ? answer.optInt("order", index + 1) : index + 1;
                result.add(new InquiryField(id, label, answerValue, order));
                consumed.add(normalizeKey(id));
                consumed.add(normalizeKey(label));
            }
        }

        JSONObject values = metadata.optJSONObject("values");
        if (values != null) {
            Iterator<String> keys = values.keys();
            int order = result.size() + 1;
            while (keys.hasNext()) {
                String key = keys.next();
                String normalizedKey = normalizeKey(key);
                if (isTechnicalField(normalizedKey) || consumed.contains(normalizedKey)) continue;
                Object raw = values.opt(key);
                String label = key;
                String text;
                if (raw instanceof JSONObject) {
                    JSONObject nested = (JSONObject) raw;
                    label = first(nested, "label", "question", "title", "name");
                    if (label.isEmpty()) label = key;
                    text = jsonValue(firstObject(nested, "value", "answer", "text", "rawValue"));
                } else {
                    text = jsonValue(raw);
                }
                if (text.isEmpty()) continue;
                result.add(new InquiryField(key, label, text, order++));
                consumed.add(normalizedKey);
            }
        }
        return result;
    }

    private static boolean isTechnicalField(String key) {
        if (key.isEmpty()) return true;
        return key.equals("name") || key.equals("customername")
                || key.equals("phone") || key.equals("tel") || key.equals("mobile")
                || key.equals("email") || key.equals("message") || key.equals("content")
                || key.equals("inquiry") || key.equals("inquirycontent")
                || key.equals("sourceurl") || key.equals("url") || key.equals("referrer")
                || key.equals("campaign") || key.startsWith("utm")
                || key.equals("pageslug") || key.equals("pageid") || key.equals("projectid");
    }

    private static String normalizeKey(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]", "");
    }

    private static String first(JSONObject value, String... keys) {
        for (String key : keys) {
            String text = safe(value.optString(key, ""));
            if (!text.isEmpty()) return text;
        }
        return "";
    }

    private static Object firstObject(JSONObject value, String... keys) {
        for (String key : keys) {
            if (value.has(key) && !value.isNull(key)) return value.opt(key);
        }
        return null;
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
        public final String id;
        public final String label;
        public final String value;
        public final int order;

        InquiryField(String id, String label, String value, int order) {
            this.id = safe(id);
            this.label = safe(label).isEmpty() ? "추가 문의정보" : safe(label);
            this.value = safe(value);
            this.order = order;
        }
    }
}
