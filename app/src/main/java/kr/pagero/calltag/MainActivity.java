package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class MainActivity extends Activity {
    private CallTagDbHelper db;

    private View sectionToday;
    private View sectionCustomers;
    private View sectionConsultations;
    private View sectionMore;

    private NavItemTextView navToday;
    private NavItemTextView navCustomers;
    private NavItemTextView navConsultations;
    private NavItemTextView navMore;

    private TextView todayDueCount;
    private TextView overdueCount;
    private LinearLayout customerList;
    private LinearLayout consultationSummary;
    private LinearLayout moreMenuList;

    private String activeCustomerFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new CallTagDbHelper(this);
        bindViews();
        bindActions();
        selectSection(sectionToday, navToday);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private void bindViews() {
        sectionToday = findViewById(R.id.sectionToday);
        sectionCustomers = findViewById(R.id.sectionCustomers);
        sectionConsultations = findViewById(R.id.sectionConsultations);
        sectionMore = findViewById(R.id.sectionMore);

        navToday = findViewById(R.id.navToday);
        navCustomers = findViewById(R.id.navCustomers);
        navConsultations = findViewById(R.id.navConsultations);
        navMore = findViewById(R.id.navMore);

        todayDueCount = findViewById(R.id.todayDueCount);
        overdueCount = findViewById(R.id.overdueCount);
        customerList = findViewById(R.id.customerList);
        consultationSummary = findViewById(R.id.consultationSummary);
        moreMenuList = findViewById(R.id.moreMenuList);
    }

    private void bindActions() {
        navToday.setOnClickListener(v -> selectSection(sectionToday, navToday));
        navCustomers.setOnClickListener(v -> selectSection(sectionCustomers, navCustomers));
        navConsultations.setOnClickListener(v -> selectSection(sectionConsultations, navConsultations));
        navMore.setOnClickListener(v -> selectSection(sectionMore, navMore));

        findViewById(R.id.addCustomerButton).setOnClickListener(v -> showAddCustomerDialog());
        findViewById(R.id.filterAll).setOnClickListener(v -> setCustomerFilter(null));
        findViewById(R.id.filterNew).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_NEW));
        findViewById(R.id.filterConsulting).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_CONSULTING));
        findViewById(R.id.filterExisting).setOnClickListener(v -> setCustomerFilter(CallTagDbHelper.STATUS_EXISTING));
    }

    private void selectSection(View selectedSection, NavItemTextView selectedNav) {
        sectionToday.setVisibility(selectedSection == sectionToday ? View.VISIBLE : View.GONE);
        sectionCustomers.setVisibility(selectedSection == sectionCustomers ? View.VISIBLE : View.GONE);
        sectionConsultations.setVisibility(selectedSection == sectionConsultations ? View.VISIBLE : View.GONE);
        sectionMore.setVisibility(selectedSection == sectionMore ? View.VISIBLE : View.GONE);

        int active = getColor(R.color.primary);
        int inactive = getColor(R.color.nav_inactive);
        navToday.setTextColor(selectedNav == navToday ? active : inactive);
        navCustomers.setTextColor(selectedNav == navCustomers ? active : inactive);
        navConsultations.setTextColor(selectedNav == navConsultations ? active : inactive);
        navMore.setTextColor(selectedNav == navMore ? active : inactive);

        refreshAll();
    }

    private void refreshAll() {
        todayDueCount.setText(String.valueOf(db.countDueTodayTasks()));
        overdueCount.setText(String.valueOf(db.countOverdueTasks()));
        renderCustomers();
        renderConsultationSummary();
        renderMoreMenu();
    }

    private void setCustomerFilter(String status) {
        activeCustomerFilter = status;
        renderCustomers();

        setFilterColor(R.id.filterAll, status == null);
        setFilterColor(R.id.filterNew, CallTagDbHelper.STATUS_NEW.equals(status));
        setFilterColor(R.id.filterConsulting, CallTagDbHelper.STATUS_CONSULTING.equals(status));
        setFilterColor(R.id.filterExisting, CallTagDbHelper.STATUS_EXISTING.equals(status));
    }

    private void setFilterColor(int viewId, boolean selected) {
        Button button = findViewById(viewId);
        button.setTextColor(getColor(selected ? R.color.primary : R.color.text_secondary));
    }

    private void renderCustomers() {
        customerList.removeAllViews();
        List<Customer> customers = db.listCustomers(activeCustomerFilter);

        if (customers.isEmpty()) {
            TextView empty = createBodyText(activeCustomerFilter == null
                    ? "아직 등록된 고객이 없습니다.\n통화 연동 전에는 고객 직접 추가로 구조를 확인할 수 있습니다."
                    : "해당 상태의 고객이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(30), dp(24), dp(30));
            empty.setBackgroundResource(R.drawable.bg_card);
            customerList.addView(empty, matchWrap());
            return;
        }

        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (Customer customer : customers) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(17), dp(18), dp(17));
            card.setBackgroundResource(R.drawable.bg_card);

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView name = new TextView(this);
            name.setText(customer.displayName);
            name.setTextColor(getColor(R.color.text_primary));
            name.setTextSize(17f);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            name.setIncludeFontPadding(false);
            header.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView badge = new TextView(this);
            badge.setText(statusLabel(customer.relationStatus));
            badge.setTextColor(statusColor(customer.relationStatus));
            badge.setTextSize(12f);
            badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            badge.setIncludeFontPadding(false);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(10), dp(6), dp(10), dp(6));
            badge.setBackgroundResource(R.drawable.bg_badge);
            header.addView(badge);

            card.addView(header, matchWrap());

            TextView phone = createBodyText(customer.primaryPhone);
            LinearLayout.LayoutParams phoneParams = matchWrap();
            phoneParams.topMargin = dp(11);
            card.addView(phone, phoneParams);

            TextView meta = createMutedText("최근 상담  " + dateFormat.format(new Date(customer.lastContactAt)));
            LinearLayout.LayoutParams metaParams = matchWrap();
            metaParams.topMargin = dp(6);
            card.addView(meta, metaParams);

            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.bottomMargin = dp(12);
            customerList.addView(card, cardParams);
        }
    }

    private void renderConsultationSummary() {
        consultationSummary.removeAllViews();
        addMetricCard(consultationSummary, "진행 중 상담", db.countOpenOpportunities() + "건", false);
        addMetricCard(consultationSummary, "신규 고객", db.countCustomersByStatus(CallTagDbHelper.STATUS_NEW) + "명", false);
        addMetricCard(consultationSummary, "상담 중 고객", db.countCustomersByStatus(CallTagDbHelper.STATUS_CONSULTING) + "명", false);
        addMetricCard(consultationSummary, "기존 고객", db.countCustomersByStatus(CallTagDbHelper.STATUS_EXISTING) + "명", false);
    }

    private void renderMoreMenu() {
        if (moreMenuList.getChildCount() > 0) return;

        addMenuCard("통화 감지 설정", "업무시간, 최소 통화시간, 신규번호 표시 기준");
        addMenuCard("고객 분류 설정", "휴면 기준일, 상담 결과, 다음 행동 항목");
        addMenuCard("제외번호", "개인전화, 거래처, 스팸 번호 관리");
        addMenuCard("개인정보 및 데이터", "권한 안내, 데이터 초기화, 개인정보 처리 안내");
        addMenuCard("계정과 이용권", "MVP 검증 후 원스토어 결제를 연결합니다.");
    }

    private void addMetricCard(LinearLayout parent, String label, String value, boolean danger) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(R.drawable.bg_card);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(getColor(R.color.text_primary));
        labelView.setTextSize(16f);
        card.addView(labelView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(getColor(danger ? R.color.danger : R.color.text_primary));
        valueView.setTextSize(22f);
        valueView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(valueView);

        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(12);
        parent.addView(card, params);
    }

    private void addMenuCard(String title, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackgroundResource(R.drawable.bg_card);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getColor(R.color.text_primary));
        titleView.setTextSize(16f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(titleView, matchWrap());

        TextView descriptionView = createMutedText(description);
        LinearLayout.LayoutParams descriptionParams = matchWrap();
        descriptionParams.topMargin = dp(7);
        card.addView(descriptionView, descriptionParams);

        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(12);
        moreMenuList.addView(card, params);
    }

    private void showAddCustomerDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(6), dp(20), 0);

        EditText nameInput = createInput("고객명 또는 업체명", InputType.TYPE_CLASS_TEXT);
        form.addView(nameInput, matchWrap());

        EditText phoneInput = createInput("전화번호", InputType.TYPE_CLASS_PHONE);
        LinearLayout.LayoutParams phoneParams = matchWrap();
        phoneParams.topMargin = dp(12);
        form.addView(phoneInput, phoneParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("신규 고객 등록")
                .setMessage("거래 완료 전까지 신규 고객으로 분류합니다.")
                .setView(form)
                .setNegativeButton("취소", null)
                .setPositiveButton("등록", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                db.insertNewLead(nameInput.getText().toString(), phoneInput.getText().toString());
                Toast.makeText(this, "신규 고객으로 등록했습니다.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                activeCustomerFilter = null;
                setCustomerFilter(null);
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (RuntimeException e) {
                Toast.makeText(this, "고객을 등록하지 못했습니다.", Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private EditText createInput(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(16f);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        return input;
    }

    private TextView createBodyText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(15f);
        text.setLineSpacing(0f, 1.3f);
        return text;
    }

    private TextView createMutedText(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_muted));
        text.setTextSize(13f);
        return text;
    }

    private String statusLabel(String status) {
        switch (status) {
            case CallTagDbHelper.STATUS_NEW: return "신규";
            case CallTagDbHelper.STATUS_CONSULTING: return "상담 중";
            case CallTagDbHelper.STATUS_EXISTING: return "기존";
            case CallTagDbHelper.STATUS_VIP: return "VIP";
            case CallTagDbHelper.STATUS_DORMANT: return "휴면";
            case CallTagDbHelper.STATUS_OPT_OUT: return "수신거부";
            case CallTagDbHelper.STATUS_EXCLUDED: return "제외";
            default: return status;
        }
    }

    private int statusColor(String status) {
        if (CallTagDbHelper.STATUS_OPT_OUT.equals(status)) return getColor(R.color.danger);
        if (CallTagDbHelper.STATUS_NEW.equals(status)) return getColor(R.color.primary);
        return getColor(R.color.text_secondary);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
