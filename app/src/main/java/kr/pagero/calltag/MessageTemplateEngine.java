package kr.pagero.calltag;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageTemplateEngine {
    public static final String VAR_CUSTOMER_NAME = "{고객명}";
    public static final String VAR_PHONE = "{전화번호}";
    public static final String VAR_CALL_DATE = "{통화일자}";
    public static final String VAR_CALL_TIME = "{통화시간}";
    public static final String VAR_MY_NAME = "{내이름}";
    public static final String VAR_BRAND_NAME = "{상호명}";
    public static final String VAR_NEXT_SCHEDULE = "{다음일정}";

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{[^{}\\r\\n]+\\}");
    private static final List<String> SUPPORTED_VARIABLES = Arrays.asList(
            VAR_CUSTOMER_NAME,
            VAR_PHONE,
            VAR_CALL_DATE,
            VAR_CALL_TIME,
            VAR_MY_NAME,
            VAR_BRAND_NAME,
            VAR_NEXT_SCHEDULE
    );

    private MessageTemplateEngine() {}

    public static String normalizeLegacyAliases(String template) {
        return safe(template)
                .replace("{날짜}", VAR_CALL_DATE)
                .replace("{시간}", VAR_CALL_TIME);
    }

    public static ValidationResult validateTemplate(String template) {
        String normalized = normalizeLegacyAliases(template);
        Set<String> unsupported = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String variable = matcher.group();
            if (!SUPPORTED_VARIABLES.contains(variable)) unsupported.add(variable);
        }
        return new ValidationResult(normalized, new ArrayList<>(unsupported));
    }

    public static RenderResult render(Context context, String template,
                                      Customer customer, CallRecord record) {
        ValidationResult validation = validateTemplate(template);
        String rendered = validation.normalizedTemplate;

        String customerName = customer == null ? "고객" : cleanCustomerName(customer.displayName);
        String phone = firstNonEmpty(
                customer == null ? "" : customer.primaryPhone,
                record == null ? "" : record.phone);
        long callTime = record == null ? System.currentTimeMillis() : record.startedAt;
        String callDate = new SimpleDateFormat("M월 d일", Locale.KOREA)
                .format(new Date(callTime));
        String callClock = new SimpleDateFormat("a h:mm", Locale.KOREA)
                .format(new Date(callTime));
        String myName = context == null ? "" : safe(AuthSessionStore.name(context)).trim();
        String brandName = context == null ? "" : safe(AuthSessionStore.brand(context)).trim();
        String nextSchedule = context == null ? "" : nextScheduleText(context, customer);

        rendered = replace(rendered, VAR_CUSTOMER_NAME, customerName);
        rendered = replace(rendered, VAR_PHONE, phone);
        rendered = replace(rendered, VAR_CALL_DATE, callDate);
        rendered = replace(rendered, VAR_CALL_TIME, callClock);
        rendered = replace(rendered, VAR_MY_NAME, myName);
        rendered = replace(rendered, VAR_BRAND_NAME, brandName);
        rendered = replace(rendered, VAR_NEXT_SCHEDULE, nextSchedule);

        List<String> unresolved = findPlaceholders(rendered);
        return new RenderResult(rendered.trim(), validation.unsupportedVariables, unresolved);
    }

    public static List<String> findPlaceholders(String value) {
        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(safe(value));
        while (matcher.find()) variables.add(matcher.group());
        return new ArrayList<>(variables);
    }

    public static String supportedVariablesLabel() {
        return String.join("  ", SUPPORTED_VARIABLES);
    }

    public static String describeVariables(List<String> variables) {
        if (variables == null || variables.isEmpty()) return "";
        return String.join(", ", variables);
    }

    private static String nextScheduleText(Context context, Customer customer) {
        if (customer == null || customer.id <= 0L) return "";
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            List<FollowUpTask> tasks = db.listTasksForCustomer(customer.id);
            long now = System.currentTimeMillis();
            FollowUpTask nextFuture = null;
            FollowUpTask latestOverdue = null;
            for (FollowUpTask task : tasks) {
                if (task == null || task.isCompleted()) continue;
                if (task.dueAt >= now) {
                    if (nextFuture == null || task.dueAt < nextFuture.dueAt) nextFuture = task;
                } else if (latestOverdue == null || task.dueAt > latestOverdue.dueAt) {
                    latestOverdue = task;
                }
            }
            FollowUpTask selected = nextFuture == null ? latestOverdue : nextFuture;
            if (selected == null) return "";
            return new SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA)
                    .format(new Date(selected.dueAt));
        } finally {
            db.close();
        }
    }

    private static String replace(String source, String variable, String value) {
        if (safe(value).trim().isEmpty()) return source;
        return source.replace(variable, value.trim());
    }

    private static String cleanCustomerName(String value) {
        String result = safe(value).trim();
        if (result.isEmpty() || "이름없는고객".equals(result)
                || "이름 없는 고객".equals(result) || "이름 없음".equals(result)) {
            return "고객";
        }
        return result;
    }

    private static String firstNonEmpty(String first, String second) {
        String firstValue = safe(first).trim();
        return firstValue.isEmpty() ? safe(second).trim() : firstValue;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class ValidationResult {
        public final String normalizedTemplate;
        public final List<String> unsupportedVariables;

        ValidationResult(String normalizedTemplate, List<String> unsupportedVariables) {
            this.normalizedTemplate = normalizedTemplate;
            this.unsupportedVariables = unsupportedVariables;
        }

        public boolean isValid() {
            return unsupportedVariables.isEmpty();
        }
    }

    public static final class RenderResult {
        public final String body;
        public final List<String> unsupportedVariables;
        public final List<String> unresolvedVariables;

        RenderResult(String body, List<String> unsupportedVariables,
                     List<String> unresolvedVariables) {
            this.body = body;
            this.unsupportedVariables = unsupportedVariables;
            this.unresolvedVariables = unresolvedVariables;
        }

        public boolean isReady() {
            return unsupportedVariables.isEmpty() && unresolvedVariables.isEmpty()
                    && !body.trim().isEmpty();
        }
    }
}
