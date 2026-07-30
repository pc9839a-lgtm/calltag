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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.background));
        root.setFitsSystemWindows(true);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), 0, dp(20), 0);
        TextView back = text("‹", 34f, R.color.text_primary, false);
        back.setGravity(Gravity.CENTER);
        back.setBackgroundResource(R.drawable.bg_clickable_row);
        back.setOnClickListener(v -> {
            if (!saving) finish();
        });
        topBar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(66)));
        TextView topTitle = text("고객 추가", 19f, R.color.text_primary, true);
        topTitle.setGravity(Gravity.CENTER);
        topBar.addView(topTitle, new LinearLayout.LayoutParams(0, dp(66), 1f));
        topBar.addView(new View(this), new LinearLayout.LayoutParams(dp(48), dp(66)));
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
        content.setPadding(dp(20), dp(22), dp(20), dp(80));

        TextView heading = text("최근 통화에서 선택", 27f, R.color.text_primary, true);
        content.addView(heading, matchWrap());

        TextView direct = text("직접 입력", 15f, R.color.text_primary, true);
        direct.setGravity(Gravity.CENTER);
        direct.setBackgroundResource(R.drawable.bg_secondary_button);
        direct.setOnClickListener(v -> {
            if (!saving) showRegistrationDialog("", "");
        });
        LinearLayout.LayoutParams directParams = matchWrap();
        directParams.height = dp(52);
        directParams.topMargin = dp(16);
        content.addView(direct, directParams);

        TextView recentTitle = text("최근 통화", 17f, R.color.text_primary, true);
        LinearLayout.LayoutParams titleParams = matchWrap();
        titleParams.topMargin = dp(28);
        content.addView(recentTitle, titleParams);

        recentCallList = new LinearLayout(this);
        recentCallList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = matchWrap();
        listParams.topMargin = dp(12);
        content.addView(recentCallList, listParams);

        emptyState = text("불러올 최근 통화가 없습니다.", 14f, R.color.text_secondary, false);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams emptyParams = matchWrap();
        emptyParams.height = dp(96);
        emptyParams.topMargin = dp(12);
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
            emptyState.setText("통화 목록 불러오기");
            emptyState.setTextColor(getColor(R.color.text_primary));
            emptyState.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            emptyState.setBackgroundResource(R.drawable.bg_primary_button);
            emptyState.setOnClickListener(v -> requestPermissions(
                    new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_CALL_LOG));
            return;
        }

        List<RecentCallItem> calls = loadRecentCalls();
        emptyState.setOnClickListener(null);
        emptyState.setBackgroundResource(R.drawable.bg_card);
        emptyState.setTextColor(getColor(R.color.text_secondary));
        emptyState.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        emptyState.setText("불러올 최근 통화가 없습니다.");
        emptyState.setVisibility(calls.isEmpty() ? View.VISIBLE : View.GONE);

        SimpleDateFormat formatter = new SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA);
        for (RecentCallItem item : calls) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(18), dp(15), dp(18), dp(15));
            row.setBackgroundResource(R.drawable.bg_dialog_choice);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> {
                if (!saving) showRegistrationDialog(item.cachedName, item.number);
            });

            LinearLayout header = new LinearLayout(this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            String name = item.cachedName.isEmpty() ? "이름 없음" : item.cachedName;
            TextView nameView = text(name, 16f, R.color.text_primary, true);
            header.addView(nameView, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView typeView = text(callTypeLabel(item.type), 12f, R.color.primary, true);
            header.addView(typeView);
            row.addView(header, matchWrap());

            TextView phone = text(item.number, 15f, R.color.text_secondary, false);
            LinearLayout.LayoutParams phoneParams = matchWrap();
            phoneParams.topMargin = dp(7);
            row.addView(phone, phoneParams);

            String meta = formatter.format(new Date(item.date)) + " · " + durationLabel(item.durationSec);
            TextView metaView = text(meta, 12f, R.color.text_muted, false);
            LinearLayout.LayoutParams metaParams = matchWrap();
            metaParams.topMargin = dp(5);
            row.addView(metaView, metaParams);

            LinearLayout.LayoutParams rowParams = matchWrap();
            rowParams.bottomMargin = dp(10);
            recentCallList.addView(row, rowParams);
        }
    }

    private List<RecentCallItem> loadRecentCalls() {
        List<RecentCallItem> rows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
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

            int scanned = 0;
            while (cursor.moveToNext() && rows.size() < 40 && scanned < 160) {
                scanned++;
                String number = numberIndex >= 0 ? cursor.getString(numberIndex) : "";
                String normalized = PhoneNumberNormalizer.normalize(number);
                if (normalized.length() < 8 || seen.contains(normalized)) continue;
                seen.add(normalized);
                if (db.findByPhone(number) != null) continue;

                String name = nameIndex >= 0 ? cursor.getString(nameIndex) : "";
                int type = typeIndex >= 0 ? cursor.getInt(typeIndex) : CallLog.Calls.INCOMING_TYPE;
                long date = dateIndex >= 0 ? cursor.getLong(dateIndex) : System.currentTimeMillis();
                long duration = durationIndex >= 0 ? Math.max(0L, cursor.getLong(durationIndex)) : 0L;
                rows.add(new RecentCallItem(number, name == null ? "" : name.trim(), type, date, duration));
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
        form.addView(name, matchWrap());
        LinearLayout.LayoutParams phoneParams = matchWrap();
        phoneParams.topMargin = dp(12);
        form.addView(phone, phoneParams);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("고객 등록")
                .setView(form)
                .setNegativeButton("취소", null)
                .setPositiveButton("등록", null)
                .create();
        dialog.setOnShowListener(ignored -> {
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
        input.setTextSize(16f);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        return input;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
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

        RecentCallItem(String number, String cachedName, int type, long date, long durationSec) {
            this.number = number;
            this.cachedName = cachedName;
            this.type = type;
            this.date = date;
            this.durationSec = durationSec;
        }
    }
}