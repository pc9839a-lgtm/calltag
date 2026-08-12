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

/** 더보기는 목적별 8개 진입점만 노출한다. */
public final class MoreSettingsHubView extends LinearLayout {
    private final List<MenuItem> items = new ArrayList<>();
    private final LinearLayout menuCard;

    public MoreSettingsHubView(Context context) {
        this(context, null);
    }

    public MoreSettingsHubView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoreSettingsHubView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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

        menuCard = new LinearLayout(getContext());
        menuCard.setOrientation(VERTICAL);
        menuCard.setPadding(dp(4), dp(3), dp(4), dp(3));
        menuCard.setBackgroundResource(R.drawable.bg_card);
        addView(menuCard, topMargin(14));

        addMenu("계정", "내 정보 로그인 로그아웃 회원탈퇴",
                v -> start(AccountActivity.class));
        addMenu("이용권", "구독 결제 무료 이용권 요금제 구매 복원",
                v -> start(BillingEntitlementActivity.class));
        addMenu("문자 관리", "통화 후 자동문자 템플릿 그룹 단체문자 발송 관리",
                v -> start(SettingsGroupActivity.intent(getContext(), SettingsGroupActivity.GROUP_MESSAGE)));
        addMenu("고객 관리", "고객 상태 일정 종류 통화 후 팝업 제외",
                v -> start(SettingsGroupActivity.intent(getContext(), SettingsGroupActivity.GROUP_CUSTOMER)));
        addMenu("페이지로", "페이지로 연결 연동 문의 고객 자동등록",
                v -> start(PageroConnectionCompactActivity.class));
        addMenu("파트너", "추천코드 친구 초대 파트너 현황 정산 수익",
                v -> start(ReferralPartnerActivity.class));
        addMenu("데이터 관리", "동기화 데이터 보호 복구 백업 복원 기기 변경",
                v -> start(SettingsGroupActivity.intent(getContext(), SettingsGroupActivity.GROUP_DATA)));
        addMenu("앱 정보", "버전 서비스 이용약관 개인정보처리방침 고객센터 문의",
                v -> start(AppInfoActivity.class));
    }

    private void addMenu(String title, String keywords, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), 0, dp(10), 0);
        row.setBackgroundResource(R.drawable.bg_clickable_row);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);

        TextView titleView = new TextView(getContext());
        titleView.setText(title);
        titleView.setTextSize(15.5f);
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
        menuCard.addView(row, rowParams());
    }

    private void filter(String raw) {
        String query = raw.trim().toLowerCase(Locale.KOREA);
        boolean any = false;
        for (MenuItem item : items) {
            boolean visible = query.isEmpty() || item.searchText.contains(query);
            item.row.setVisibility(visible ? VISIBLE : GONE);
            any |= visible;
        }
        menuCard.setVisibility(any ? VISIBLE : GONE);
    }

    private void start(Class<?> destination) {
        getContext().startActivity(new Intent(getContext(), destination));
    }

    private void start(Intent intent) {
        getContext().startActivity(intent);
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
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, dp(56));
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
