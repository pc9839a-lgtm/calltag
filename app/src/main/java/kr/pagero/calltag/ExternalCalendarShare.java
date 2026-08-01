package kr.pagero.calltag;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.CalendarContract;
import android.widget.Toast;

/** Google 캘린더·삼성 캘린더 등 Android 일정 앱의 등록 화면을 연다. */
public final class ExternalCalendarShare {
    private ExternalCalendarShare() {}

    public static boolean open(Activity activity, FollowUpTask task,
                               Customer customer, String memo) {
        if (activity == null || task == null) return false;

        long duration = TaskTypeStore.TYPE_MEETING.equals(task.taskType)
                ? 60L * 60L * 1000L
                : 30L * 60L * 1000L;
        String name = customer == null || customer.displayName.trim().isEmpty()
                ? task.customerName : customer.displayName;
        String phone = customer == null || customer.primaryPhone.trim().isEmpty()
                ? task.phone : customer.primaryPhone;

        StringBuilder description = new StringBuilder();
        description.append("콜태그 고객 · ").append(name);
        if (phone != null && !phone.trim().isEmpty()) {
            description.append("\n연락처 · ").append(phone.trim());
        }
        description.append("\n일정 · ").append(task.title);
        if (memo != null && !memo.trim().isEmpty()) {
            description.append("\n메모 · ").append(memo.trim());
        }

        Intent insert = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, task.dueAt)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, task.dueAt + duration)
                .putExtra(CalendarContract.Events.TITLE, name + " · " + task.title)
                .putExtra(CalendarContract.Events.DESCRIPTION, description.toString())
                .putExtra(CalendarContract.Events.AVAILABILITY,
                        CalendarContract.Events.AVAILABILITY_BUSY);
        try {
            activity.startActivity(Intent.createChooser(insert, "캘린더 앱 선택"));
            return true;
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(activity,
                    "일정을 저장할 캘린더 앱을 찾지 못했습니다.", Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
