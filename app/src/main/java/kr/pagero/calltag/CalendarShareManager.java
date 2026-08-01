package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.provider.CalendarContract;
import android.widget.Toast;

/** Google 캘린더, 삼성 캘린더 등 설치된 캘린더 앱의 일정 추가 화면을 연다. */
public final class CalendarShareManager {
    private static final long DEFAULT_DURATION_MS = 60L * 60L * 1000L;

    private CalendarShareManager() {}

    public static boolean open(Activity activity, FollowUpTask task) {
        if (activity == null || task == null) return false;
        String title = safe(task.title).isEmpty() ? "콜태그 할 일" : task.title.trim();
        StringBuilder description = new StringBuilder();
        if (!safe(task.customerName).isEmpty()) {
            description.append("고객: ").append(task.customerName.trim());
        }
        if (!safe(task.phone).isEmpty()) {
            if (description.length() > 0) description.append("\n");
            description.append("연락처: ").append(task.phone.trim());
        }
        description.append("\n\n콜태그에서 공유한 일정입니다.");

        Intent insert = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, description.toString())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, task.dueAt)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, task.dueAt + DEFAULT_DURATION_MS)
                .putExtra(CalendarContract.Events.AVAILABILITY,
                        CalendarContract.Events.AVAILABILITY_BUSY);
        try {
            activity.startActivity(Intent.createChooser(insert, "캘린더 앱 선택"));
            return true;
        } catch (RuntimeException error) {
            Toast.makeText(activity, "일정을 추가할 캘린더 앱을 찾지 못했습니다.",
                    Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
