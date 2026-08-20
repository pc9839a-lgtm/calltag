package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Compact customer editor that stays on the current home screen. */
public final class QuickCustomerEditDialog {
    private QuickCustomerEditDialog() {}

    public static void show(Activity activity, PendingCallRecord call, Runnable onSaved) {
        if (activity == null || activity.isFinishing() || call == null) return;

        CallTagDbHelper db = new CallTagDbHelper(activity);
        Customer customer;
        try {
            customer = db.findByPhone(call.phone);
        } catch (RuntimeException error) {
            db.close();
            Toast.makeText(activity, "고객 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String initialName = customer != null ? customer.displayName : safe(call.cachedName);
        if (initialName.isEmpty()) initialName = defaultCustomerName(call.phone);
        String initialStatus = customer != null ? customer.relationStatus : db.firstStage();
        String initialMemo = customer != null ? customer.memo : "";
        long initialCustomerId = customer != null ? customer.id : -1L;

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 18), dp(activity, 8), dp(activity, 18), dp(activity, 8));

        TextView nameLabel = label(activity, "고객명");
        root.addView(nameLabel, matchWrap());
        EditText nameInput = input(activity, "고객명 또는 업체명", InputType.TYPE_CLASS_TEXT);
        nameInput.setText(initialName);
        nameInput.setSelection(nameInput.length());
        root.addView(nameInput, height(activity, 52, 7));

        root.addView(labelWithTop(activity, "고객 상태", 15), matchWrap());
        Button statusButton = new Button(activity);
        statusButton.setAllCaps(false);
        statusButton.setTextSize(14f);
        statusButton.setTextColor(activity.getColor(R.color.text_primary));
        statusButton.setBackgroundResource(R.drawable.bg_secondary_button);
        final String[] selectedStatus = {initialStatus};
        statusButton.setText(selectedStatus[0] + "  ▾");
        root.addView(statusButton, height(activity, 50, 7));

        root.addView(labelWithTop(activity, "메모", 15), matchWrap());
        EditText memoInput = input(activity, "고객 메모를 입력하세요",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        memoInput.setSingleLine(false);
        memoInput.setGravity(Gravity.TOP | Gravity.START);
        memoInput.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        memoInput.setText(initialMemo);
        memoInput.setSelection(memoInput.length());
        root.addView(memoInput, height(activity, 128, 7));

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.Theme_CallTag_Dialog)
                .setTitle("고객 정보 수정")
                .setView(root)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", null)
                .create();

        statusButton.setOnClickListener(v -> {
            List<ActionChoiceDialog.Option> options = new ArrayList<>();
            try {
                for (StageOption stage : db.listStages()) {
                    options.add(new ActionChoiceDialog.Option(
                            stage.name,
                            stage.name,
                            stage.name.equals(selectedStatus[0]) ? "현재 상태" : "이 상태로 변경",
                            stage.color));
                }
            } catch (RuntimeException error) {
                Toast.makeText(activity, "고객 상태를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            ActionChoiceDialog.show(activity, "고객 상태", nameInput.getText().toString().trim(),
                    options, option -> {
                        selectedStatus[0] = option.key;
                        statusButton.setText(selectedStatus[0] + "  ▾");
                    });
        });

        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) name = defaultCustomerName(call.phone);
                String memo = memoInput.getText().toString().trim();
                try {
                    long customerId = initialCustomerId;
                    Customer latest = db.findByPhone(call.phone);
                    if (latest != null) customerId = latest.id;
                    if (customerId <= 0L) {
                        customerId = db.insertCustomer(name, call.phone, selectedStatus[0], memo);
                    } else {
                        db.updateCustomerProfile(customerId, name, selectedStatus[0], memo);
                    }
                    long now = System.currentTimeMillis();
                    db.insertInteraction(customerId, "MEMO_EDIT", now, now, 0L,
                            "UPDATED", memo);
                    HomeTaskRefreshStore.mark(activity);
                    Toast.makeText(activity, "고객 정보를 저장했습니다.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (onSaved != null) onSaved.run();
                } catch (RuntimeException error) {
                    Toast.makeText(activity, "고객 정보를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialog.setOnDismissListener(ignored -> db.close());
        dialog.show();
    }

    private static EditText input(Activity activity, String hint, int inputType) {
        EditText input = new EditText(activity);
        input.setHint(hint);
        input.setInputType(inputType);
        input.setTextSize(15f);
        input.setTextColor(activity.getColor(R.color.text_primary));
        input.setHintTextColor(activity.getColor(R.color.text_muted));
        input.setBackgroundResource(R.drawable.bg_input);
        input.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
        return input;
    }

    private static TextView label(Activity activity, String value) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(13f);
        view.setTextColor(activity.getColor(R.color.text_secondary));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private static TextView labelWithTop(Activity activity, String value, int top) {
        TextView view = label(activity, value);
        view.setPadding(0, dp(activity, top), 0, 0);
        return view;
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams height(Activity activity, int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, height));
        params.topMargin = dp(activity, top);
        return params;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String defaultCustomerName(String phone) {
        String normalized = PhoneNumberNormalizer.normalize(phone);
        String suffix = normalized.length() >= 4
                ? normalized.substring(normalized.length() - 4) : normalized;
        return suffix.isEmpty() ? "새 고객" : "고객 " + suffix;
    }
}
