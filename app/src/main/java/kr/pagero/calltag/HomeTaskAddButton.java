package kr.pagero.calltag;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Safe home task registration that never finishes or recreates MainActivity. */
public final class HomeTaskAddButton extends Button {
    public HomeTaskAddButton(Context context) {
        super(context);
        initialize();
    }

    public HomeTaskAddButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public HomeTaskAddButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setOnClickListener(v -> startFlow());
    }

    private void startFlow() {
        Activity activity = activity();
        if (activity == null || activity.isFinishing()) return;

        CallTagDbHelper db = new CallTagDbHelper(activity);
        List<Customer> customers;
        try {
            customers = db.listCustomers(null);
        } finally {
            db.close();
        }
        if (customers.isEmpty()) {
            Toast.makeText(activity, "먼저 고객을 추가해주세요.", Toast.LENGTH_SHORT).show();
            View customerNav = activity.findViewById(R.id.navCustomers);
            if (customerNav != null) customerNav.performClick();
            return;
        }

        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        CallTagDbHelper colorDb = new CallTagDbHelper(activity);
        try {
            for (Customer customer : customers) {
                options.add(new ActionChoiceDialog.Option(
                        String.valueOf(customer.id),
                        customer.displayName,
                        customer.primaryPhone + " · " + customer.relationStatus,
                        colorDb.stageColor(customer.relationStatus)));
            }
        } finally {
            colorDb.close();
        }

        ActionChoiceDialog.show(activity, "할 일 고객 선택", null, options, option -> {
            try {
                long customerId = Long.parseLong(option.key);
                chooseTaskType(activity, customerId);
            } catch (NumberFormatException error) {
                Toast.makeText(activity, "고객 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void chooseTaskType(Activity activity, long customerId) {
        TaskTypeStore store = new TaskTypeStore(activity);
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        try {
            for (TaskTypeOption type : store.list()) {
                options.add(new ActionChoiceDialog.Option(
                        type.code, type.name, "이 종류로 등록", type.color));
            }
        } finally {
            store.close();
        }

        ActionChoiceDialog.show(activity, "할 일 종류", null, options, option -> {
            TaskTypeStore lookup = new TaskTypeStore(activity);
            TaskTypeOption type;
            try {
                type = lookup.find(option.key);
            } finally {
                lookup.close();
            }
            chooseDate(activity, customerId, type);
        });
    }

    private void chooseDate(Activity activity, long customerId, TaskTypeOption type) {
        Calendar selected = Calendar.getInstance();
        new DatePickerDialog(activity, (view, year, month, dayOfMonth) -> {
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            chooseTime(activity, customerId, type, selected);
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void chooseTime(Activity activity, long customerId,
                            TaskTypeOption type, Calendar selected) {
        new TimePickerDialog(activity, (view, hourOfDay, minute) -> {
            selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selected.set(Calendar.MINUTE, minute);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            save(activity, customerId, type, selected.getTimeInMillis());
        }, 10, 0, false).show();
    }

    private void save(Activity activity, long customerId,
                      TaskTypeOption type, long dueAt) {
        setEnabled(false);
        CallTagDbHelper db = new CallTagDbHelper(activity);
        try {
            Customer customer = db.findCustomerById(customerId);
            if (customer == null) {
                Toast.makeText(activity, "고객을 찾지 못했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            db.insertFollowUpTask(customerId, 0L, type.code, type.name, dueAt);
            long now = System.currentTimeMillis();
            db.insertInteraction(customerId, "SCHEDULE_CREATE", now, now, 0L,
                    "SCHEDULED", type.name);
            Toast.makeText(activity, "할 일을 등록했습니다.", Toast.LENGTH_SHORT).show();
            View homeNav = activity.findViewById(R.id.navToday);
            if (homeNav != null) homeNav.performClick();
        } catch (RuntimeException error) {
            Toast.makeText(activity, "할 일을 저장하지 못했습니다.", Toast.LENGTH_LONG).show();
        } finally {
            db.close();
            setEnabled(true);
        }
    }

    private Activity activity() {
        Context current = getContext();
        return current instanceof Activity ? (Activity) current : null;
    }
}
