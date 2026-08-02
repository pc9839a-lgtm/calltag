package kr.pagero.calltag;

import org.json.JSONObject;

public final class PageroLead {
    public final long id;
    public final String eventId;
    public final String siteId;
    public final String customerName;
    public final String phone;
    public final String normalizedPhone;
    public final String email;
    public final String inquiryContent;
    public final String sourceUrl;
    public final String campaign;
    public final long submittedAt;

    private PageroLead(
            long id,
            String eventId,
            String siteId,
            String customerName,
            String phone,
            String normalizedPhone,
            String email,
            String inquiryContent,
            String sourceUrl,
            String campaign,
            long submittedAt) {
        this.id = id;
        this.eventId = safe(eventId);
        this.siteId = safe(siteId);
        this.customerName = safe(customerName).isEmpty() ? "이름 없는 고객" : safe(customerName);
        this.phone = safe(phone);
        this.normalizedPhone = safe(normalizedPhone);
        this.email = safe(email);
        this.inquiryContent = safe(inquiryContent);
        this.sourceUrl = safe(sourceUrl);
        this.campaign = safe(campaign);
        this.submittedAt = submittedAt > 0L ? submittedAt : System.currentTimeMillis();
    }

    public static PageroLead fromJson(JSONObject value) {
        if (value == null) throw new IllegalArgumentException("페이지로 문의 데이터가 없습니다.");
        JSONObject customer = value.optJSONObject("customer");
        JSONObject inquiry = value.optJSONObject("inquiry");
        if (customer == null) customer = new JSONObject();
        if (inquiry == null) inquiry = new JSONObject();

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

        return new PageroLead(
                id,
                eventId,
                value.optString("siteId", ""),
                customer.optString("name", ""),
                phone,
                normalized,
                customer.optString("email", ""),
                inquiry.optString("content", ""),
                inquiry.optString("sourceUrl", ""),
                inquiry.optString("campaign", ""),
                value.optLong("submittedAt", System.currentTimeMillis()));
    }

    public String memoLine() {
        StringBuilder line = new StringBuilder("[페이지로]");
        if (!inquiryContent.isEmpty()) line.append(' ').append(inquiryContent);
        if (!siteId.isEmpty()) line.append("\n랜딩페이지: ").append(siteId);
        if (!campaign.isEmpty()) line.append("\n캠페인: ").append(campaign);
        if (!sourceUrl.isEmpty()) line.append("\n접수 URL: ").append(sourceUrl);
        return line.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
