package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight customer editor used from home cards and caller popups.
 * Resolves the customer by id first and phone second so a stale id never closes the app.
 */
public final class CustomerQuickEditActivity extends Activity {
    public static final String EXTRA_CUSTOMER_ID = "customer_id";
    public static final String EXTRA_FALLBACK_PHONE = "fallback_phone";

    private CallTagDbHelper db;
    private long customerId;
    private String fallbackPhone = "";
    private String currentStatus = "";
    private Customer customer;
    private EditText nameInput;
    private TextView phoneView;
    private Button statusButton;
    private EditText memoInput;
    private Button saveButton;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallTagDbHelper(this);
        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, -1L);
        fallbackPhone = safe(getIntent().getStringExtra(EXTRA_FALLBACK_PHONE));
        setContentView(buildScreen());
        loadCustomer();
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.background));
        root.setFitsSystemWindows(true);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), 0, dp(12), 0);

        TextView back = text("‹", 32f, R.color.text_primary, false);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> {
            if (!saving) finish();
        });
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(56)));

        TextView title = text("고객 수정", 19f, R.color.text_primary, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));
        top.addView(new View(this), new LinearLayout.LayoutParams(dp(48), dp(56)));
        root.addView(top, matchWrap());

        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.border));
        root.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(36));

        content.addView(label("고객명"), matchWrap());
        nameInput = input("고객명 또는 업체명", InputType.TYPE_CLASS_TEXT);
        content.addView(nameInput, fixedHeight(52, 7));

        content.addView(labelWithTop("전화번호", 16), matchWrap());
        phoneView = text("", 15f, R.color.text_primary, false);
        phoneView.setGravity(Gravity.CENTER_VERTICAL);
        phoneView.setBackgroundResource(R.drawable.bg_soft_panel);
        phoneView.setPadding(dp(14), 0, dp(14), 0);
        content.addView(phoneView, fixedHeight(50, 7));

        content.addView(labelWithTop("고객 상태", 16), matchWrap());
        statusButton = new Button(this);
        statusButton.setAllCaps(false);
        statusButton.setTextSize(15f);
        statusButton.setTextColor(getColor(R.color.text_primary));
        statusButton.setBackgroundResource(R.drawable.bg_secondary_button);
        statusButton.setOnClickListener(v -> showStatusDialog());
        content.addView(statusButton, fixedHeight(50, 7));

        content.addView(labelWithTop("메모", 16), matchWrap());
        memoInput = input("고객 메모", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        memoInput.setSingleLine(false);
        memoInput.setGravity(Gravity.TOP | Gravity.START);
        memoInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        content.addView(memoInput, fixedHeight(132, 7));

        saveButton = new Button(this);
        saveButton.setText("저장");
        saveButton.setAllCaps(false);
        saveButton.setTextColor(getColor(android.R.color.white));
        saveButton.setTextSize(15f);
        saveButton.setBackgroundResource(R.drawable.bg_primary_button);
        saveButton.setOnClickListener(v -> save());
        content.addView(saveButton, fixedHeight(52, 22));

        Button detail = new Button(this);
        detail.setText("상세 이력 보기");
        detail.setAllCaps(false);
        detail.setTextColor(getColor(R.color.text_primary));
        detail.setTextSize(14f);
        detail.setBackgroundResource(R.drawable.bg_secondary_button);
        detail.setOnClickListener(v -> openDetail());
        content.addView(detail, fixedHeight(48, 10));

        scroll.addView(content, matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void loadCustomer() {
        try {
            customer = resolveCustomer();
        } catch (RuntimeException error) {
            customer = null;
        }
        if (customer == null) {
            openMainFallback("고객 정보를 다시 불러옵니다.");
            return;
        }
        customerId = customer.id;
        currentStatus = customer.relationStatus;
        nameInput.setText(customer.displayName);
        nameInput.setSelection(nameInput.length());
        phoneView.setText(customer.primaryPhone);
        statusButton.setText(currentStatus + "  ▾");
        memoInput.setText(customer.memo);
    }

    private Customer resolveCustomer() {
        Customer byId = customerId > 0L ? db.findCustomerById(customerId) : null;
        if (byId != null) return byId;
        return fallbackPhone.isEmpty() ? null : db.findByPhone(fallbackPhone);
    }

    private void showStatusDialog() {
        if (saving || customer == null) return;
        List<ActionChoiceDialog.Option> options = new ArrayList<>();
        try {
            for (StageOption stage : db.listStages()) {
                options.add(new ActionChoiceDialog.Option(
                        stage.name,
                        stage.name,
                        stage.name.equals(currentStatus) ? "현재 상태" : "이 상태로 변경",
                        stage.color));
            }
        } catch (RuntimeException error) {
            Toast.makeText(this, "고객 상태를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        ActionChoiceDialog.show(this, "고객 상태", customer.displayName,
                options, option -> {
                    currentStatus = option.key;
                    statusButton.setText(currentStatus + "  ▾");
                });
    }

    private void save() {
        if (saving) return;
        Customer latest;
        try {
            latest = resolveCustomer();
        } catch (RuntimeException error) {
            latest = null;
        }
        if (latest == null) {
            openMainFallback("고객 정보를 찾을 수 없습니다.");
            return;
        }

        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) name = latest.displayName;
        String status = currentStatus.isEmpty() ? latest.relationStatus : currentStatus;
        String memo = memoInput.getText().toString().trim();

        setSaving(true);
        try {
            db.updateCustomerProfile(latest.id, name, status, memo);
            ContactNameSyncManager.requestSyncAll(this);
            Toast.makeText(this, "고객 정보를 수정했습니다.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } catch (IllegalArgumentException error) {
            setSaving(false);
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            setSaving(false);
            Toast.makeText(this, "고객 정보를 저장하지 못했습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void openDetail() {
        if (customerId <= 0L || saving) return;
        try {
            startActivity(new Intent(this, CustomerDetailActivity.class)
                    .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, customerId));
        } catch (RuntimeException error) {
            Toast.makeText(this, "상세 화면을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openMainFallback(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        try {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (RuntimeException ignored) {
            // Auth/setup routing will recover on the next launcher open.
        }
        finish();
    }

    private void setSaving(boolean value) {
        saving = value;
        nameInput.setEnabled(!value);
        statusButton.setEnabled(!value);
        memoInput.setEnabled(!value);
        saveButton.setEnabled(!value);
        saveButton.setAlpha(value ? 0.55f : 1f);
        saveButton.setText(value ? "저장 중" : "저장");
    }

    private TextView label(String value) {
        return text(value, 14f, R.color.text_primary, true);
    }

    private TextView labelWithTop(String value, int marginTop) {
        TextView view = label(value);
        view.setPadding(0, dp(marginTop), 0, 0);
        return view;
    }

    private EditText input(String hint, int type) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setHintTextColor(getColor(R.color.text_muted));
        view.setTextColor(getColor(R.color.text_primary));
        view.setTextSize(15f);
        view.setInputType(type);
        view.setSingleLine(true);
        view.setBackgroundResource(R.drawable.bg_input);
        view.setPadding(dp(14), 0, dp(14), 0);
        return view;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(getColor(color));
        view.setTextSize(size);
        view.setIncludeFontPadding(false);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
        return view;
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}
