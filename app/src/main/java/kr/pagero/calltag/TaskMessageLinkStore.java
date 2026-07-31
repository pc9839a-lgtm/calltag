package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 일정과 후속문자의 연결 설정을 일정 ID 기준으로 보존한다. */
public final class TaskMessageLinkStore {
    private static final String PREFS = "calltag_task_message_links_v1";
    private static final String KEY_LINKS = "links_json";

    private TaskMessageLinkStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized Link find(Context context, long taskId) {
        if (taskId <= 0L) return null;
        for (Link link : readAll(context)) {
            if (link.taskId == taskId) return link.copy();
        }
        return null;
    }

    public static synchronized Link findByMessageId(Context context, long messageId) {
        if (messageId <= 0L) return null;
        for (Link link : readAll(context)) {
            if (link.messageId == messageId) return link.copy();
        }
        return null;
    }

    public static synchronized void save(Context context, Link value) {
        if (value == null || value.taskId <= 0L) return;
        List<Link> links = readAll(context);
        int index = indexOf(links, value.taskId);
        Link saved = value.copy();
        saved.updatedAt = System.currentTimeMillis();
        if (index < 0) links.add(saved);
        else links.set(index, saved);
        writeAll(context, links);
    }

    public static synchronized void remove(Context context, long taskId) {
        List<Link> links = readAll(context);
        int index = indexOf(links, taskId);
        if (index < 0) return;
        links.remove(index);
        writeAll(context, links);
    }

    private static int indexOf(List<Link> links, long taskId) {
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).taskId == taskId) return i;
        }
        return -1;
    }

    private static List<Link> readAll(Context context) {
        List<Link> links = new ArrayList<>();
        String raw = prefs(context).getString(KEY_LINKS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object != null) links.add(Link.fromJson(object));
            }
        } catch (JSONException ignored) {
            prefs(context).edit().remove(KEY_LINKS).apply();
        }
        return links;
    }

    private static void writeAll(Context context, List<Link> links) {
        JSONArray array = new JSONArray();
        for (Link link : links) array.put(link.toJson());
        prefs(context).edit().putString(KEY_LINKS, array.toString()).apply();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Link {
        public long taskId;
        public long customerId;
        public String mode;
        public long messageId;
        public String templateId;
        public String bodySnapshot;
        public int subscriptionId;
        public long taskDueAt;
        public long messageScheduledAt;
        public long updatedAt;

        public Link(long taskId, long customerId, String mode, long messageId,
                    String templateId, String bodySnapshot, int subscriptionId,
                    long taskDueAt, long messageScheduledAt, long updatedAt) {
            this.taskId = taskId;
            this.customerId = customerId;
            this.mode = safe(mode);
            this.messageId = messageId;
            this.templateId = safe(templateId);
            this.bodySnapshot = safe(bodySnapshot);
            this.subscriptionId = subscriptionId;
            this.taskDueAt = taskDueAt;
            this.messageScheduledAt = messageScheduledAt;
            this.updatedAt = updatedAt;
        }

        Link copy() {
            return new Link(taskId, customerId, mode, messageId, templateId,
                    bodySnapshot, subscriptionId, taskDueAt,
                    messageScheduledAt, updatedAt);
        }

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("task_id", taskId);
                object.put("customer_id", customerId);
                object.put("mode", mode);
                object.put("message_id", messageId);
                object.put("template_id", templateId);
                object.put("body_snapshot", bodySnapshot);
                object.put("subscription_id", subscriptionId);
                object.put("task_due_at", taskDueAt);
                object.put("message_scheduled_at", messageScheduledAt);
                object.put("updated_at", updatedAt);
            } catch (JSONException ignored) {
            }
            return object;
        }

        static Link fromJson(JSONObject object) {
            return new Link(
                    object.optLong("task_id", 0L),
                    object.optLong("customer_id", 0L),
                    object.optString("mode", ""),
                    object.optLong("message_id", 0L),
                    object.optString("template_id", ""),
                    object.optString("body_snapshot", ""),
                    object.optInt("subscription_id", -1),
                    object.optLong("task_due_at", 0L),
                    object.optLong("message_scheduled_at", 0L),
                    object.optLong("updated_at", 0L));
        }
    }
}
