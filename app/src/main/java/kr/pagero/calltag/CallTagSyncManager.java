package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CallTagSyncManager {
    public static final String ACTION_STATE_CHANGED =
            "kr.pagero.calltag.SECURE_SYNC_STATE_CHANGED";

    private static final long NORMAL_INTERVAL_MS = 15L * 60L * 1000L;
    private static final long PREPARING_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES_PER_RUN = 100;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final Object STATE_LOCK = new Object();
    private static boolean maintenance;

    private CallTagSyncManager() {}

    public static void request(Context context) {
        request(context, false);
    }

    public static void request(Context context, boolean force) {
        Context app = context.getApplicationContext();
        if (!CallTagSyncPreferenceStore.isEnabled(app)) return;
        if (!AuthSessionStore.hasSession(app)) return;

        CallTagSyncLocalStore store = new CallTagSyncLocalStore(app);
        if (!store.isReady()) {
            store.close();
            return;
        }
        CallTagSyncLocalStore.StatusSnapshot state = store.status();
        long interval = "PREPARING".equals(state.status)
                ? PREPARING_INTERVAL_MS : NORMAL_INTERVAL_MS;
        long now = System.currentTimeMillis();
        boolean throttled = !force && now - state.lastAttemptAt < interval;
        store.close();
        if (throttled || !acquireRunSlot()) return;

        EXECUTOR.execute(() -> {
            try {
                run(app);
            } finally {
                releaseRunSlot();
                broadcast(app);
            }
        });
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static boolean beginMaintenance() {
        synchronized (STATE_LOCK) {
            if (maintenance || RUNNING.get()) return false;
            maintenance = true;
            return true;
        }
    }

    public static void endMaintenance() {
        synchronized (STATE_LOCK) {
            maintenance = false;
        }
    }

    public static boolean isMaintenanceRunning() {
        synchronized (STATE_LOCK) {
            return maintenance;
        }
    }

    private static boolean acquireRunSlot() {
        synchronized (STATE_LOCK) {
            if (maintenance || RUNNING.get()) return false;
            RUNNING.set(true);
            return true;
        }
    }

    private static void releaseRunSlot() {
        synchronized (STATE_LOCK) {
            RUNNING.set(false);
        }
    }

    private static void run(Context context) {
        CallTagSyncLocalStore store = new CallTagSyncLocalStore(context);
        try {
            if (!store.isReady()) return;
            long now = System.currentTimeMillis();
            store.setLastAttemptAt(now);
            store.markStatus("SYNCING", "서버 준비 상태를 확인하고 있습니다.");
            broadcast(context);

            String session = AuthSessionStore.session(context);
            String deviceId = CallTagSyncDeviceStore.deviceId(context);

            // 고객 원문을 만들기 전에 민감정보가 없는 status 호출로 서버 활성화 여부를 확인한다.
            JSONObject statusResponse = CallTagSyncApiClient.status(session, deviceId);
            JSONObject serverStatus = statusResponse.optJSONObject("sync");
            long serverRecords = serverStatus == null
                    ? 0L : serverStatus.optLong("recordCount", 0L);

            if (!store.initialized()) {
                store.markStatus("RESTORING", "기존 데이터를 안전하게 확인하고 있습니다.");
                broadcast(context);
                bootstrap(context, store, session, deviceId);
                store.setInitialized(true);
            }

            store.markStatus("SCANNING", "기기에서 변경된 항목을 확인하고 있습니다.");
            CallTagSyncDataAdapter.scanLocal(context, store);
            pushPending(store, session, deviceId);
            pullChanges(context, store, session, deviceId);

            JSONObject finalStatus = CallTagSyncApiClient.status(session, deviceId);
            JSONObject finalSync = finalStatus.optJSONObject("sync");
            if (finalSync != null) serverRecords = finalSync.optLong("recordCount", serverRecords);
            store.markSuccess(serverRecords);
        } catch (CallTagSyncApiClient.ApiException error) {
            if ("CALLTAG_SYNC_NOT_ENABLED".equals(error.code)
                    || "CALLTAG_SYNC_KEY_INVALID".equals(error.code)
                    || error.status == 404
                    || error.status == 405) {
                store.markStatus("PREPARING",
                        "서버 데이터 보호 기능을 준비 중입니다. 기기 데이터는 그대로 유지됩니다.");
            } else if (error.status == 401 || error.status == 403) {
                store.markStatus("AUTH_REQUIRED", "로그인 상태를 다시 확인해주세요.");
            } else if (error.status == 429) {
                store.markStatus("WAITING", "요청이 많아 잠시 후 다시 시도합니다.");
            } else {
                store.markStatus("ERROR", safeMessage(error));
            }
        } catch (Exception error) {
            store.markStatus("ERROR", safeMessage(error));
        } finally {
            store.close();
        }
    }

    private static void bootstrap(
            Context context,
            CallTagSyncLocalStore store,
            String session,
            String deviceId) throws Exception {
        Long snapshotCursor = null;
        String afterType = "";
        String afterId = "";
        for (int page = 0; page < MAX_PAGES_PER_RUN; page++) {
            JSONObject response = CallTagSyncApiClient.bootstrap(
                    session, deviceId, snapshotCursor, afterType, afterId, PAGE_SIZE);
            if (snapshotCursor == null) snapshotCursor = response.optLong("snapshotCursor", 0L);
            JSONArray items = response.optJSONArray("items");
            CallTagSyncDataAdapter.applyRemote(context,
                    items == null ? new JSONArray() : items, store);
            if (response.optBoolean("complete", true)) {
                store.setCursor(snapshotCursor == null ? 0L : snapshotCursor);
                return;
            }
            JSONObject next = response.optJSONObject("nextAfter");
            if (next == null) throw new IllegalStateException("복구 다음 위치를 확인하지 못했습니다.");
            afterType = next.optString("entityType", "");
            afterId = next.optString("entityId", "");
            if (afterType.isEmpty() || afterId.isEmpty()) {
                throw new IllegalStateException("복구 다음 위치가 올바르지 않습니다.");
            }
        }
        throw new IllegalStateException("한 번에 복구할 데이터가 너무 많습니다. 다시 시도해주세요.");
    }

    private static void pushPending(
            CallTagSyncLocalStore store,
            String session,
            String deviceId) throws Exception {
        for (int page = 0; page < MAX_PAGES_PER_RUN; page++) {
            List<CallTagSyncLocalStore.PendingItem> pending = store.listPending(PAGE_SIZE);
            if (pending.isEmpty()) return;
            JSONArray items = new JSONArray();
            for (CallTagSyncLocalStore.PendingItem item : pending) {
                JSONObject body = new JSONObject();
                body.put("entityType", item.entityType);
                body.put("entityId", item.syncId);
                body.put("version", item.version);
                body.put("deleted", item.deleted);
                body.put("payload", item.deleted
                        ? new JSONObject() : new JSONObject(item.payloadJson));
                items.put(body);
            }
            JSONObject response = CallTagSyncApiClient.push(session, deviceId, items);
            JSONArray accepted = response.optJSONArray("accepted");
            int acceptedCount = 0;
            if (accepted != null) {
                for (int index = 0; index < accepted.length(); index++) {
                    JSONObject item = accepted.optJSONObject(index);
                    if (item == null) continue;
                    store.markAccepted(
                            item.optString("entityType", ""),
                            item.optString("entityId", ""),
                            item.optInt("version", 1));
                    acceptedCount++;
                }
            }
            JSONArray conflicts = response.optJSONArray("conflicts");
            if (conflicts != null && conflicts.length() > 0) {
                throw new IllegalStateException(
                        "다른 기기에서 변경된 데이터가 있어 다시 확인이 필요합니다.");
            }
            if (acceptedCount == 0) {
                throw new IllegalStateException("변경된 데이터를 서버에서 확인하지 못했습니다.");
            }
        }
        throw new IllegalStateException("한 번에 보낼 변경사항이 너무 많습니다. 다시 시도해주세요.");
    }

    private static void pullChanges(
            Context context,
            CallTagSyncLocalStore store,
            String session,
            String deviceId) throws Exception {
        long cursor = store.cursor();
        for (int page = 0; page < MAX_PAGES_PER_RUN; page++) {
            JSONObject response = CallTagSyncApiClient.pull(
                    session, deviceId, cursor, PAGE_SIZE);
            JSONArray items = response.optJSONArray("items");
            CallTagSyncDataAdapter.ApplyResult applied =
                    CallTagSyncDataAdapter.applyRemote(context,
                            items == null ? new JSONArray() : items, store);
            if (applied.conflicts > 0) {
                throw new IllegalStateException(
                        "이 기기의 변경사항과 다른 기기의 변경사항이 겹쳤습니다.");
            }
            long nextCursor = response.optLong("nextCursor", cursor);
            if (nextCursor < cursor) {
                throw new IllegalStateException("서버 동기화 위치가 올바르지 않습니다.");
            }
            cursor = nextCursor;
            store.setCursor(cursor);
            if (!response.optBoolean("hasMore", false)) return;
        }
        throw new IllegalStateException("받아올 변경사항이 많습니다. 다시 시도해주세요.");
    }

    private static String safeMessage(Exception error) {
        String value = error == null || error.getMessage() == null
                ? "데이터를 동기화하지 못했습니다." : error.getMessage().trim();
        if (value.isEmpty()) value = "데이터를 동기화하지 못했습니다.";
        return value.length() > 140 ? value.substring(0, 140) : value;
    }

    private static void broadcast(Context context) {
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }
}
