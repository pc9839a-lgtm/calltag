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
        root.setPadding(dp(20), dp(18), dp(20), dp(42));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> {
            if (!working) finish();
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("앱 상태 진단", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        root.addView(header, matchWrap());

        LinearLayout notice = card();
        notice.addView(title("실제 단말 테스트용", 17f), matchWrap());
        notice.addView(body("권한·SIM·예약·캠페인 상태를 자동 확인합니다. 실제 문자 발송 성공 여부는 테스트 번호로 직접 확인해야 합니다."), topMargin(8));
        root.addView(notice, topMargin(18));

        statusTitle = title("상태를 확인하는 중입니다.", 18f);
        root.addView(statusTitle, topMargin(20));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        refreshButton = button("새로고침", false);
        refreshButton.setOnClickListener(v -> refreshReport());
        actionRow.addView(refreshButton, new LinearLayout.LayoutParams(0, dp(52), 1f));
        repairButton = button("상태 재계산", true);
        repairButton.setOnClickListener(v -> confirmRepair());
        LinearLayout.LayoutParams repairParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        repairParams.leftMargin = dp(8);
        actionRow.addView(repairButton, repairParams);
        root.addView(actionRow, topMargin(10));

        reportView = body("진단 정보를 생성하고 있습니다.");
        reportView.setTextIsSelectable(true);
        reportView.setBackgroundResource(R.drawable.bg_card);
        reportView.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.addView(reportView, topMargin(12));

        copyButton = button("비식별 진단 정보 복사", false);
        copyButton.setOnClickListener(v -> copyReport());
        root.addView(copyButton, fixedHeight(52, 10));
        root.addView(body("복사되는 정보에는 고객명·전화번호·문자 본문이 포함되지 않습니다. 고객 데이터 내보내기 기능이 아닙니다."), topMargin(8));

        root.addView(title("실제 단말 E2E 체크리스트", 18f), topMargin(26));
        root.addView(body("각 항목을 실제 휴대전화에서 확인한 뒤 체크하세요. 체크 상태는 이 기기에만 저장됩니다."), topMargin(7));

        LinearLayout checklist = card();
        addCheck(checklist, "single_sim", "단일 SIM에서 수동 문자 발송 성공");
        addCheck(checklist, "dual_sim", "듀얼 SIM에서 선택 회선 발송 성공");
        addCheck(checklist, "permission_denied", "문자·전화·알림 권한 거부 시 데이터 손실 없음");
        addCheck(checklist, "exclusion", "발송 제외 고객이 실제로 건너뜀 처리됨");
        addCheck(checklist, "duplicate", "같은 번호·같은 캠페인 중복발송 차단");
        addCheck(checklist, "invalid_phone", "잘못된 번호가 전체 캠페인을 중단하지 않음");
        addCheck(checklist, "long_sms", "장문 문자가 분할 발송되고 최종 상태가 반영됨");
        addCheck(checklist, "scheduled", "예약 시각에 문자 발송 및 상태 반영");
        addCheck(checklist, "retry", "실패·건너뜀 고객만 재시도 가능");
        addCheck(checklist, "app_killed", "앱을 닫은 상태에서 예약 발송 동작 확인");
        addCheck(checklist, "reboot", "재부팅 후 예약 작업 상태 확인");
        addCheck(checklist, "campaign_100", "100명 이상 캠페인 생성·스크롤·상태 갱신 확인");
        addCheck(checklist, "concurrent", "통화 종료 정리와 캠페인 발송 동시 발생 확인");
        root.addView(checklist, topMargin(10));

        Button resetChecklist = button("체크리스트 초기화", false);
        resetChecklist.setOnClickListener(v -> confirmResetChecklist());
        root.addView(resetChecklist, fixedHeight(48, 10));
        return scroll;
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
                        ? "자동 점검 결과 · 확인 필요 없음"
                        : "자동 점검 결과 · 확인 필요 " + result.warningCount + "개");
                setWorking(false, "새로고침");
            });
        }, "calltag-diagnostic-refresh").start();
    }

    private void confirmRepair() {
        if (working) return;
        new AlertDialog.Builder(this)
                .setTitle("캠페인 상태를 재계산할까요?")
                .setMessage("문자 작업의 실제 상태를 기준으로 캠페인 수신자와 캠페인 집계를 다시 맞춥니다. 고객·문자·캠페인 데이터는 삭제하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("재계산", (dialog, which) -> repair())
                .show();
    }

    private void repair() {
        if (working) return;
        setWorking(true, "재계산 중…");
        new Thread(() -> {
            DiagnosticReport.RepairResult repair = DiagnosticReport.reconcileCampaigns(this);
            DiagnosticReport.Snapshot refreshed = DiagnosticReport.collect(this);
            runOnUiThread(() -> {
                snapshot = refreshed;
                reportView.setText(refreshed.text);
                statusTitle.setText(refreshed.warningCount == 0
                        ? "자동 점검 결과 · 확인 필요 없음"
                        : "자동 점검 결과 · 확인 필요 " + refreshed.warningCount + "개");
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
        Toast.makeText(this, "비식별 진단 정보를 복사했습니다.", Toast.LENGTH_SHORT).show();
    }

    private void addCheck(LinearLayout parent, String key, String label) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        CheckBox check = new CheckBox(this);
        check.setText(label);
        check.setTextColor(getColor(R.color.text_primary));
        check.setTextSize(14f);
        check.setPadding(0, dp(7), 0, dp(7));
        check.setChecked(prefs.getBoolean(key, false));
        check.setOnCheckedChangeListener((buttonView, isChecked) ->
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(key, isChecked).apply());
        checks.add(new CheckItem(key, check));
        parent.addView(check, matchWrap());
    }

    private void confirmResetChecklist() {
        new AlertDialog.Builder(this)
                .setTitle("체크리스트 초기화")
                .setMessage("이 기기에 저장된 실제 단말 테스트 체크 상태를 모두 해제할까요?")
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
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
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
        text.setTextSize(13f);
        text.setLineSpacing(dp(3), 1f);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
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
