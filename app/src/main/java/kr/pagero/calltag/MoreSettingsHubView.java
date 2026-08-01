package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 검색 가능한 섹션형 설정 버튼 목록. */
public final class MoreSettingsHubView extends LinearLayout {
    private final List<MenuItem> items = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();

    public MoreSettingsHubView(Context context) {
        super(context);
        init();
    }

    public MoreSettingsHubView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MoreSettingsHubView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);

        EditText search = new EditText(getContext());
        search.setSingleLine(true);
        search.setHint("설정 검색");
        search.setTextSize(14f);
        search.setTextColor(getContext().getColor(R.color.text_primary));
        search.setHintTextColor(getContext().getColor(R.color.text_muted));
        search.setBackgroundResource(R.drawable.bg_input);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        addView(search, new LayoutParams(LayoutParams.MATCH_PARENT, dp(46)));

        Section messages = section("문자");
        messages.add("문자 템플릿", "템플릿 문구 이미지", MessageTemplateLibraryActivity.class);
        messages.add("자동문자", "통화 부재중 후속 자동", MessageAutomationSettingsActivity.class);
        messages.add("그룹·단체문자", "고객 그룹 캠페인", GroupCampaignHubActivity.class);
        messages.add("발송 관리", "발송 내역 제외 번호", MessageSafetyHubActivity.class);

        Section customers = section("고객·일정");
        customers.add("고객 상태", "단계 상태 색상", StageSettingsActivity.class);
        customers.add("일정 종류", "할 일 종류", TaskTypeSettingsActivity.class);

        Section app = section("앱·계정");
        app.add("계정 및 개인정보", "계정 개인정보 로그인", AccountActivity.class);
        app.add("앱 진단", "권한 오류 상태", DiagnosticActivity.class);
        app.add("백업 및 복원", "암호화 백업 복원", BackupRestoreActivity.class);
    }

    private Section section(String title) {
        Section section = new Section(title);
        sections.add(section);
        addView(section.root, topMargin(20));
        return section;
    }

    private void filter(String raw) {
        String query = raw.trim().toLowerCase(Locale.KOREA);
        for (MenuItem item : items) {
            item.row.setVisibility(query.isEmpty() || item.searchText.contains(query)
                    ? VISIBLE : GONE);
        }
        for (Section section : sections) section.refreshVisibility();
    }

    private final class Section {
        final LinearLayout root;
        final LinearLayout rows;
        final List<MenuItem> menuItems = new ArrayList<>();

        Section(String title) {
            root = new LinearLayout(getContext());
            root.setOrientation(VERTICAL);

            TextView label = new TextView(getContext());
            label.setText(title);
            label.setTextColor(getContext().getColor(R.color.text_secondary));
            label.setTextSize(13f);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            label.setIncludeFontPadding(false);
            root.addView(label, new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            rows = new LinearLayout(getContext());
            rows.setOrientation(VERTICAL);
            rows.setPadding(dp(4), dp(3), dp(4), dp(3));
            rows.setBackgroundResource(R.drawable.bg_card);
            root.addView(rows, topMargin(8));
        }

        void add(String title, String keywords, Class<?> destination) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), 0, dp(10), 0);
            row.setBackgroundResource(R.drawable.bg_clickable_row);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> getContext().startActivity(
                    new Intent(getContext(), destination)));

            TextView titleView = new TextView(getContext());
            titleView.setText(title);
            titleView.setTextSize(15f);
            titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            titleView.setTextColor(getContext().getColor(R.color.text_primary));
            titleView.setIncludeFontPadding(false);
            row.addView(titleView, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            TextView arrow = new TextView(getContext());
            arrow.setText("›");
            arrow.setTextSize(24f);
            arrow.setTextColor(getContext().getColor(R.color.text_muted));
            arrow.setGravity(Gravity.CENTER);
            row.addView(arrow, new LayoutParams(dp(28), dp(44)));

            MenuItem item = new MenuItem(row,
                    (title + " " + keywords).toLowerCase(Locale.KOREA));
            items.add(item);
            menuItems.add(item);
            rows.addView(row, rowParams());
        }

        void refreshVisibility() {
            boolean any = false;
            for (MenuItem item : menuItems) {
                if (item.row.getVisibility() == VISIBLE) {
                    any = true;
                    break;
                }
            }
            root.setVisibility(any ? VISIBLE : GONE);
        }
    }

    private static final class MenuItem {
        final View row;
        final String searchText;

        MenuItem(View row, String searchText) {
            this.row = row;
            this.searchText = searchText;
        }
    }

    private LayoutParams rowParams() {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(54));
        params.bottomMargin = dp(2);
        return params;
    }

    private LayoutParams topMargin(int value) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(value);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
