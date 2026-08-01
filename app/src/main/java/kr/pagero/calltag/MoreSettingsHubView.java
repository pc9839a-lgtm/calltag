package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 텍스트 나열 대신 검색 가능한 큰 버튼 카드로 구성한 더보기. */
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
        search.setTextSize(15f);
        search.setTextColor(getContext().getColor(R.color.text_primary));
        search.setHintTextColor(getContext().getColor(R.color.text_muted));
        search.setBackgroundResource(R.drawable.bg_input);
        search.setPadding(dp(16), 0, dp(16), 0);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        addView(search, new LayoutParams(LayoutParams.MATCH_PARENT, dp(52)));

        Section messages = section("문자");
        messages.add("문자 템플릿", "템플릿 문구 이미지", MessageTemplateLibraryActivity.class);
        messages.add("통화 후 자동문자", "자동 발송 부재중 후속", MessageAutomationSettingsActivity.class);
        messages.add("그룹·단체문자", "고객 그룹 캠페인", GroupCampaignHubActivity.class);
        messages.add("발송 관리", "발송 내역 제외 번호", MessageSafetyHubActivity.class);

        Section customers = section("고객·일정");
        customers.add("고객 상태", "고객 단계 상태 색상", StageSettingsActivity.class);
        customers.add("일정 종류", "할 일 일정 종류", TaskTypeSettingsActivity.class);

        Section app = section("앱·계정");
        app.add("계정", "개인정보 로그인", AccountActivity.class);
        app.add("앱 진단", "권한 오류 상태", DiagnosticActivity.class);
        app.add("백업·복원", "암호화 백업 복원", BackupRestoreActivity.class);
    }

    private Section section(String title) {
        Section section = new Section(title);
        sections.add(section);
        addView(section.root, topMargin(18));
        return section;
    }

    private void filter(String raw) {
        String query = raw.trim().toLowerCase(Locale.KOREA);
        for (MenuItem item : items) {
            boolean visible = query.isEmpty()
                    || item.searchText.contains(query);
            item.button.setVisibility(visible ? VISIBLE : GONE);
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
            label.setTextColor(getContext().getColor(R.color.text_primary));
            label.setTextSize(17f);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            label.setIncludeFontPadding(false);
            root.addView(label, new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            rows = new LinearLayout(getContext());
            rows.setOrientation(VERTICAL);
            root.addView(rows, topMargin(8));
        }

        void add(String title, String keywords, Class<?> destination) {
            Button button = new Button(getContext());
            button.setText(title);
            button.setAllCaps(false);
            button.setTextSize(15f);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setTextColor(getContext().getColor(R.color.text_primary));
            button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            button.setPadding(dp(18), 0, dp(18), 0);
            button.setBackgroundResource(R.drawable.bg_secondary_button);
            button.setMinWidth(0);
            button.setOnClickListener(v -> getContext().startActivity(
                    new Intent(getContext(), destination)));

            MenuItem item = new MenuItem(button,
                    (title + " " + keywords).toLowerCase(Locale.KOREA));
            items.add(item);
            menuItems.add(item);
            rows.addView(button, rowParams());
        }

        void refreshVisibility() {
            boolean any = false;
            for (MenuItem item : menuItems) {
                if (item.button.getVisibility() == VISIBLE) {
                    any = true;
                    break;
                }
            }
            root.setVisibility(any ? VISIBLE : GONE);
        }
    }

    private static final class MenuItem {
        final Button button;
        final String searchText;

        MenuItem(Button button, String searchText) {
            this.button = button;
            this.searchText = searchText;
        }
    }

    private LayoutParams rowParams() {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(58));
        params.bottomMargin = dp(8);
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
