package kr.pagero.calltag;

import android.app.Activity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** In-place follow-up task creation used by the home pending-call cards. */
public final class QuickTaskCreateDialog {
    private QuickTaskCreateDialog() {}

    public static void show(Activity activity, Customer customer, Runnable onSaved) {
        if (activity == null || activity.isFinishing() || customer == null) return;

        List<TaskTypeOption> availableTypes;
        TaskTypeStore store = new TaskTypeStore(activity);
        try {
            availableTypes = new ArrayList<>(store.list());
        } catch (RuntimeException error) {
            Toast.makeText(activity, "일정 종류를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        } finally {
            store.close();
        }

        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        for (TaskTypeOption type : availableTypes) {
            options.add(new ActionChoiceDialog.Option(
                    type.code,
                    type.name,
                    "이 종류로 할 일 등록",
                    type.color));
        }

        ActionChoiceDialog.show(activity,
                "할 일 등록",
                customer.displayName,
                options,
                option -> {
                    TaskTypeOption selected = findType(availableTypes, option.key);
                    if (selected == null) {
                        Toast.makeText(activity, "일정 종류를 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Calendar initial = Calendar.getInstance();
                    TaskDateChoiceDialog.show(activity, initial, "이 날짜로 등록",
                            (year, month, dayOfMonth) -> {
                                Calendar due = Calendar.getInstance();
                                due.set(year, month, dayOfMonth,
                                        initial.get(Calendar.HOUR_OF_DAY),
                                        initial.get(Calendar.MINUTE), 0);
                                due.set(Calendar.MILLISECOND, 0);
                                TaskTimeChoiceDialog.show(activity,
                                        initial.get(Calendar.HOUR_OF_DAY),
                                        initial.get(Calendar.MINUTE),
                                        "이 시간으로 등록",
                                        (hourOfDay, minute) -> save(
                                                activity, customer, selected, due,
                                                hourOfDay, minute, onSaved));
                            });
                });
    }

    private static void save(Activity activity, Customer customer, TaskTypeOption selected,
                             Calendar due, int hourOfDay, int minute, Runnable onSaved) {
        due.set(Calendar.HOUR_OF_DAY, hourOfDay);
        due.set(Calendar.MINUTE, minute);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);
        CallTagDbHelper db = new CallTagDbHelper(activity);
        try {
            db.insertFollowUpTask(
                    customer.id,
                    0L,
                    selected.code,
                    selected.name,
                    due.getTimeInMillis());
            long now = System.currentTimeMillis();
            db.insertInteraction(
                    customer.id,
                    "SCHEDULE_CREATE",
                    now,
                    now,
                    0L,
                    "SCHEDULED",
                    selected.name);
            HomeTaskRefreshStore.mark(activity);
            Toast.makeText(activity, "할 일을 등록했습니다.", Toast.LENGTH_SHORT).show();
            if (onSaved != null) onSaved.run();
        } catch (RuntimeException error) {
            Toast.makeText(activity, "할 일을 저장하지 못했습니다.", Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    private static TaskTypeOption findType(List<TaskTypeOption> types, String code) {
        for (TaskTypeOption type : types) {
            if (type != null && type.code.equals(code)) return type;
        }
        return null;
    }
}
