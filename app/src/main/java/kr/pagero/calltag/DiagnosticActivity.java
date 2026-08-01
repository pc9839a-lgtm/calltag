package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class DiagnosticActivity extends Activity {
    private static final String PREFS = "calltag_diagnostic_checklist";

    private TextView statusTitle;
    private TextView reportView;
    private Button refreshButton;
    private Button repairButton;
    private Button copyButton;
    private LinearLayout checklist;
    private Button resetChecklist;
    private TextView reportToggle;
    private TextView checklistToggle;
    private DiagnosticReport.Snapshot snapshot;
    private boolean working;
    private final List<CheckItem> checks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildContent());
        refreshReport();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(getColor(R.color.background));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(27f);
        back.setOnClickListener(v -> {
            if (!working) finish();
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(46)));
        TextView title = title("앱 상태 진단", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(10);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        LinearLayout statusCard = card();
        statusTitle = title("상태를 확인하는 중입니다", 17f);
        statusCard.addView(statusTitle, matchWrap());
        TextView statusMeta = body("권한·SIM·예약·캠페인·데이터 연결 상태");
        statusCard.addView(statusMeta, topMargin(5));
        root.addView(statusCard, topMargin(14));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        refreshButton = button("새로고침", false);
        refreshButton.setOnClickListener(v -> refreshReport());
        actionRow.addView(refreshButton, new LinearLayout.LayoutParams(0, dp(48), 1f));
        repairButton = button("정합성 복구", true);
        repairButton.setOnClickListener(v -> confirmRepair());
        LinearLayout.LayoutParams repairParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        repairParams.leftMargin = dp(7);
        actionRow.addView(repairButton, repairParams);
        root.addView(actionRow, topMargin(8));

        reportToggle = toggleRow("상세 진단 정보");
        reportToggle.setOnClickListener(v -> toggleReport());
        root.addView(reportToggle, topMargin(20));

        reportView = body("진단 정보를 생성하고 있습니다");
        reportView.setTextIsSelectable(true);
        reportView.setBackgroundResource(R.drawable.bg_card);
        reportView.setPadding(dp(14), dp(13), dp(14), dp(13));
        reportView.setVisibility(View.GONE);
        root.addView(reportView, topMargin(8));

        copyButton = button("진단 정보 복사", false);
        copyButton.setOnClickListener(v -> copyReport());
        copyButton.setVisibility(View.GONE);
        root.addView(copyButton, fixedHeight(48, 8));

        checklistToggle = toggleRow("실기기 체크리스트");
        checklistToggle.setOnClickListener(v -> toggleChecklist());
        root.addView(checklistToggle, topMargin(20));

        checklist = card();
        checklist.setVisibility(View.GONE);
        addCheck(checklist, "single_sim", "단일 SIM 수동 문자 발송");
        addCheck(checklist, "dual_sim", "듀얼 SIM 선택 회선 발송");
        addCheck(checklist, "permission_denied", "권한 거부 시 데이터 유지");
        addCheck(checklist, "exclusion", "발송 제외 고객 건너뜀");
        addCheck(checklist, "duplicate", "같은 번호 중복발송 차단");
        addCheck(checklist, "invalid_phone", "잘못된 번호 개별 실패 처리");
        addCheck(checklist, "long_sms", "장문 문자 최종 상태 반영");
        addCheck(checklist, "scheduled", "예약 문자 시각·상태 반영");
        addCheck(checklist, "retry", "실패·건너뜀 고객만 재시도");
        addCheck(checklist, "app_killed", "앱 종료 상태 예약 발송");
        addCheck(checklist, "reboot", "재부팅 후 예약 상태 유지");
        addCheck(checklist, "campaign_100", "100명 캠페인 스크롤·상태 갱신");
        addCheck(checklist, "concurrent", "통화 정리와 캠페인 동시 처리");
        addCheck(checklist, "orphan_job", "고아 작업 자동 발송 차단");
        addCheck(checklist, "late_callback", "늦은 콜백 최종 상태 보호");
        addCheck(checklist, "campaign_delete", "캠페인 삭제 전 알람 정리");
        addCheck(checklist, "stale_group_member", "삭제 고객 그룹 참조 정리");
        root.addView(checklist, topMargin(8));

        resetChecklist = button("체크 초기화", false);
        resetChecklist.setOnClickListener(v -> confirmResetChecklist());
        resetChecklist.setVisibility(View.GONE);
        root.addView(resetChecklist, fixedHeight(46, 8));
        return scroll;
    }

    private void toggleReport() {
        boolean show = reportView.getVisibility() != View.VISIBLE;
        reportView.setVisibility(show ? View.VISIBLE : View.GONE);
        copyButton.setVisibility(show ? View.VISIBLE : View.GONE);
        reportToggle.setText("상세 진단 정보" + (show ? "    ︿" : "    ﹀"));
    }

    private void toggleChecklist() {
        boolean show = checklist.getVisibility() != View.VISIBLE;
        checklist.setVisibility(show ? View.VISIBLE : View.GONE);
        resetChecklist.setVisibility(show ? View.VISIBLE : View.GONE);
        checklistToggle.setText("실기기 체크리스트" + (show ? "    ︿" : "    ﹀"));
    }

    private TextView toggleRow(String label) {
        TextView row = title(label + "    ﹀", 15f);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(14), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setMinHeight(dp(54));
        return row;
    }

    private void refreshReport() {
        if (working) return;
        setWorking(true, "진단 중…");
        new Thread(() -> {
            DiagnosticReport.Snapshot result = DiagnosticReport.collect(this);
            runOnUiThread(() -> {
                snapshot = result;
                reportView.setText(result.text);
                statusTitle.setText(result.warningCount == 0
                        ? "확인 필요한 문제가 없습니다"
                        : "확인 필요 " + result.warningCount + "개");
                statusTitle.setTextColor(getColor(result.warningCount == 0
                        ? R.color.text_primary : R.color.danger));
                setWorking(false, "새로고침");
            });
        }, "calltag-diagnostic-refresh").start();
    }

    private void confirmRepair() {
        if (working) return;
        new AlertDialog.Builder(this)
                .setTitle("데이터 정합성 복구")
                .setMessage("잘못된 참조와 남은 알람을 정리합니다. 누락 문자를 만들거나 자동 재발송하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("복구", (dialog, which) -> repair())
                .show();
    }

    private void repair() {
        if (working) return;
        setWorking(true, "복구 중…");
        new Thread(() -> {
            DiagnosticReport.RepairResult repair = DiagnosticReport.reconcileCampaigns(this);
            DiagnosticReport.Snapshot refreshed = DiagnosticReport.collect(this);
            runOnUiThread(() -> {
                snapshot = refreshed;
                reportView.setText(refreshed.text);
                statusTitle.setText(refreshed.warningCount == 0
                        ? "확인 필요한 문제가 없습니다"
                        : "확인 필요 " + refreshed.warningCount + "개");
                statusTitle.setTextColor(getColor(refreshed.warningCount == 0
                        ? R.color.text_primary : R.color.danger));
                setWorking(false, "새로고침");
                Toast.makeText(this, repair.summary(), Toast.LENGTH_LONG).show();
            });
        }, "calltag-diagnostic-repair").start();
    }

    private void copyReport() {
        if (snapshot == null || snapshot.text.trim().isEmpty()) {
            Toast.makeText(this, "먼저 진단을 실행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "클립보드를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("콜태그 비식별 진단", snapshot.text));
        Toast.makeText(this, "진단 정보를 복사했습니다.", Toast.LENGTH_SHORT).show();
    }

    private void addCheck(LinearLayout parent, String key, String label) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        CheckBox check = new CheckBox(this);
        check.setText(label);
        check.setTextColor(getColor(R.color.text_primary));
        check.setTextSize(13f);
        check.setPadding(0, dp(5), 0, dp(5));
        check.setChecked(prefs.getBoolean(key, false));
        check.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(key, isChecked).apply());
        checks.add(new CheckItem(key, check));
        parent.addView(check, matchWrap());
    }

    private void confirmResetChecklist() {
        new AlertDialog.Builder(this)
                .setTitle("체크 초기화")
                .setMessage("이 기기에 저장된 체크 상태를 모두 해제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("초기화", (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
                    for (CheckItem item : checks) item.view.setChecked(false);
                })
                .show();
    }

    private void setWorking(boolean value, String refreshText) {
        working = value;
        refreshButton.setEnabled(!value);
        repairButton.setEnabled(!value);
        copyButton.setEnabled(!value);
        refreshButton.setAlpha(value ? 0.55f : 1f);
        repairButton.setAlpha(value ? 0.55f : 1f);
        copyButton.setAlpha(value ? 0.55f : 1f);
        refreshButton.setText(refreshText);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(12f);
        text.setIncludeFontPadding(false);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(13f);
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

    private static final class CheckItem {
        final String key;
        final CheckBox view;

        CheckItem(String key, CheckBox view) {
            this.key = key;
            this.view = view;
        }
    }
}
