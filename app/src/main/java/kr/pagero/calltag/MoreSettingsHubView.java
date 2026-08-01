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

/** 검색과 접이식 묶음으로 정리한 더보기 화면. */
public final class MoreSettingsHubView extends LinearLayout {
    private final List<Group> groups = new ArrayList<>();
    private EditText search;

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

        search = new EditText(getContext());
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
                applySearch(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        addView(search, new LayoutParams(LayoutParams.MATCH_PARENT, dp(52)));

        Group messages = addGroup("문자", true);
        messages.add("문자 템플릿", MessageTemplateLibraryActivity.class);
        messages.add("자동문자", MessageAutomationSettingsActivity.class);
        messages.add("단체문자", CampaignListActivity.class);
        messages.add("고객 그룹", MessageGroupActivity.class);
        messages.add("발송 내역", MessageHistoryActivity.class);
        messages.add("발송 제외", MessageExclusionActivity.class);

        Group customer = addGroup("고객·일정", false);
        customer.add("고객 상태", StageSettingsActivity.class);
        customer.add("일정 종류", TaskTypeSettingsActivity.class);

        Group app = addGroup("앱·계정", false);
        app.add("계정 및 개인정보", AccountActivity.class);
        app.add("앱 상태 진단", DiagnosticActivity.class);
        app.add("백업 및 복원", BackupRestoreActivity.class);

        renderGroups();
    }

    private Group addGroup(String title, boolean open) {
        Group group = new Group(title, open);
        groups.add(group);
        return group;
    }

    private void applySearch(String value) {
        String query = value.trim().toLowerCase(Locale.KOREA);
        for (Group group : groups) {
            boolean groupMatch = group.title.toLowerCase(Locale.KOREA).contains(query);
            int visibleRows = 0;
            for (Row row : group.rows) {
                boolean visible = query.isEmpty()
                        || groupMatch
                        || row.title.toLowerCase(Locale.KOREA).contains(query);
                row.view.setVisibility(visible ? VISIBLE : GONE);
                if (visible) visibleRows++;
            }
            group.root.setVisibility(query.isEmpty() || visibleRows > 0 ? VISIBLE : GONE);
            group.forcedOpen = !query.isEmpty() && visibleRows > 0;
            group.render();
        }
    }

    private void renderGroups() {
        for (Group group : groups) {
            addView(group.root, topMargin(12));
            group.render();
        }
    }

    private final class Group {
        final String title;
        final LinearLayout root;
        final TextView header;
        final LinearLayout body;
        final List<Row> rows = new ArrayList<>();
        boolean open;
        boolean forcedOpen;

        Group(String title, boolean open) {
            this.title = title;
            this.open = open;
            root = new LinearLayout(getContext());
            root.setOrientation(VERTICAL);
            root.setBackgroundResource(R.drawable.bg_card);

            header = new TextView(getContext());
            header.setTextSize(17f);
            header.setTextColor(getContext().getColor(R.color.text_primary));
            header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(16), 0, dp(16), 0);
            header.setClickable(true);
            header.setFocusable(true);
            header.setOnClickListener(v -> {
                if (forcedOpen) return;
                this.open = !this.open;
                render();
            });
            root.addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, dp(58)));

            body = new LinearLayout(getContext());
            body.setOrientation(VERTICAL);
            body.setPadding(dp(8), 0, dp(8), dp(8));
            root.addView(body, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }

        void add(String title, Class<?> destination) {
            TextView view = new TextView(getContext());
            view.setText(title + "    ›");
            view.setTextSize(15f);
            view.setTextColor(getContext().getColor(R.color.text_primary));
            view.setGravity(Gravity.CENTER_VERTICAL);
            view.setPadding(dp(14), 0, dp(14), 0);
            view.setBackgroundResource(R.drawable.bg_clickable_row);
            view.setClickable(true);
            view.setFocusable(true);
            view.setOnClickListener(v -> getContext().startActivity(
                    new Intent(getContext(), destination)));
            Row row = new Row(title, view);
            rows.add(row);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(54));
            params.topMargin = dp(6);
            body.addView(view, params);
        }

        void render() {
            boolean expanded = forcedOpen || open;
            header.setText(title + (expanded ? "    ︿" : "    ﹀"));
            body.setVisibility(expanded ? VISIBLE : GONE);
        }
    }

    private static final class Row {
        final String title;
        final View view;

        Row(String title, View view) {
            this.title = title;
            this.view = view;
        }
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
