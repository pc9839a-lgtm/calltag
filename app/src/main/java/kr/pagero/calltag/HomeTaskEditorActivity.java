package kr.pagero.calltag;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Runs home task creation outside MainActivity so dialog callbacks never trigger a
 * re-entrant home refresh while the activity is rendering task/customer cards.
 */
public final class HomeTaskEditorActivity extends Activity {
    private CallTagDbHelper db;
    private TaskTypeStore taskTypes;
    private LinearLayout customerList;
    private TextView emptyView;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallTagDbHelper(this);
        taskTypes = new TaskTypeStore(this);
        setContentView(buildScreen());
        renderCustomers();
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.background));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), 0, dp(12), 0);

        TextView back = text("‹", 32f, R.color.text_primary, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> {
            if (!saving) finish();
        });
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(56)));

        TextView title = text("할 일 등록", 19f, R.color.text_primary, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));
        top.addView(new View(this), new LinearLayout.LayoutParams(dp(48), dp(56)));
        root.addView(top, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.border));
        root.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(18), dp(16), dp(30));

        body.addView(text("고객을 선택하세요", 16f, R.color.text_primary, true), matchWrap());
        TextView helper = text("선택 후 할 일 종류와 날짜·시간을 지정합니다.",
                13f, R.color.text_secondary, false);
        body.addView(helper, topMargin(5));

        customerList = new LinearLayout(this);
        customerList.setOrientation(LinearLayout.VERTICAL);
        body.addView(customerList, topMargin(14));

        emptyView = text("등록된 고객이 없습니다. 먼저 고객을 추가해주세요.",
                13f, R.color.text_secondary, false);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setBackgroundResource(R.drawable.bg_card);
        body.addView(emptyView, fixedHeight(76, 8));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(body, matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void renderCustomers() {
        customerList.removeAllViews();
        List<Customer> customers;
        try {
            customers = db.listCustomers(null);
        } catch (RuntimeException error) {
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("고객 목록을 불러오지 못했습니다.");
            return;
        }

        emptyView.setVisibility(customers.isEmpty() ? View.VISIBLE : View.GONE);
        for (Customer customer : customers) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(15), dp(12), dp(10), dp(12));
            row.setBackgroundResource(R.drawable.bg_clickable_row);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                if (!saving) chooseTaskType(customer);
            });

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            labels.addView(text(customer.displayName, 15f, R.color.text_primary, true), matchWrap());
            labels.addView(text(customer.primaryPhone + " · " + customer.relationStatus,
                    12.5f, R.color.text_secondary, false), topMargin(4));
            row.addView(labels, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView arrow = text("›", 23f, R.color.text_muted, false);
            arrow.setGravity(Gravity.CENTER);
            row.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(44)));

            LinearLayout.LayoutParams params = matchWrap();
            params.bottomMargin = dp(7);
            customerList.addView(row, params);
        }
    }

    private void chooseTaskType(Customer customer) {
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        try {
            for (TaskTypeOption type : taskTypes.list()) {
                options.add(new ActionChoiceDialog.Option(
                        type.code, type.name, "이 종류로 등록", type.color));
            }
        } catch (RuntimeException error) {
            Toast.makeText(this, "할 일 종류를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        ActionChoiceDialog.show(this, "할 일 종류", customer.displayName,
                options, option -> {
                    TaskTypeOption selected;
                    try {
                        selected = taskTypes.find(option.key);
                    } catch (RuntimeException error) {
                        Toast.makeText(this, "할 일 종류를 확인하지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    chooseDate(customer, selected);
                });
    }

    private void chooseDate(Customer customer, TaskTypeOption type) {
        Calendar selected = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            chooseTime(customer, type, selected);
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void chooseTime(Customer customer, TaskTypeOption type, Calendar selected) {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selected.set(Calendar.MINUTE, minute);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            save(customer, type, selected.getTimeInMillis());
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false).show();
    }

    private void save(Customer customer, TaskTypeOption type, long dueAt) {
        if (saving) return;
        saving = true;
        try {
            Customer latest = db.findCustomerById(customer.id);
            if (latest == null) {
                saving = false;
                Toast.makeText(this, "고객 정보를 다시 확인해주세요.", Toast.LENGTH_LONG).show();
                return;
            }
            db.insertFollowUpTask(latest.id, 0L, type.code, type.name, dueAt);
            long now = System.currentTimeMillis();
            db.insertInteraction(latest.id, "SCHEDULE_CREATE", now, now, 0L,
                    "SCHEDULED", type.name);
            HomeTaskRefreshStore.mark(this);
            Toast.makeText(this,
                    isToday(dueAt) ? "오늘 할 일에 등록했습니다." : "일정에 등록했습니다.",
                    Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (RuntimeException error) {
            saving = false;
            Toast.makeText(this, "할 일을 저장하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isToday(long value) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(value);
        Calendar today = Calendar.getInstance();
        return target.get(Calendar.ERA) == today.get(Calendar.ERA)
                && target.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value == null ? "" : value);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(value);
        return params;
    }

    private LinearLayout.LayoutParams fixedHeight(int height, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(topMargin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (taskTypes != null) taskTypes.close();
        if (db != null) db.close();
        super.onDestroy();
    }
}
