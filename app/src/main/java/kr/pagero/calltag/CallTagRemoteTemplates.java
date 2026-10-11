package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Public operator-curated template suggestions, explicitly user-imported.
 * No SMS send, auto-activation, modification of local user templates or
 * account-specific/private data is permitted by this class.
 */
public final class CallTagRemoteTemplates {
    private static final String URL = "https://calltag.pagero.kr/api/mobile-templates/v1";
    private static final String PREFS = "calltag_public_template_catalog_v1";
    private static final int MAX_BYTES = 8192;
    private static final Set<String> PURPOSES = new HashSet<>();
    static {
        Collections.addAll(PURPOSES,"incoming","outgoing","missed","follow_up","general");
    }

    private CallTagRemoteTemplates() {}

    public static final class Item {
        public final String id;
        public final String name;
        public final String body;
        public final String purpose;
        private Item(String id,String name,String body,String purpose) {
            this.id=id;this.name=name;this.body=body;this.purpose=purpose;
        }
    }

    public static List<Item> cached(Context context) {
        if (context == null) return Collections.emptyList();
        String raw = prefs(context).getString("catalog","");
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        try {
            return parse(new JSONObject(raw));
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    public static void refreshAsync(Context context, Runnable onComplete) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                refreshNow(app);
            } catch (Exception ignored) {
                // A network failure must not replace the last valid catalog.
            }
            if (onComplete != null) new Handler(Looper.getMainLooper()).post(onComplete);
        },"calltag-remote-template-catalog").start();
    }

    private static void refreshNow(Context context) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL(URL).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(4500);
            connection.setReadTimeout(4500);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept","application/json");
            connection.setRequestProperty("Cache-Control","no-cache");
            if (connection.getResponseCode() != 200) return;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] buffer = new byte[2048];
                int size;
                while ((size=stream.read(buffer)) != -1) {
                    if (output.size()+size > MAX_BYTES) return;
                    output.write(buffer,0,size);
                }
            }
            JSONObject value = new JSONObject(new String(
                    output.toByteArray(),StandardCharsets.UTF_8));
            List<Item> checked = parse(value);
            if (checked.isEmpty()) return;
            long revision = value.getLong("revision");
            SharedPreferences prefs = prefs(context);
            synchronized (CallTagRemoteTemplates.class) {
                if (revision <= prefs.getLong("revision",0L)) return;
                if (!prefs.edit().putString("catalog",value.toString())
                        .putLong("revision",revision).commit()) {
                    throw new IllegalStateException("template_catalog_cache_failed");
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static List<Item> parse(JSONObject value) throws Exception {
        if (value.optInt("schemaVersion",-1) != 1) return Collections.emptyList();
        Object rev = value.opt("revision");
        if (!(rev instanceof Number) || ((Number)rev).longValue() < 1L
                || ((Number)rev).doubleValue() != ((Number)rev).longValue()) {
            return Collections.emptyList();
        }
        JSONArray templates = value.optJSONArray("templates");
        if (templates == null || templates.length() < 1 || templates.length() > 12)
            return Collections.emptyList();
        List<Item> result = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i=0;i<templates.length();i++) {
            JSONObject item = templates.optJSONObject(i);
            if (item == null) return Collections.emptyList();
            if (!(item.opt("id") instanceof String)
                    || !(item.opt("name") instanceof String)
                    || !(item.opt("body") instanceof String)
                    || !(item.opt("purpose") instanceof String))
                return Collections.emptyList();
            String id=item.getString("id");
            String name=item.getString("name").trim();
            String body=item.getString("body").trim();
            String purpose=item.getString("purpose");
            if (!id.matches("[a-z0-9_-]{3,40}") || !ids.add(id)
                    || name.isEmpty() || name.length()>60
                    || body.isEmpty() || body.length()>500 || !PURPOSES.contains(purpose))
                return Collections.emptyList();
            MessageTemplateEngine.ValidationResult validation =
                    MessageTemplateEngine.validateTemplate(body);
            if (!validation.isValid() || !validation.normalizedTemplate.equals(body))
                return Collections.emptyList();
            result.add(new Item(id,name,body,purpose));
        }
        return result;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }
}
