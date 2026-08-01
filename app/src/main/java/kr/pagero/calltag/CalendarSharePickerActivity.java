package kr.pagero.calltag;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 콜태그의 예정 일정을 선택해 설치된 Android 캘린더 앱에 등록한다. */
public final class CalendarSharePickerActivity extends Activity {
    private CallTagDbHelper db;
    private EditText searchInput;
    private LinearLayout listContainer;
    private List<FollowUpTask> tasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallTagDbHelper(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        tasks = db.listPendingTasks();
        render();
    }

    private View buildContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(6), dp(16), dp(6));
        TextView back = title("‹", 31f);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView screenTitle = title("외부 캘린더에 추가", 20f);
        LinearLayout.LayoutParams screenTitleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        screenTitleParams.leftMargin = dp(8);
        header.addView(screenTitle, screenTitleParams);
        page.addView(header, matchWrap());

        TextView guide = body("일정을 누르면 Google 캘린더·삼성 캘린더 등 저장할 앱과 계정을 선택할 수 있습니다.");
        guide.setPadding(dp(16), dp(8), dp(16), dp(10));
        page.addView(guide, matchWrap());

        searchInput = new EditText(this);
        searchInput.setHint("고객명·전화번호·일정 검색");
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14f);
        searchInput.setTextColor(getColor(R.color.text_primary));
        searchInput.setHintTextColor(getColor(R.color.text_muted));
        searchInput.setBackgroundResource(R.drawable.bg_input);
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                render();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
        searchParams.leftMargin = dp(16);
        searchParams.rightMargin = dp(16);
        page.addView(searchInput, searchParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(16), dp(6), dp(16), dp(32));
        scroll.addView(listContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void render() {
        if (listContainer == null) return;
        listContainer.removeAllViews();
        String query = searchInput == null ? "" : searchInput.getText().toString().trim();
        String textQuery = query.toLowerCase(Locale.KOREA);
        String phoneQuery = PhoneNumberNormalizer.normalize(query);

        int visible = 0;
        for (FollowUpTask task : tasks) {
            boolean match = textQuery.isEmpty()
                    || safe(task.customerName).toLowerCase(Locale.KOREA).contains(textQuery)
                    || safe(task.title).toLowerCase(Locale.KOREA).contains(textQuery)
                    || (!phoneQuery.isEmpty()
                    && PhoneNumberNormalizer.normalize(task.phone).contains(phoneQuery));
            if (!match) continue;
            listContainer.addView(taskRow(task), topMargin(8));
            visible++;
        }
        if (visible == 0) {
            TextView empty = body(tasks.isEmpty()
                    ? "공유할 예정 일정이 없습니다."
                    : "검색 결과가 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setBackgroundResource(R.drawable.bg_card);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            listContainer.addView(empty, topMargin(10));
        }
    }

    private View taskRow(FollowUpTask task) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(15), dp(12), dp(10), dp(12));
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> share(task));

        LinearLayout textArea = new LinearLayout(this);
        textArea.setOrientation(LinearLayout.VERTICAL);
        TextView name = title(task.customerName + " · " + task.title, 15f);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        textArea.addView(name, matchWrap());
        SimpleDateFormat format = new SimpleDateFormat("M월 d일 E a h:mm", Locale.KOREA);
        TextView meta = body(format.format(new Date(task.dueAt)) + " · " + task.phone);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        textArea.addView(meta, topMargin(5));
        row.addView(textArea, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = title("›", 24f);
        arrow.setTextColor(getColor(R.color.text_muted));
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(32), dp(44)));
        return row;
    }

    private void share(FollowUpTask task) {
        Customer customer = db.findCustomerById(task.customerId);
        String memo = customer == null ? "" : CustomerInsightResolver.latestMemo(db, customer);
        ExternalCalendarShare.open(this, task, customer, memo);
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
        view.setTextSize(13f);
        view.setIncludeFontPadding(false);
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}
