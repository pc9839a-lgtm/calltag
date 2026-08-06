package kr.pagero.calltag;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Calendar;
import java.util.List;
import java.util.WeakHashMap;

/** 홈의 '오늘 할 일'에는 오늘 00:00~23:59의 미완료 일정만 남긴다. */
public final class HomeTodayTaskFilter {
    private static final WeakHashMap<Activity, View.OnLayoutChangeListener> LISTENERS =
            new WeakHashMap<>();
    private static final WeakHashMap<Activity, Boolean> FILTERING = new WeakHashMap<>();

    private HomeTodayTaskFilter() {}

    public static void install(Activity activity) {
        if (!(activity instanceof MainActivity) || activity.isFinishing()) return;
        LinearLayout list = activity.findViewById(R.id.todayTaskList);
        if (list == null) return;

        if (!LISTENERS.containsKey(activity)) {
            View.OnLayoutChangeListener listener = (v, left, top, right, bottom,
                                                     oldLeft, oldTop, oldRight, oldBottom) ->
                    filter(activity, list);
            LISTENERS.put(activity, listener);
            list.addOnLayoutChangeListener(listener);
        }
        list.post(() -> filter(activity, list));
    }

    public static void uninstall(Activity activity) {
        if (activity == null) return;
        View.OnLayoutChangeListener listener = LISTENERS.remove(activity);
        LinearLayout list = activity.findViewById(R.id.todayTaskList);
        if (listener != null && list != null) list.removeOnLayoutChangeListener(listener);
        FILTERING.remove(activity);
    }

    private static void filter(Activity activity, LinearLayout list) {
        if (activity.isFinishing() || Boolean.TRUE.equals(FILTERING.get(activity))) return;
        FILTERING.put(activity, true);
        CallTagDbHelper db = new CallTagDbHelper(activity);
        try {
            List<FollowUpTask> tasks = db.listPendingTasks();
            int paired = Math.min(tasks.size(), list.getChildCount());
            for (int index = paired - 1; index >= 0; index--) {
                if (!isToday(tasks.get(index).dueAt)) list.removeViewAt(index);
            }

            // 렌더링 도중 데이터가 바뀌어 카드와 작업 수가 어긋나면 안전하게 초과 카드 제거.
            while (list.getChildCount() > tasks.size()) {
                list.removeViewAt(list.getChildCount() - 1);
            }

            View empty = activity.findViewById(R.id.todayEmpty);
            if (empty != null) {
                empty.setVisibility(list.getChildCount() == 0 ? View.VISIBLE : View.GONE);
            }
        } finally {
            db.close();
            FILTERING.remove(activity);
        }
    }

    private static boolean isToday(long millis) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        return target.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }
}
