package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class CustomerAddActivity extends Activity {
    private static final int REQUEST_CONTACT = 2301;

    private CallTagDbHelper db;
    private boolean saving;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new CallTagDbHelper(this);
        setContentView(buildScreen());
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

        TextView contact = text("연락처에서 선택", 15f, R.color.text_primary, true);
        contact.setGravity(Gravity.CENTER);
        contact.setBackgroundResource(R.drawable.bg_clickable_row);
        contact.setFocusable(true);
        contact.setOnClickListener(v -> {
            if (!saving) pickContact();
        });
        content.addView(contact, fixedHeight(52, 10));

        scroll.addView(content, matchWrap());
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void pickContact() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_PICK, Phone.CONTENT_URI),
                    REQUEST_CONTACT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "연락처 앱을 열 수 없습니다. 번호를 직접 입력해주세요.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CONTACT || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        String name = "";
        String number = "";
        try (Cursor cursor = getContentResolver().query(data.getData(),
                new String[]{Phone.DISPLAY_NAME, Phone.NUMBER}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
                number = cursor.getString(1);
            }
        } catch (SecurityException | IllegalArgumentException e) {
            Toast.makeText(this, "선택한 연락처를 읽을 수 없습니다. 번호를 직접 입력해주세요.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        showRegistrationDialog(name, number);
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
}
