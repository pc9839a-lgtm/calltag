package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** 콜태그의 모든 예정 일정을 선택한 Google·삼성 캘린더에 계속 반영한다. */
public final class CalendarSharePickerActivity extends Activity {
    private static final int REQUEST_CALENDAR = 8701;

    private Switch enabled;
    private TextView calendarValue;
    private TextView status;
    private Button syncButton;
    private List<ExternalCalendarSyncManager.CalendarInfo> calendars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        render();
        if (!ExternalCalendarSyncManager.hasPermissions(this)) requestCalendarPermissions();
        else loadCalendars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ExternalCalendarSyncManager.hasPermissions(this)) loadCalendars();
        render();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView screenTitle = title("외부 캘린더 연동", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(6);
        header.addView(screenTitle, titleParams);
        root.addView(header, matchWrap());

        root.addView(body(
                "콜태그에 등록된 모든 예정 일정을 선택한 Google 캘린더·삼성 캘린더에 한 번에 반영합니다. "
                        + "일정 변경·완료·삭제도 다음 연동 때 같이 반영됩니다."), top(14));

        LinearLayout enableCard = card();
        LinearLayout enableRow = new LinearLayout(this);
        enableRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout enableText = new LinearLayout(this);
        enableText.setOrientation(LinearLayout.VERTICAL);
        enableText.addView(title("전체 일정 자동 연동", 16f), matchWrap());
        enableText.addView(body("앱을 사용할 때 변경된 일정을 자동으로 반영"), top(4));
        enableRow.addView(enableText, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        enabled = new Switch(this);
        enabled.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !ExternalCalendarSyncManager.hasPermissions(this)) {
                button.setChecked(false);
                requestCalendarPermissions();
                return;
            }
            if (checked && ExternalCalendarSyncStore.calendarId(this) < 0L) {
                button.setChecked(false);
                chooseCalendar();
                return;
            }
            ExternalCalendarSyncStore.setEnabled(this, checked);
            if (checked) syncAll(false);
            render();
        });
        enableRow.addView(enabled);
        enableCard.addView(enableRow, matchWrap());
        root.addView(enableCard, top(18));

        LinearLayout calendarCard = card();
        calendarCard.setClickable(true);
        calendarCard.setFocusable(true);
        calendarCard.setOnClickListener(v -> chooseCalendar());
        calendarCard.addView(body("연동할 캘린더"), matchWrap());
        calendarValue = title("선택 필요", 17f);
        calendarCard.addView(calendarValue, top(8));
        TextView chooseHint = body("눌러서 Google·삼성 계정 중 선택");
        chooseHint.setTextColor(getColor(R.color.primary));
        calendarCard.addView(chooseHint, top(7));
        root.addView(calendarCard, top(10));

        status = body("");
        status.setPadding(dp(16), dp(16), dp(16), dp(16));
        status.setBackgroundResource(R.drawable.bg_card);
        root.addView(status, top(10));

        syncButton = button("전체 일정 지금 연동", true);
        syncButton.setOnClickListener(v -> syncAll(true));
        root.addView(syncButton, fixedTop(52, 14));

        Button permissions = button("캘린더 권한 확인", false);
        permissions.setOnClickListener(v -> requestCalendarPermissions());
        root.addView(permissions, fixedTop(50, 8));
        return scroll;
    }

    private void loadCalendars() {
        calendars = ExternalCalendarSyncManager.writableCalendars(this);
        if (ExternalCalendarSyncStore.calendarId(this) < 0L
                && calendars != null && calendars.size() == 1) {
            ExternalCalendarSyncManager.CalendarInfo only = calendars.get(0);
            ExternalCalendarSyncStore.setCalendar(this, only.id, only.label());
        }
        render();
    }

    private void chooseCalendar() {
        if (!ExternalCalendarSyncManager.hasPermissions(this)) {
            requestCalendarPermissions();
            return;
        }
        calendars = ExternalCalendarSyncManager.writableCalendars(this);
        if (calendars.isEmpty()) {
            Toast.makeText(this, "쓰기 가능한 외부 캘린더가 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[calendars.size()];
        int selected = -1;
        long currentId = ExternalCalendarSyncStore.calendarId(this);
        for (int i = 0; i < calendars.size(); i++) {
            labels[i] = calendars.get(i).label();
            if (calendars.get(i).id == currentId) selected = i;
        }
        new AlertDialog.Builder(this)
                .setTitle("전체 일정을 연동할 캘린더")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    ExternalCalendarSyncManager.CalendarInfo picked = calendars.get(which);
                    ExternalCalendarSyncStore.setCalendar(this, picked.id, picked.label());
                    dialog.dismiss();
                    render();
                    if (ExternalCalendarSyncStore.isEnabled(this)) syncAll(false);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void syncAll(boolean notify) {
        if (!ExternalCalendarSyncManager.hasPermissions(this)) {
            requestCalendarPermissions();
            return;
        }
        if (ExternalCalendarSyncStore.calendarId(this) < 0L) {
            chooseCalendar();
            return;
        }
        syncButton.setEnabled(false);
        syncButton.setText("전체 일정 연동 중…");
        ExternalCalendarSyncManager.requestSync(this, true, result -> runOnUiThread(() -> {
            render();
            if (notify) {
                Toast.makeText(this,
                        result.success
                                ? "예정 일정 " + result.synced + "건을 외부 캘린더에 반영했습니다."
                                : result.error,
                        result.success ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            }
        }));
    }

    private void render() {
        if (enabled == null) return;
        enabled.setOnCheckedChangeListener(null);
        enabled.setChecked(ExternalCalendarSyncStore.isEnabled(this));
        enabled.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !ExternalCalendarSyncManager.hasPermissions(this)) {
                button.setChecked(false);
                requestCalendarPermissions();
                return;
            }
            if (checked && ExternalCalendarSyncStore.calendarId(this) < 0L) {
                button.setChecked(false);
                chooseCalendar();
                return;
            }
            ExternalCalendarSyncStore.setEnabled(this, checked);
            if (checked) syncAll(false);
            render();
        });

        String name = ExternalCalendarSyncStore.calendarName(this);
        calendarValue.setText(name.isEmpty() ? "선택 필요" : name);
        boolean running = ExternalCalendarSyncManager.isRunning();
        syncButton.setEnabled(!running);
        syncButton.setText(running ? "전체 일정 연동 중…" : "전체 일정 지금 연동");

        if (!ExternalCalendarSyncManager.hasPermissions(this)) {
            status.setText("캘린더 읽기·쓰기 권한을 허용해주세요.");
            status.setTextColor(getColor(R.color.danger));
            return;
        }
        if (ExternalCalendarSyncStore.calendarId(this) < 0L) {
            status.setText("연동할 외부 캘린더를 먼저 선택해주세요.");
            status.setTextColor(getColor(R.color.text_secondary));
            return;
        }
        String error = ExternalCalendarSyncStore.lastError(this);
        if (!error.isEmpty()) {
            status.setText(error);
            status.setTextColor(getColor(R.color.danger));
            return;
        }
        long last = ExternalCalendarSyncStore.lastSyncAt(this);
        if (last <= 0L) {
            status.setText("아직 전체 일정을 연동하지 않았습니다.");
        } else {
            String when = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT).format(new Date(last));
            status.setText(when + " · 예정 일정 "
                    + ExternalCalendarSyncStore.lastSyncCount(this) + "건 반영");
        }
        status.setTextColor(getColor(R.color.text_secondary));
    }

    private void requestCalendarPermissions() {
        if (ExternalCalendarSyncManager.hasPermissions(this)) {
            loadCalendars();
            return;
        }
        requestPermissions(new String[]{
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
        }, REQUEST_CALENDAR);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQUEST_CALENDAR) return;
        boolean granted = true;
        for (int value : results) {
            if (value != PackageManager.PERMISSION_GRANTED) granted = false;
        }
        if (granted) {
            loadCalendars();
            chooseCalendar();
        } else {
            Toast.makeText(this, "전체 일정 연동에는 캘린더 권한이 필요합니다.",
                    Toast.LENGTH_LONG).show();
        }
        render();
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView title(String value, float size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setIncludeFontPadding(false);
        return view;
    }

    private TextView body(String value) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(R.color.text_secondary));
        view.setTextSize(14f);
        view.setIncludeFontPadding(false);
        view.setLineSpacing(0f, 1.2f);
        return view;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dp(margin);
        return params;
    }

    private LinearLayout.LayoutParams fixedTop(int height, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(margin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
