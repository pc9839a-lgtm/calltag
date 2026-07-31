package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 전화 고객관리 제외와 독립된 문자 발송 제외 정책 저장소.
 *
 * 최종 발송 직전에는 반드시 evaluate()를 다시 호출해야 한다.
 */
public final class MessageExclusionStore {
    private static final String PREFS = "calltag_message_exclusions";
    private static final String KEY_RULES = "rules_v1";

    public static final int FLAG_AUTO = 1;
    public static final int FLAG_ALL = 1 << 1;
    public static final int FLAG_INCOMING = 1 << 2;
    public static final int FLAG_OUTGOING = 1 << 3;
    public static final int FLAG_MISSED = 1 << 4;
    public static final int FLAG_FOLLOW_UP = 1 << 5;

    private MessageExclusionStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void save(Context context, long customerId,
                                         String displayName, String phone, int flags) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        if (normalized.length() < 8) {
            throw new IllegalArgumentException("전화번호를 확인해주세요.");
        }
        Map<String, Rule> rules = readMap(context);
        if (flags == 0) {
            rules.remove(normalized);
        } else {
            rules.put(normalized, new Rule(
                    customerId,
                    safe(displayName).trim(),
                    safe(phone).trim(),
                    normalized,
                    flags,
                    System.currentTimeMillis()));
        }
        write(context, rules.values());
    }

    public static synchronized void remove(Context context, String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        Map<String, Rule> rules = readMap(context);
        if (rules.remove(normalized) != null) write(context, rules.values());
    }

    public static synchronized Rule find(Context context, String phone) {
        return readMap(context).get(PhoneNumberNormalizer.normalize(phone));
    }

    public static synchronized List<Rule> list(Context context) {
        List<Rule> rules = new ArrayList<>(readMap(context).values());
        Collections.sort(rules, (left, right) -> Long.compare(right.updatedAt, left.updatedAt));
        return rules;
    }

    public static Decision evaluate(Context context, long customerId,
                                    String phone, String triggerType) {
        Rule rule = find(context, phone);
        if (rule == null) return Decision.allowed();

        if ((rule.flags & FLAG_ALL) != 0) {
            return Decision.blocked("전체 문자 제외 고객입니다.", rule);
        }

        boolean manual = MessageAutomationManager.TRIGGER_MANUAL.equals(triggerType);
        if (manual) return Decision.allowed();

        int typeFlag = flagForTrigger(triggerType);
        if (typeFlag != 0 && (rule.flags & typeFlag) != 0) {
            return Decision.blocked(typeLabel(typeFlag) + " 제외 고객입니다.", rule);
        }

        if (MessageAutomationManager.TRIGGER_CONNECTED.equals(triggerType)
                && ((rule.flags & FLAG_INCOMING) != 0 || (rule.flags & FLAG_OUTGOING) != 0)) {
            return Decision.blocked("수신·발신 유형 제외 고객입니다.", rule);
        }

        if ((rule.flags & FLAG_AUTO) != 0 && isAutomaticTrigger(triggerType)) {
            return Decision.blocked("자동문자 제외 고객입니다.", rule);
        }
        return Decision.allowed();
    }

    public static String summary(int flags) {
        if (flags == 0) return "제외 없음";
        if ((flags & FLAG_ALL) != 0) return "전체 문자 제외";
        List<String> labels = new ArrayList<>();
        if ((flags & FLAG_AUTO) != 0) labels.add("자동문자");
        if ((flags & FLAG_INCOMING) != 0) labels.add("수신");
        if ((flags & FLAG_OUTGOING) != 0) labels.add("발신");
        if ((flags & FLAG_MISSED) != 0) labels.add("부재중");
        if ((flags & FLAG_FOLLOW_UP) != 0) labels.add("후속");
        return labels.isEmpty() ? "제외 없음" : String.join(" · ", labels) + " 제외";
    }

    private static boolean isAutomaticTrigger(String triggerType) {
        return !MessageAutomationManager.TRIGGER_MANUAL.equals(triggerType);
    }

    private static int flagForTrigger(String triggerType) {
        if (MessageAutomationManager.TRIGGER_INCOMING.equals(triggerType)) return FLAG_INCOMING;
        if (MessageAutomationManager.TRIGGER_OUTGOING.equals(triggerType)) return FLAG_OUTGOING;
        if (MessageAutomationManager.TRIGGER_MISSED.equals(triggerType)) return FLAG_MISSED;
        if (MessageAutomationManager.TRIGGER_DELAYED.equals(triggerType)) return FLAG_FOLLOW_UP;
        return 0;
    }

    private static String typeLabel(int flag) {
        if (flag == FLAG_INCOMING) return "수신 자동문자";
        if (flag == FLAG_OUTGOING) return "발신 자동문자";
        if (flag == FLAG_MISSED) return "부재중 문자";
        if (flag == FLAG_FOLLOW_UP) return "후속문자";
        return "문자 발송";
    }

    private static Map<String, Rule> readMap(Context context) {
        Map<String, Rule> result = new LinkedHashMap<>();
        String raw = prefs(context).getString(KEY_RULES, "[]");
        try {
            JSONArray array = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String normalized = object.optString("normalizedPhone", "");
                if (normalized.length() < 8) continue;
                result.put(normalized, new Rule(
                        object.optLong("customerId", 0L),
                        object.optString("displayName", ""),
                        object.optString("phone", normalized),
                        normalized,
                        object.optInt("flags", 0),
                        object.optLong("updatedAt", 0L)));
            }
        } catch (JSONException ignored) {
            // 손상된 저장값은 빈 목록으로 복구한다.
        }
        return result;
    }

    private static void write(Context context, Iterable<Rule> rules) {
        JSONArray array = new JSONArray();
        for (Rule rule : rules) {
            try {
                JSONObject object = new JSONObject();
                object.put("customerId", rule.customerId);
                object.put("displayName", rule.displayName);
                object.put("phone", rule.phone);
                object.put("normalizedPhone", rule.normalizedPhone);
                object.put("flags", rule.flags);
                object.put("updatedAt", rule.updatedAt);
                array.put(object);
            } catch (JSONException ignored) {
                // JSONObject primitive values do not normally fail.
            }
        }
        prefs(context).edit().putString(KEY_RULES, array.toString()).apply();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Rule {
        public final long customerId;
        public final String displayName;
        public final String phone;
        public final String normalizedPhone;
        public final int flags;
        public final long updatedAt;

        Rule(long customerId, String displayName, String phone,
             String normalizedPhone, int flags, long updatedAt) {
            this.customerId = customerId;
            this.displayName = displayName;
            this.phone = phone;
            this.normalizedPhone = normalizedPhone;
            this.flags = flags;
            this.updatedAt = updatedAt;
        }
    }

    public static final class Decision {
        public final boolean blocked;
        public final String reason;
        public final Rule rule;

        private Decision(boolean blocked, String reason, Rule rule) {
            this.blocked = blocked;
            this.reason = reason;
            this.rule = rule;
        }

        static Decision allowed() {
            return new Decision(false, "", null);
        }

        static Decision blocked(String reason, Rule rule) {
            return new Decision(true, reason, rule);
        }
    }
}
