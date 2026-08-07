package kr.pagero.calltag;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CustomerAddActivity extends Activity {
    private static final int REQUEST_CALL_LOG = 2301;

    private CallTagDbHelper db;
    private LinearLayout recentCallList;
    private TextView emptyState;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallTagDbHelper(this);
        setContentView(buildScreen());
        renderRecentCalls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recentCallList != null) renderRecentCalls();
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.background));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(8), 0, dp(16), 0);
        TextView back = text("‹", 32f, R.color.text_primary, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> {
            if (!saving) finish();
        });
        topBar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(52)));
        TextView topTitle = text("고객 추가", 19f, R.color.text_primary, true);
        topTitle.setGravity(Gravity.CENTER);
        topBar.addView(topTitle, new LinearLayout.LayoutParams(0, dp(52), 1f));
        topBar.addView(new View(this), new LinearLayout.LayoutParams(dp(48), dp(52)));
        root.addView(topBar, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.border));
        root.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(14), dp(16), dp(48));

        TextView direct = text("직접 입력", 15f, android.R.color.white, true);
        direct.setGravity(Gravity.CENTER);
        direct.setBackgroundResource(R.drawable.bg_primary_button);
        direct.setClickable(true);
        direct.setFocusable(true);
        direct.setOnClickListener(v -> {
            if (!saving) showRegistrationDialog("", "");
        });
        content.addView(direct, fixedHeight(52, 0));

        TextView recentTitle = text("최근 통화", 13f, R.color.text_secondary, true);
        content.addView(recentTitle, topMargin(22));

        recentCallList = new LinearLayout(this);
        recentCallList.setOrientation(LinearLayout.VERTICAL);
        content.addView(recentCallList, topMargin(8));

        emptyState = text("불러올 최근 통화가 없습니다", 13f,
                R.color.text_secondary, false);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams emptyParams = fixedHeight(64, 8);
        content.addView(emptyState, emptyParams);

        scroll.addView(content, matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void renderRecentCalls() {
        recentCallList.removeAllViews();
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("최근 통화 불러오기");
            emptyState.setTextColor(getColor(android.R.color.white));
            emptyState.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            emptyState.setBackgroundResource(R.drawable.bg_primary_button);
            emptyState.setClickable(true);
            emptyState.setFocusable(true);
            emptyState.setOnClickListener(v -> requestPermissions(
                    new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_CALL_LOG));
            return;
        }

        List<RecentCallItem> calls = loadRecentCalls();
        emptyState.setOnClickListener(null);
        emptyState.setClickable(false);
        emptyState.setFocusable(false);
        emptyState.setBackgroundResource(R.drawable.bg_card);
        emptyState.setTextColor(getColor(R.color.text_secondary));
        emptyState.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        emptyState.setText("불러올 최근 통화가 없습니다");
        emptyState.setVisibility(calls.isEmpty() ? View.VISIBLE : View.GONE);

        SimpleDateFormat formatter = new SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA);
        for (RecentCallItem item : calls) {
            Customer registered = db.findByPhone(item.number);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(11), dp(8), dp(11));
            row.setBackgroundResource(R.drawable.bg_clickable_row);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                if (saving) return;
                Customer existing = db.findByPhone(item.number);
                if (existing != null) openCustomer(existing.id);
                else showRegistrationDialog(item.cachedName, item.number);
            });

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            String name = registered != null
                    ? registered.displayName
                    : (item.cachedName.isEmpty() ? "이름 없음" : item.cachedName);
            TextView nameView = text(name, 15f, R.color.text_primary, true);
            nameView.setSingleLine(true);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(nameView, matchWrap());

            TextView phone = text(item.number, 13f, R.color.text_secondary, false);
            phone.setSingleLine(true);
            phone.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(phone, topMargin(4));

            String meta = callTypeLabel(item.type) + " · "
                    + formatter.format(new Date(item.date)) + " · "
                    + durationLabel(item.durationSec);
            if (registered != null) meta += " · 등록됨";
            TextView metaView = text(meta, 11f, R.color.text_muted, false);
            metaView.setSingleLine(true);
            metaView.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(metaView, topMargin(4));
            row.addView(labels, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView arrow = text("›", 23f, R.color.text_muted, false);
            arrow.setGravity(Gravity.CENTER);
            row.addView(arrow, new LinearLayout.LayoutParams(dp(30), dp(46)));

            LinearLayout.LayoutParams rowParams = matchWrap();
            rowParams.bottomMargin = dp(6);
            recentCallList.addView(row, rowParams);
        }
    }

    /**
     * Customer-add recent calls intentionally mirrors the device CallLog instead of deduping by
     * phone number or hiding already-registered CallTag customers. Samsung/Pixel phone apps may
     * visually group consecutive calls, but every raw CallLog row remains visible here in the same
     * DATE DESC order so a call made after adding a customer never disappears from this screen.
     */
    private List<RecentCallItem> loadRecentCalls() {
        List<RecentCallItem> rows = new ArrayList<>();
        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
        };

        try (Cursor cursor = getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC")) {
            if (cursor == null) return rows;
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);

            while (cursor.moveToNext() && rows.size() < 60) {
                String number = numberIndex >= 0 ? cursor.getString(numberIndex) : "";
                String normalized = PhoneNumberNormalizer.normalize(number);
                if (normalized.length() < 8) continue;

                String name = nameIndex >= 0 ? cursor.getString(nameIndex) : "";
                int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : CallLog.Calls.INCOMING_TYPE;
                long date = dateIndex >= 0 ? cursor.getLong(dateIndex) : System.currentTimeMillis();
                long duration = durationIndex >= 0 ? Math.max(0L, cursor.getLong(durationIndex)) : 0L;
                rows.add(new RecentCallItem(number, name == null ? "" : name.trim(),
                        type, date, duration));
            }
        } catch (SecurityException ignored) {
            Toast.makeText(this, "통화 목록 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException ignored) {
            Toast.makeText(this, "통화 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
        return rows;
    }

    private void showRegistrationDialog(String defaultName, String defaultPhone) {
        Customer duplicate = db.findByPhone(defaultPhone);
        if (duplicate != null) {
            Toast.makeText(this, "이미 등록된 고객입니다.", Toast.LENGTH_SHORT).show();
            openCustomer(duplicate.id);
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);

        EditText name = input("고객명 또는 업체명", InputType.TYPE_CLASS_TEXT);
        name.setText(defaultName == null ? "" : defaultName);
        EditText phone = input("전화번호", InputType.TYPE_CLASS_PHONE);
        phone.setText(defaultPhone == null ? "" : defaultPhone);
        form.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        phoneParams.topMargin = dp(10);
        form.addView(phone, phoneParams);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("고객 등록")
                .setView(form)
                .setNegativeButton("취소", null)
                .setPositiveButton("등록", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            Button register = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            register.setOnClickListener(v -> {
                if (saving) return;
                String enteredPhone = phone.getText().toString().trim();
                if (PhoneNumberNormalizer.normalize(enteredPhone).length() < 8) {
                    Toast.makeText(this, "전화번호를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Customer existing = db.findByPhone(enteredPhone);
                if (existing != null) {
                    dialog.dismiss();
                    Toast.makeText(this, "이미 등록된 고객입니다.", Toast.LENGTH_SHORT).show();
                    openCustomer(existing.id);
                    return;
                }

                saving = true;
                register.setEnabled(false);
                register.setText("등록 중");
                try {
                    long id = db.insertNewLead(name.getText().toString(), enteredPhone);
                    dialog.dismiss();
                    openCustomer(id);
                } catch (IllegalArgumentException e) {
                    saving = false;
                    register.setEnabled(true);
                    register.setText("등록");
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                } catch (RuntimeException e) {
                    saving = false;
                    register.setEnabled(true);
                    register.setText("등록");
                    Toast.makeText(this, "고객을 등록하지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.setOnDismissListener(ignored -> saving = false);
        dialog.show();
    }

    private void openCustomer(long customerId) {
        startActivity(new Intent(this, CustomerDetailActivity.class)
                .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CALL_LOG) return;
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "직접 입력으로 고객을 추가할 수 있습니다.", Toast.LENGTH_SHORT).show();
        }
        renderRecentCalls();
    }

    private String callTypeLabel(int type) {
        if (type == CallLog.Calls.OUTGOING_TYPE) return "발신";
        if (type == CallLog.Calls.MISSED_TYPE) return "부재중";
        if (type == CallLog.Calls.REJECTED_TYPE) return "거절";
        if (type == CallLog.Calls.BLOCKED_TYPE) return "차단";
        return "수신";
    }

    private String durationLabel(long seconds) {
        long safeSeconds = Math.max(0L, seconds);
        long minutes = safeSeconds / 60L;
        long remain = safeSeconds % 60L;
        return minutes > 0 ? minutes + "분 " + remain + "초" : remain + "초";
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private EditText input(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(15f);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), 0, dp(14), 0);
        return input;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fixedHeight(int height, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(topMargin);
        return params;
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

    private static final class RecentCallItem {
        final String number;
        final String cachedName;
        final int type;
        final long date;
        final long durationSec;

        RecentCallItem(String number, String cachedName, int type,
                       long date, long durationSec) {
            this.number = number;
            this.cachedName = cachedName;
            this.type = type;
            this.date = date;
            this.durationSec = durationSec;
        }
    }
}
