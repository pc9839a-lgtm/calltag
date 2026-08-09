from pathlib import Path
import re


def read(path):
    return Path(path).read_text(encoding='utf-8')

def write(path, value):
    Path(path).write_text(value, encoding='utf-8')

def replace(path, old, new, count=1):
    value = read(path)
    actual = value.count(old)
    if actual < count:
        raise SystemExit(f'{path}: expected >= {count}, got {actual}: {old[:100]!r}')
    write(path, value.replace(old, new, count))

def sub(path, pattern, replacement, count=1, flags=0):
    value = read(path)
    value, actual = re.subn(pattern, replacement, value, count=count, flags=flags)
    if actual != count:
        raise SystemExit(f'{path}: expected {count}, got {actual}: {pattern[:100]}')
    write(path, value)

stats='app/src/main/java/kr/pagero/calltag/CustomerStatsView.java'
detail='app/src/main/java/kr/pagero/calltag/CustomerDetailActivity.java'
auto='app/src/main/java/kr/pagero/calltag/MessageAutomationSettingsActivity.java'
date='app/src/main/java/kr/pagero/calltag/TaskDateChoiceDialog.java'

# TaskDateChoiceDialog: add optional maximum date and remove forbidden OEM class name from comment.
replace(date, '/** CallTag 전용 날짜 선택 UI. OEM DatePickerDialog를 사용하지 않는다. */',
              '/** CallTag 전용 날짜 선택 UI. OEM 날짜 선택기를 사용하지 않는다. */')
replace(date, '''    public static void show(Context context, Calendar initial, String actionLabel, Listener listener) {
        Calendar safeInitial = initial == null ? Calendar.getInstance() : (Calendar) initial.clone();
        clearTime(safeInitial);

        State state = new State();''', '''    public static void show(Context context, Calendar initial, String actionLabel, Listener listener) {
        show(context, initial, null, actionLabel, listener);
    }

    public static void show(Context context, Calendar initial, Calendar maximumDate,
                            String actionLabel, Listener listener) {
        Calendar safeInitial = initial == null ? Calendar.getInstance() : (Calendar) initial.clone();
        clearTime(safeInitial);
        Calendar safeMaximum = maximumDate == null ? null : (Calendar) maximumDate.clone();
        if (safeMaximum != null) clearTime(safeMaximum);
        if (safeMaximum != null && safeInitial.after(safeMaximum)) {
            safeInitial.setTimeInMillis(safeMaximum.getTimeInMillis());
        }

        State state = new State();''')
replace(date, '''                    boolean selected = sameDay(date, state.selected);
                    boolean isToday = sameDay(date, today);

                    TextView day = choice(context, String.valueOf(dayNumber));
                    day.setTextColor(context.getColor(selected
                            ? android.R.color.white
                            : column == 0 ? R.color.danger
                            : column == 6 ? R.color.primary : R.color.text_primary));
                    day.setBackgroundResource(selected
                            ? R.drawable.bg_primary_button
                            : isToday ? R.drawable.bg_selected_row : R.drawable.bg_secondary_button);
                    day.setOnClickListener(v -> {
                        state.selected.setTimeInMillis(date.getTimeInMillis());
                        render[0].run();
                    });''', '''                    boolean selected = sameDay(date, state.selected);
                    boolean isToday = sameDay(date, today);
                    boolean disabled = safeMaximum != null && date.after(safeMaximum);

                    TextView day = choice(context, String.valueOf(dayNumber));
                    day.setTextColor(context.getColor(disabled
                            ? R.color.text_muted
                            : selected ? android.R.color.white
                            : column == 0 ? R.color.danger
                            : column == 6 ? R.color.primary : R.color.text_primary));
                    day.setAlpha(disabled ? 0.45f : 1f);
                    day.setBackgroundResource(selected
                            ? R.drawable.bg_primary_button
                            : isToday ? R.drawable.bg_selected_row : R.drawable.bg_secondary_button);
                    if (!disabled) {
                        day.setOnClickListener(v -> {
                            state.selected.setTimeInMillis(date.getTimeInMillis());
                            render[0].run();
                        });
                    }''')

# CustomerStatsView: replace system date picker with CallTag picker and style range dialogs.
replace(stats, 'import android.app.DatePickerDialog;\n', '')
sub(stats, r'''    private void openDatePicker\(Activity activity, Calendar initial, DateSelected listener\) \{.*?\n    \}\n\n    private void showRangeWarning''', '''    private void openDatePicker(Activity activity, Calendar initial, DateSelected listener) {
        Calendar maximum = Calendar.getInstance();
        setEndOfDay(maximum);
        TaskDateChoiceDialog.show(activity, initial, maximum, "이 날짜로 선택",
                (year, month, day) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    listener.onSelected(selected.getTimeInMillis());
                });
    }

    private void showRangeWarning''', flags=re.S)
replace(stats, '        AlertDialog dialog = new AlertDialog.Builder(activity)\n',
               '        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.Theme_CallTag_Dialog)\n')
replace(stats, '''        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {''', '''        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {''')
replace(stats, '''                    renderDashboard();
                }));
        dialog.show();''', '''                    renderDashboard();
                });
        });
        dialog.show();''', 1)
replace(stats, '''        new AlertDialog.Builder(getContext())
                .setTitle("조회 기간 확인")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .show();''', '''        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.Theme_CallTag_Dialog)
                .setTitle("조회 기간 확인")
                .setMessage(message)
                .setPositiveButton("확인", null)
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();''')

# CustomerDetailActivity: all task date/time flows use CallTag pickers.
replace(detail, 'import android.app.DatePickerDialog;\n', '')
replace(detail, 'import android.app.TimePickerDialog;\n', '')
sub(detail, r'''    private void showNewScheduleDatePicker\(String title, String taskType\) \{.*?\n    \}\n\n    private void showNewTaskMessageDialog''', '''    private void showNewScheduleDatePicker(String title, String taskType) {
        Calendar defaultDate = Calendar.getInstance();
        defaultDate.add(Calendar.DAY_OF_MONTH, 1);
        TaskDateChoiceDialog.show(this, defaultDate, "이 날짜로 선택", (year, month, dayOfMonth) -> {
            Calendar due = Calendar.getInstance();
            due.set(year, month, dayOfMonth, 10, 0, 0);
            due.set(Calendar.MILLISECOND, 0);
            TaskTimeChoiceDialog.show(this, 10, 0, "이 시간으로 등록", (hourOfDay, minute) -> {
                due.set(Calendar.HOUR_OF_DAY, hourOfDay);
                due.set(Calendar.MINUTE, minute);
                showNewTaskMessageDialog(title, taskType, due.getTimeInMillis());
            });
        });
    }

    private void showNewTaskMessageDialog''', flags=re.S)
sub(detail, r'''    private void showRescheduleDatePicker\(FollowUpTask task\) \{.*?\n    \}\n\n    private void applyReschedule''', '''    private void showRescheduleDatePicker(FollowUpTask task) {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(task.dueAt);
        TaskDateChoiceDialog.show(this, current, "이 날짜로 변경", (year, month, dayOfMonth) -> {
            Calendar changed = Calendar.getInstance();
            changed.set(year, month, dayOfMonth,
                    current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), 0);
            changed.set(Calendar.MILLISECOND, 0);
            TaskTimeChoiceDialog.show(this,
                    current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE),
                    "이 시간으로 변경", (hourOfDay, minute) -> {
                changed.set(Calendar.HOUR_OF_DAY, hourOfDay);
                changed.set(Calendar.MINUTE, minute);
                long changedAt = changed.getTimeInMillis();
                if (TaskMessageLifecycleManager.hasAdjustableLink(this, task.id)) {
                    AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                            .setTitle("연결된 후속문자")
                            .setMessage("일정 시간이 변경됐습니다. 연결된 후속문자도 새 일정에 맞춰 변경할까요?")
                            .setNegativeButton("문자 유지", (value, which) ->
                                    applyReschedule(task, changedAt, false))
                            .setPositiveButton("함께 변경", (value, which) ->
                                    applyReschedule(task, changedAt, true))
                            .create();
                    dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
                    dialog.show();
                } else {
                    applyReschedule(task, changedAt, false);
                }
            });
        });
    }

    private void applyReschedule''', flags=re.S)
sub(detail, r'''    private void confirmDeleteTask\(FollowUpTask task\) \{.*?\n    \}\n\n    private FollowUpTask copyTask''', '''    private void confirmDeleteTask(FollowUpTask task) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("할 일 삭제")
                .setMessage("‘" + task.title + "’ 일정을 삭제합니다. 연결된 미발송 후속문자도 취소됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (value, which) -> {
                    TaskMessageLifecycleManager.onTaskDeleted(this, task);
                    db.deleteTask(task.id);
                    Toast.makeText(this, "일정과 연결된 예약문자를 삭제했습니다.", Toast.LENGTH_SHORT).show();
                    loadCustomer();
                })
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private FollowUpTask copyTask''', flags=re.S)

# MessageAutomationSettingsActivity: no framework spinner rows; style common settings dialog.
replace(auto, 'import android.widget.ArrayAdapter;\n', '')
replace(auto, '''        line.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));''',
              '        line.setAdapter(new CallTagSpinnerAdapter(this, labels));')
replace(auto, '        AlertDialog dialog = new AlertDialog.Builder(this)\n                .setTitle("공통 발송 설정")',
              '        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)\n                .setTitle("공통 발송 설정")')
replace(auto, '''        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {''', '''        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {''')
# This is the first custom settings dialog ending after renderValues().
replace(auto, '''                    renderValues();
                    dialog.dismiss();
                }));''', '''                    renderValues();
                    dialog.dismiss();
                });
        });''', 1)
