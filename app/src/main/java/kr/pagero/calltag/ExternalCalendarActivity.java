package kr.pagero.calltag;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 콜태그의 예정 일정을 외부 캘린더 앱으로 보낸다. */
public final class ExternalCalendarActivity extends Activity {
    private CallTagDbHelper db;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallTagDbHelper(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private View buildContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(getColor(R.color.background));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(4), dp(16), dp(4));
        TextView back = title("‹", 30f);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView screenTitle = title("외부 캘린더로 보내기", 20f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(6);
        header.addView(screenTitle, titleParams);
        page.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(16), dp(8), dp(16), dp(36));
        scroll.addView(list, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void render() {
        list.removeAllViews();
        List<FollowUpTask> tasks = db.listPendingTasks();
        if (tasks.isEmpty()) {
            TextView empty = body("공유할 예정 일정이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setBackgroundResource(R.drawable.bg_card);
            empty.setPadding(dp(16), dp(30), dp(16), dp(30));
            list.addView(empty, matchWrap());
            return;
        }

        TextView guide = body("일정을 누르면 Google 캘린더·삼성 캘린더 등 설치된 캘린더 앱의 저장 화면이 열립니다.");
        guide.setLineSpacing(0f, 1.2f);
        list.addView(guide, matchWrap());

        SimpleDateFormat date = new SimpleDateFormat("M월 d일 E a h:mm", Locale.KOREA);
        for (FollowUpTask task : tasks) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(14), dp(12), dp(10), dp(12));
            card.setBackgroundResource(R.drawable.bg_clickable_row);
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> CalendarShareManager.open(this, task));

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView name = title(task.title, 15f);
            labels.addView(name, matchWrap());
            TextView meta = body(task.customerName + " · " + date.format(new Date(task.dueAt)));
            meta.setSingleLine(true);
            labels.addView(meta, topMargin(4));
            card.addView(labels, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button add = button("추가");
            add.setOnClickListener(v -> CalendarShareManager.open(this, task));
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(70), dp(42));
            buttonParams.leftMargin = dp(8);
            card.addView(add, buttonParams);

            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.topMargin = dp(8);
            list.addView(card, cardParams);
        }
    }

    private Button button(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.primary));
        button.setBackgroundResource(R.drawable.bg_secondary_button);
        button.setMinWidth(0);
        return button;
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}
