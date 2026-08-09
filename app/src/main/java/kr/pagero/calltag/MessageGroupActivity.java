package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 수동그룹은 검색·전체선택을 지원하고 스마트그룹은 실제 CRM 조건으로 계산한다. */
public final class MessageGroupActivity extends Activity {
    private MessageGroupStore store;
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new MessageGroupStore(this);
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
        header.setPadding(dp(8), 0, dp(16), 0);
        TextView back = title("‹", 31f);
        back.setGravity(Gravity.CENTER);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(52)));
        TextView screenTitle = title("고객 그룹", 21f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(8);
        header.addView(screenTitle, titleParams);
        page.addView(header, matchWrap());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(16), dp(8), dp(16), dp(8));
        Button manual = button("수동 그룹", true);
        manual.setOnClickListener(v -> showManualEditor(null));
        actions.addView(manual, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button smart = button("스마트 그룹", false);
        smart.setOnClickListener(v -> showSmartEditor(null));
        LinearLayout.LayoutParams smartParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        smartParams.leftMargin = dp(8);
        actions.addView(smart, smartParams);
        page.addView(actions, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(16), dp(2), dp(16), dp(32));
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
        List<MessageGroupStore.Group> groups = store.list();
        if (groups.isEmpty()) {
            TextView empty = body("아직 만든 그룹이 없습니다.");
            empty.setGravity(Gravity.CENTER);
            empty.setBackgroundResource(R.drawable.bg_card);
            empty.setPadding(dp(18), dp(30), dp(18), dp(30));
            listContainer.addView(empty, topMargin(10));
            return;
        }
        for (MessageGroupStore.Group group : groups) {
            listContainer.addView(groupCard(group), topMargin(8));
        }
    }

    private View groupCard(MessageGroupStore.Group group) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(13));
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = title(group.name, 16f);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        nameRow.addView(name, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView type = body(MessageGroupStore.TYPE_MANUAL.equals(group.type) ? "수동" : "스마트");
        type.setTextColor(getColor(R.color.primary));
        type.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        type.setBackgroundResource(R.drawable.bg_soft_panel);
        type.setPadding(dp(10), dp(5), dp(10), dp(5));
        nameRow.addView(type);
        card.addView(nameRow, matchWrap());

        int count = store.countMembers(this, group);
        TextView rule = body(MessageGroupStore.describe(group) + " · " + count + "명");
        rule.setSingleLine(true);
        rule.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(rule, topMargin(6));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button campaign = button("단체문자", true);
        campaign.setEnabled(count > 0);
        campaign.setAlpha(count > 0 ? 1f : 0.45f);
        campaign.setOnClickListener(v -> startActivity(new Intent(this, CampaignComposerActivity.class)
                .putExtra(CampaignComposerActivity.EXTRA_GROUP_ID, group.id)));
        actions.addView(campaign, new LinearLayout.LayoutParams(0, dp(44), 1f));

        Button edit = button("수정", false);
        edit.setOnClickListener(v -> {
            if (MessageGroupStore.TYPE_MANUAL.equals(group.type)) showManualEditor(group);
            else showSmartEditor(group);
        });
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, dp(44), 1f);
        editParams.leftMargin = dp(7);
        actions.addView(edit, editParams);

        Button delete = button("삭제", false);
        delete.setTextColor(getColor(R.color.danger));
        delete.setOnClickListener(v -> confirmDelete(group));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, dp(44), 0.8f);
        deleteParams.leftMargin = dp(7);
        actions.addView(delete, deleteParams);
        card.addView(actions, topMargin(11));
        return card;
    }

    private void showManualEditor(MessageGroupStore.Group current) {
        CallTagDbHelper crm = new CallTagDbHelper(this);
        List<Customer> customers;
        try {
            customers = crm.listCustomers(null);
        } finally {
            crm.close();
        }
        if (customers.isEmpty()) {
            Toast.makeText(this, "먼저 고객을 등록해주세요.", Toast.LENGTH_LONG).show();
            return;
        }

        Set<Long> selectedIds = new HashSet<>();
        if (current != null) selectedIds.addAll(store.manualMemberIds(current.id));
        List<ManualRow> rows = new ArrayList<>();

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(4), dp(18), dp(4));

        EditText name = input("그룹 이름");
        name.setText(current == null ? "" : current.name);
        content.addView(name, fixedHeight(50, 0));

        EditText search = input("고객명·전화번호·상태 검색");
        content.addView(search, fixedHeight(48, 9));

        TextView selectedText = body("");
        selectedText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selectedText.setTextColor(getColor(R.color.text_primary));

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.addView(selectedText, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button selectAll = button("전체 선택", false);
        tools.addView(selectAll, new LinearLayout.LayoutParams(dp(92), dp(42)));
        Button clearAll = button("전체 해제", false);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(92), dp(42));
        clearParams.leftMargin = dp(6);
        tools.addView(clearAll, clearParams);
        content.addView(tools, topMargin(9));

        LinearLayout customerList = new LinearLayout(this);
        customerList.setOrientation(LinearLayout.VERTICAL);
        for (Customer customer : customers) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(5), dp(4), dp(5));
            row.setBackgroundResource(R.drawable.bg_clickable_row);

            CheckBox check = new CheckBox(this);
            check.setChecked(selectedIds.contains(customer.id));
            check.setTag(customer.id);
            row.addView(check, new LinearLayout.LayoutParams(dp(48), dp(48)));

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView customerName = title(customer.displayName, 14f);
            customerName.setSingleLine(true);
            customerName.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(customerName, matchWrap());
            TextView meta = body(customer.primaryPhone + " · " + customer.relationStatus);
            meta.setSingleLine(true);
            meta.setEllipsize(TextUtils.TruncateAt.END);
            labels.addView(meta, topMargin(3));
            row.addView(labels, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> check.setChecked(!check.isChecked()));
            ManualRow holder = new ManualRow(customer, row, check);
            rows.add(holder);
            check.setOnCheckedChangeListener((buttonView, checked) -> {
                if (checked) selectedIds.add(customer.id);
                else selectedIds.remove(customer.id);
                updateSelectedCount(selectedIds, selectedText);
            });
            customerList.addView(row, topMargin(2));
        }
        updateSelectedCount(selectedIds, selectedText);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterManualRows(rows, s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        selectAll.setOnClickListener(v -> setVisibleRowsChecked(rows, true));
        clearAll.setOnClickListener(v -> setVisibleRowsChecked(rows, false));

        ScrollView listScroll = new ScrollView(this);
        listScroll.setFillViewport(true);
        listScroll.addView(customerList, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));
        content.addView(listScroll, fixedHeight(360, 8));

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle(current == null ? "수동 그룹 만들기" : "수동 그룹 수정")
                .setView(content)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (selectedIds.isEmpty()) {
                    Toast.makeText(this, "고객을 한 명 이상 선택해주세요.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    store.saveManual(current == null ? "" : current.id,
                            name.getText().toString(), new ArrayList<>(selectedIds));
                    dialog.dismiss();
                    render();
                } catch (IllegalArgumentException error) {
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
        dialog.show();
    }

    private void filterManualRows(List<ManualRow> rows, String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.KOREA);
        String phone = PhoneNumberNormalizer.normalize(rawQuery);
        for (ManualRow holder : rows) {
            Customer customer = holder.customer;
            boolean visible = query.isEmpty()
                    || customer.displayName.toLowerCase(Locale.KOREA).contains(query)
                    || customer.relationStatus.toLowerCase(Locale.KOREA).contains(query)
                    || (!phone.isEmpty()
                    && PhoneNumberNormalizer.normalize(customer.primaryPhone).contains(phone));
            holder.row.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setVisibleRowsChecked(List<ManualRow> rows, boolean checked) {
        for (ManualRow holder : rows) {
            if (holder.row.getVisibility() == View.VISIBLE) holder.check.setChecked(checked);
        }
    }

    private void updateSelectedCount(Set<Long> selectedIds, TextView text) {
        text.setText("선택 " + selectedIds.size() + "명");
    }

    private void showSmartEditor(MessageGroupStore.Group current) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(4), dp(18), dp(4));
        EditText name = input("그룹 이름");
        name.setText(current == null ? "" : current.name);
        form.addView(name, fixedHeight(50, 0));

        CallTagDbHelper crm = new CallTagDbHelper(this);
        List<StageOption> stages;
        try {
            stages = crm.listStages();
        } finally {
            crm.close();
        }
        List<String> statusValues = new ArrayList<>();
        List<String> statusLabels = new ArrayList<>();
        statusValues.add("");
        statusLabels.add("모든 고객 상태");
        for (StageOption stage : stages) {
            statusValues.add(stage.name);
            statusLabels.add("상태 · " + stage.name);
        }
        Spinner status = spinner(statusLabels);
        selectValue(status, statusValues, current == null ? "" : current.statusFilter);
        form.addView(label("고객 상태"), topMargin(14));
        form.addView(status, fixedHeight(50, 6));

        List<String> inactiveLabels = List.of("기간 제한 없음", "7일 이상 미접촉",
                "30일 이상 미접촉", "90일 이상 미접촉");
        int[] inactiveValues = {0, 7, 30, 90};
        Spinner inactive = spinner(inactiveLabels);
        int currentDays = current == null ? 0 : current.inactiveDays;
        for (int i = 0; i < inactiveValues.length; i++) {
            if (inactiveValues[i] == currentDays) inactive.setSelection(i);
        }
        form.addView(label("최근 연락"), topMargin(12));
        form.addView(inactive, fixedHeight(50, 6));

        Switch pending = new Switch(this);
        pending.setText("미완료 할 일이 있는 고객만");
        pending.setTextColor(getColor(R.color.text_primary));
        pending.setTextSize(14f);
        pending.setChecked(current != null && current.pendingOnly);
        form.addView(pending, fixedHeight(52, 12));

        TextView ruleNote = body("스마트 그룹은 고객 상태 · 최근 연락 · 미완료 할 일 조건으로 자동 갱신됩니다.");
        ruleNote.setBackgroundResource(R.drawable.bg_soft_panel);
        ruleNote.setPadding(dp(12), dp(10), dp(12), dp(10));
        form.addView(ruleNote, topMargin(12));

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle(current == null ? "스마트 그룹 만들기" : "스마트 그룹 수정")
                .setView(form)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            CallTagDialogStyler.apply(dialog);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    MessageGroupStore.Group saved = store.saveSmart(
                            current == null ? "" : current.id,
                            name.getText().toString(),
                            statusValues.get(status.getSelectedItemPosition()),
                            inactiveValues[inactive.getSelectedItemPosition()],
                            pending.isChecked(),
                            MessageGroupStore.TRANSACTION_ANY);
                    int count = store.countMembers(this, saved);
                    dialog.dismiss();
                    Toast.makeText(this, "현재 조건에 맞는 고객 " + count + "명",
                            Toast.LENGTH_LONG).show();
                    render();
                } catch (IllegalArgumentException error) {
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
        dialog.show();
    }

    private void confirmDelete(MessageGroupStore.Group group) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_CallTag_Dialog)
                .setTitle("그룹 삭제")
                .setMessage("‘" + group.name + "’ 그룹을 삭제할까요? 기존 단체문자 내역은 유지됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (ignored, which) -> {
                    store.delete(group.id);
                    render();
                })
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private Spinner spinner(List<String> labels) {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(R.drawable.bg_secondary_button);
        spinner.setPadding(dp(8), 0, dp(8), 0);
        spinner.setPopupBackgroundResource(R.drawable.bg_dialog_panel);
        spinner.setAdapter(new CallTagSpinnerAdapter(this, labels));
        return spinner;
    }

    private void selectValue(Spinner spinner, List<String> values, String value) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(getColor(R.color.text_muted));
        input.setTextColor(getColor(R.color.text_primary));
        input.setTextSize(14f);
        input.setSingleLine(true);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackgroundResource(R.drawable.bg_input);
        return input;
    }

    private TextView label(String value) {
        TextView text = title(value, 13f);
        text.setTextColor(getColor(R.color.text_secondary));
        return text;
    }

    private TextView title(String value, float size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_primary));
        text.setTextSize(size);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView body(String value) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(R.color.text_secondary));
        text.setTextSize(13f);
        text.setIncludeFontPadding(false);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(primary ? android.R.color.white : R.color.text_primary));
        button.setBackgroundResource(primary
                ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        return button;
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

    private LinearLayout.LayoutParams fixedHeight(int height, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(height));
        params.topMargin = dp(topMargin);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (store != null) store.close();
        super.onDestroy();
    }

    private static final class ManualRow {
        final Customer customer;
        final View row;
        final CheckBox check;

        ManualRow(Customer customer, View row, CheckBox check) {
            this.customer = customer;
            this.row = row;
            this.check = check;
        }
    }
}
