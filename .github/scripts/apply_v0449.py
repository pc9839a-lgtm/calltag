from pathlib import Path
import re


def read(path):
    return Path(path).read_text(encoding="utf-8")


def write(path, value):
    Path(path).write_text(value, encoding="utf-8")


def sub(path, pattern, replacement, count=1, flags=0):
    value = read(path)
    value, changed = re.subn(pattern, replacement, value, count=count, flags=flags)
    if changed != count:
        raise SystemExit(f"{path}: expected {count} replacement(s), got {changed}: {pattern[:100]}")
    write(path, value)


def replace(path, old, new, count=1):
    value = read(path)
    actual = value.count(old)
    if actual < count:
        raise SystemExit(f"{path}: expected >= {count}, got {actual}: {old[:100]!r}")
    write(path, value.replace(old, new, count))


main = "app/src/main/java/kr/pagero/calltag/MainActivity.java"
home = "app/src/main/java/kr/pagero/calltag/HomeTaskEditorActivity.java"
pending = "app/src/main/java/kr/pagero/calltag/PendingCallSectionView.java"
time = "app/src/main/java/kr/pagero/calltag/TaskTimeChoiceDialog.java"
styler = "app/src/main/java/kr/pagero/calltag/CallTagDialogStyler.java"
spinner = "app/src/main/java/kr/pagero/calltag/CallTagSpinnerAdapter.java"
gradle = "app/build.gradle"
workflow = ".github/workflows/calltag-hotfix-build.yml"

replace(main, "import android.app.DatePickerDialog;\n", "")
replace(home, "import android.app.DatePickerDialog;\n", "")

sub(
    main,
    r'''        new AlertDialog\.Builder\(this\)\s*\n\s*\.setTitle\("통화 감지 권한"\).*?\n\s*\.show\(\);''',
    '''        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)\n                .setTitle("통화 감지 권한")\n                .setMessage("발신·수신·부재중 통화를 할 일과 연결하는 데 필요합니다.")\n                .setNegativeButton("취소", null)\n                .setPositiveButton("권한 허용", (value, which) -> requestPermissions(\n                        missing.toArray(new String[0]), REQUEST_MONITOR_PERMISSIONS))\n                .create();\n        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));\n        dialog.show();''',
    flags=re.S,
)

sub(
    main,
    r'''    private void showRescheduleDatePicker\(FollowUpTask task\) \{.*?\n    \}\n\n    private void completeCalendarTask''',
    '''    private void showRescheduleDatePicker(FollowUpTask task) {\n        Calendar current = Calendar.getInstance();\n        current.setTimeInMillis(task.dueAt);\n        TaskDateChoiceDialog.show(this, current, "이 날짜로 변경", (year, month, dayOfMonth) -> {\n            Calendar changed = Calendar.getInstance();\n            changed.set(year, month, dayOfMonth,\n                    current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), 0);\n            changed.set(Calendar.MILLISECOND, 0);\n            TaskTimeChoiceDialog.show(this,\n                    current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE),\n                    "이 시간으로 변경", (hourOfDay, minute) -> {\n                changed.set(Calendar.HOUR_OF_DAY, hourOfDay);\n                changed.set(Calendar.MINUTE, minute);\n                db.updateTaskDue(task.id, changed.getTimeInMillis());\n                selectedCalendarDate.setTimeInMillis(changed.getTimeInMillis());\n                visibleCalendarMonth.setTimeInMillis(changed.getTimeInMillis());\n                visibleCalendarMonth.set(Calendar.DAY_OF_MONTH, 1);\n                clearTime(visibleCalendarMonth);\n                long now = System.currentTimeMillis();\n                db.insertInteraction(task.customerId, "SCHEDULE_CHANGE", now, now, 0L,\n                        "SCHEDULED", task.title + " 일정 변경");\n                Toast.makeText(this, "일정을 변경했습니다.", Toast.LENGTH_SHORT).show();\n                refreshAll();\n            });\n        });\n    }\n\n    private void completeCalendarTask''',
    flags=re.S,
)

sub(
    main,
    r'''    private void confirmDeleteTask\(FollowUpTask task\) \{.*?\n    \}\n\n    private void renderMoreMenu''',
    '''    private void confirmDeleteTask(FollowUpTask task) {\n        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)\n                .setTitle("할 일 삭제")\n                .setMessage("‘" + task.title + "’ 일정을 삭제합니다.")\n                .setNegativeButton("취소", null)\n                .setPositiveButton("삭제", (value, which) -> {\n                    db.deleteTask(task.id);\n                    Toast.makeText(this, "삭제했습니다.", Toast.LENGTH_SHORT).show();\n                    refreshAll();\n                })\n                .create();\n        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));\n        dialog.show();\n    }\n\n    private void renderMoreMenu''',
    flags=re.S,
)

sub(
    home,
    r'''    private void chooseDate\(Customer customer, TaskTypeOption type\) \{.*?\n    \}\n\n    private void chooseTime''',
    '''    private void chooseDate(Customer customer, TaskTypeOption type) {\n        Calendar selected = Calendar.getInstance();\n        TaskDateChoiceDialog.show(this, selected, "이 날짜로 선택", (year, month, dayOfMonth) -> {\n            selected.set(Calendar.YEAR, year);\n            selected.set(Calendar.MONTH, month);\n            selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);\n            chooseTime(customer, type, selected);\n        });\n    }\n\n    private void chooseTime''',
    flags=re.S,
)

sub(
    pending,
    r'''    private void confirmDelete\(PendingCallRecord call\) \{.*?\n    \}\n\n    private void dial''',
    '''    private void confirmDelete(PendingCallRecord call) {\n        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.Theme_CallTag_Dialog)\n                .setTitle("확인할 통화 삭제")\n                .setMessage("콜태그의 확인할 통화 목록에서만 삭제합니다. 휴대폰 기본 통화기록은 삭제하지 않습니다.")\n                .setNegativeButton("취소", null)\n                .setPositiveButton("삭제", (value, which) -> {\n                    try (PendingCallStore store = new PendingCallStore(getContext())) {\n                        if (store.deletePending(call.callLogId)) {\n                            CrashTelemetryStore.record(getContext(), "pending_call", "deleted",\n                                    String.valueOf(call.callLogId));\n                            refresh();\n                        } else {\n                            Toast.makeText(getContext(), "이미 처리된 통화입니다.", Toast.LENGTH_SHORT).show();\n                            refresh();\n                        }\n                    }\n                })\n                .create();\n        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));\n        dialog.show();\n    }\n\n    private void dial''',
    flags=re.S,
)

replace(
    time,
    "import android.widget.LinearLayout;\nimport android.widget.TextView;\n",
    "import android.widget.LinearLayout;\nimport android.widget.ScrollView;\nimport android.widget.TextView;\n",
)
replace(
    time,
    "        root.addView(summary, fixedTop(context, 58, 14));",
    "        summary.setMinHeight(dp(context, 58));\n        summary.setPadding(dp(context, 8), dp(context, 10), dp(context, 8), dp(context, 10));\n        root.addView(summary, top(context, 14));",
)
replace(time, "        root.addView(periodRow, fixedTop(context, 46, 7));", "        root.addView(periodRow, top(context, 7));")
replace(
    time,
    "            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(\n                    LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 42));",
    "            LinearLayout.LayoutParams lineParams = matchWrap();",
    2,
)
replace(time, "        root.addView(actions, fixedTop(context, 50, 18));", "        root.addView(actions, top(context, 18));")
replace(
    time,
    "        AlertDialog dialog = new AlertDialog.Builder(context)\n                .setView(root)\n                .create();",
    "        ScrollView scroll = new ScrollView(context);\n        scroll.setFillViewport(true);\n        scroll.addView(root, matchWrap());\n\n        AlertDialog dialog = new AlertDialog.Builder(context, R.style.Theme_CallTag_Dialog)\n                .setView(scroll)\n                .create();",
)
replace(
    time,
    "        view.setBackgroundResource(R.drawable.bg_secondary_button);\n        view.setClickable(true);",
    "        view.setBackgroundResource(R.drawable.bg_secondary_button);\n        view.setMinHeight(dp(context, 44));\n        view.setPadding(dp(context, 6), dp(context, 8), dp(context, 6), dp(context, 8));\n        view.setClickable(true);",
    1,
)
replace(
    time,
    "        view.setBackgroundResource(primary\n                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);\n        view.setClickable(true);",
    "        view.setBackgroundResource(primary\n                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);\n        view.setMinHeight(dp(context, 46));\n        view.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));\n        view.setClickable(true);",
)
sub(
    time,
    r'''    private static LinearLayout\.LayoutParams fixedTop\(Context context, int height, int margin\) \{.*?\n    \}\n\n''',
    "",
    flags=re.S,
)
replace(
    time,
    "        return new LinearLayout.LayoutParams(0, dp(context, 46), weight);",
    "        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);",
)

replace(
    styler,
    "            int width = Math.min(displayWidth - dp(dialog, SIDE_MARGIN_DP), dp(dialog, MAX_WIDTH_DP));\n            params.width = Math.max(dp(dialog, 280), width);\n            params.dimAmount = 0.72f;",
    "            int availableWidth = Math.max(1, displayWidth - dp(dialog, SIDE_MARGIN_DP * 2));\n            params.width = Math.min(availableWidth, dp(dialog, MAX_WIDTH_DP));\n            params.dimAmount = 0.72f;",
)
replace(
    styler,
    "            window.setAttributes(params);\n            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);\n            window.setLayout(params.width, WindowManager.LayoutParams.WRAP_CONTENT);",
    "            window.setAttributes(params);\n            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);\n            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);\n            window.setLayout(params.width, WindowManager.LayoutParams.WRAP_CONTENT);",
)
replace(
    styler,
    "        button.setTextSize(14f);\n        button.setBackgroundResource(primary",
    "        button.setTextSize(14f);\n        button.setMinHeight(dp(button, 44));\n        button.setPadding(dp(button, 12), dp(button, 6), dp(button, 12), dp(button, 6));\n        button.setBackgroundResource(primary",
)
replace(
    styler,
    "    private static int dp(AlertDialog dialog, int value) {\n        return Math.round(value * dialog.getContext().getResources().getDisplayMetrics().density);\n    }",
    "    private static int dp(AlertDialog dialog, int value) {\n        return Math.round(value * dialog.getContext().getResources().getDisplayMetrics().density);\n    }\n\n    private static int dp(View view, int value) {\n        return Math.round(value * view.getResources().getDisplayMetrics().density);\n    }",
)

replace(
    spinner,
    "        super(context, android.R.layout.simple_spinner_item, values);\n        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);",
    "        super(context, 0, values);",
)

replace(gradle, "versionCode 86\n        versionName '0.44.8'", "versionCode 87\n        versionName '0.44.9'")

value = read(workflow)
value = value.replace("CallTag 0.44.8 calendar time picker regression", "CallTag 0.44.9 dialog and small-screen regression")
value = value.replace("Verify 0.44.8 contracts", "Verify 0.44.9 contracts")
value = value.replace("grep -F 'versionCode 86' app/build.gradle", "grep -F 'versionCode 87' app/build.gradle")
value = value.replace("grep -F \"versionName '0.44.8'\" app/build.gradle", "grep -F \"versionName '0.44.9'\" app/build.gradle")
value = value.replace("calltag-v0.44.8-code86-debug-apk", "calltag-v0.44.9-code87-debug-apk")
value = value.replace("calltag-v0.44.8-code86-instrumentation-results", "calltag-v0.44.9-code87-instrumentation-results")

anchor = "          grep -F 'public static void show(Context context, int initialHourOfDay, int initialMinute' app/src/main/java/kr/pagero/calltag/TaskTimeChoiceDialog.java\n"
extra = anchor + '''
          # Date/time dialogs are CallTag-native, scroll-safe and never OEM pickers.
          test -e app/src/main/java/kr/pagero/calltag/TaskDateChoiceDialog.java
          grep -F 'TaskDateChoiceDialog.show(this, selected' app/src/main/java/kr/pagero/calltag/HomeTaskEditorActivity.java
          grep -F 'TaskDateChoiceDialog.show(this, current' app/src/main/java/kr/pagero/calltag/MainActivity.java
          assert_absent 'DatePickerDialog' app/src/main/java
          grep -F 'ScrollView scroll = new ScrollView(context);' app/src/main/java/kr/pagero/calltag/TaskTimeChoiceDialog.java
          grep -F 'ScrollView scroll = new ScrollView(context);' app/src/main/java/kr/pagero/calltag/TaskDateChoiceDialog.java

          # Framework dialogs stay dark and fit narrow screens / keyboard resize.
          grep -F 'SIDE_MARGIN_DP * 2' app/src/main/java/kr/pagero/calltag/CallTagDialogStyler.java
          grep -F 'SOFT_INPUT_ADJUST_RESIZE' app/src/main/java/kr/pagero/calltag/CallTagDialogStyler.java
          grep -F 'new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)' app/src/main/java/kr/pagero/calltag/MainActivity.java
          grep -F 'new AlertDialog.Builder(getContext(), R.style.Theme_CallTag_Dialog)' app/src/main/java/kr/pagero/calltag/PendingCallSectionView.java

          # Spinner rows are CallTag-native with no framework simple_spinner resources.
          grep -F 'super(context, 0, values);' app/src/main/java/kr/pagero/calltag/CallTagSpinnerAdapter.java
          assert_absent 'android.R.layout.simple_spinner_' app/src/main/java
'''
if anchor not in value:
    raise SystemExit("workflow contract anchor missing")
value = value.replace(anchor, extra, 1)

verify_anchor = "      - name: Verify 0.44.9 contracts\n"
audit = '''      - name: Inventory framework UI calls
        shell: bash
        run: |
          echo '=== AlertDialog builders ==='
          grep -R -n -F 'new AlertDialog.Builder' app/src/main/java || true
          echo '=== Picker references ==='
          grep -R -n -E 'DatePickerDialog|TimePickerDialog' app/src/main/java || true
          echo '=== Spinner references ==='
          grep -R -n -E 'simple_spinner_|new Spinner|Spinner<' app/src/main/java || true

      - name: Verify 0.44.9 contracts
'''
if verify_anchor not in value:
    raise SystemExit("workflow verify anchor missing")
value = value.replace(verify_anchor, audit, 1)
write(workflow, value)
