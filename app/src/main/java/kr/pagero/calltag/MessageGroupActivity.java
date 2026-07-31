package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
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
import java.util.Set;

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
        header.setPadding(dp(20), dp(16), dp(20), dp(12));
        Button back = button("‹", false);
        back.setTextSize(28f);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView title = title("문자 그룹", 22f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(title, titleParams);
        page.addView(header, matchWrap());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(dp(20), 0, dp(20), 0);
        Button manual = button("수동 그룹", true);
        manual.setOnClickListener(v -> showManualEditor(null));
        actions.addView(manual, new LinearLayout.LayoutParams(0, dp(52), 1f));
        Button smart = button("스마트 그룹", false);
        smart.setOnClickListener(v -> showSmartEditor(null));
        LinearLayout.LayoutParams smartParams = new LinearLayout.LayoutParams(0, dp(52), 1f);
        smartParams.leftMargin = dp(8);
        actions.addView(smart, smartParams);
        page.addView(actions, matchWrap());

        TextView guide = body("수동 그룹은 선택한 고객을 고정 저장합니다. 스마트 그룹은 캠페인을 만들 때 최신 고객 상태로 다시 계산합니다.");
        guide.setPadding(dp(20), dp(10), dp(20), dp(10));
        page.addView(guide, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(20), dp(4), dp(20), dp(40));
        scroll.addView(listContainer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return page;
    }

    private void render() {
        listContainer.removeAllViews();
        List<MessageGroupStore.Group> groups = store.list();
        if (groups.isEmpty()) {
            TextView empty = body("아직 만든 그룹이 없습니다.\n직접 고객을 고르거나 조건형 스마트 그룹을 만들어주세요.");
            empty.setGravity(Gravity.CENTER);
            empty.setBackgroundResource(R.drawable.bg_card);
            empty.setPadding(dp(18), dp(34), dp(18), dp(34));
            listContainer.addView(empty, matchWrap());
            return;
        }
        for (MessageGroupStore.Group group : groups) {
            listContainer.addView(groupCard(group), topMargin(10));
        }
    }

    private View groupCard(MessageGroupStore.Group group) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameRow.addView(title(group.name, 17f),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView type = body(MessageGroupStore.TYPE_MANUAL.equals(group.type) ? "수동" : "스마트");
        type.setBackgroundResource(R.drawable.bg_soft_panel);
        type.setPadding(dp(10), dp(5), dp(10), dp(5));
        nameRow.addView(type);
        card.addView(nameRow, matchWrap());

        int count = store.countMembers(this, group);
        TextView rule = body(MessageGroupStore.describe(group) + " · 현재 " + count + "명");
        card.addView(rule, topMargin(7));

        LinearLayout primary = new LinearLayout(this);
        primary.setOrientation(LinearLayout.HORIZONTAL);
        Button campaign = button("이 그룹에 문자", true);
        campaign.setEnabled(count > 0);
        campaign.setAlpha(count > 0 ? 1f : 0.45f);
        campaign.setOnClickListener(v -> startActivity(new Intent(this, CampaignComposerActivity.class)
                .putExtra(CampaignComposerActivity.EXTRA_GROUP_ID, group.id)));
        primary.addView(campaign, new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button edit = button("수정", false);
        edit.setOnClickListener(v -> {
            if (MessageGroupStore.TYPE_MANUAL.equals(group.type)) showManualEditor(group);
            else showSmartEditor(group);
        });
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, dp(48), 1f);
        editParams.leftMargin = dp(8);
        primary.addView(edit, editParams);
        card.addView(primary, topMargin(12));

        Button delete = button("그룹 삭제", false);
        delete.setOnClickListener(v -> confirmDelete(group));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
        deleteParams.topMargin = dp(8);
        card.addView(delete, deleteParams);
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

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(4), dp(20), dp(4));
        EditText name = input("그룹 이름");
        name.setText(current == null ? "" : current.name);
        form.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        TextView selectedText = body("");
        form.addView(selectedText, topMargin(10));
        Set<Long> selectedIds = new HashSet<>();
        if (current != null) selectedIds.addAll(store.manualMemberIds(current.id));
        List<CheckBox> checks = new ArrayList<>();
        LinearLayout customerList = new LinearLayout(this);
        customerList.setOrientation(LinearLayout.VERTICAL);
        for (Customer customer : customers) {
            CheckBox check = new CheckBox(this);
            check.setText(customer.displayName + "\n" + customer.primaryPhone
                    + " · " + customer.relationStatus);
            check.setTextColor(getColor(R.color.text_primary));
            check.setTextSize(14f);
            check.setPadding(0, dp(7), 0, dp(7));
            check.setTag(customer.id);
            check.setChecked(selectedIds.contains(customer.id));
            check.setOnCheckedChangeListener((buttonView, isChecked) -> updateSelectedCount(checks, selectedText));
            checks.add(check);
            customerList.addView(check, matchWrap());
        }
        updateSelectedCount(checks, selectedText);
        form.addView(customerList, topMargin(4));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(current == null ? "수동 그룹 만들기" : "수동 그룹 수정")
                .setView(scroll)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    List<Long> ids = new ArrayList<>();
                    for (CheckBox check : checks) {
                        if (check.isChecked()) ids.add((Long) check.getTag());
                    }
                    if (ids.isEmpty()) {
                        Toast.makeText(this, "고객을 한 명 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        store.saveManual(current == null ? "" : current.id,
                                name.getText().toString(), ids);
                        dialog.dismiss();
                        render();
                    } catch (IllegalArgumentException error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
    }

    private void updateSelectedCount(List<CheckBox> checks, TextView text) {
        int count = 0;
        for (CheckBox check : checks) if (check.isChecked()) count++;
        text.setText("선택한 고객 " + count + "명");
    }

    private void showSmartEditor(MessageGroupStore.Group current) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(4), dp(20), dp(4));
        EditText name = input("그룹 이름");
        name.setText(current == null ? "" : current.name);
        form.addView(name, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

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
        form.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        List<String> inactiveLabels = List.of("기간 제한 없음", "7일 이상 미접촉",
                "30일 이상 미접촉", "90일 이상 미접촉");
        int[] inactiveValues = {0, 7, 30, 90};
        Spinner inactive = spinner(inactiveLabels);
        int currentDays = current == null ? 0 : current.inactiveDays;
        for (int i = 0; i < inactiveValues.length; i++) {
            if (inactiveValues[i] == currentDays) inactive.setSelection(i);
        }
        form.addView(label("최근 연락"), topMargin(12));
        form.addView(inactive, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        Switch pending = new Switch(this);
        pending.setText("미완료 일정이 있는 고객만");
        pending.setTextColor(getColor(R.color.text_primary));
        pending.setChecked(current != null && current.pendingOnly);
        form.addView(pending, topMargin(12));

        List<String> transactionValues = List.of(
                MessageGroupStore.TRANSACTION_ANY,
                MessageGroupStore.TRANSACTION_HAS,
                MessageGroupStore.TRANSACTION_NONE);
        List<String> transactionLabels = List.of("거래 여부 전체", "거래 고객만", "미거래 고객만");
        Spinner transaction = spinner(transactionLabels);
        selectValue(transaction, transactionValues,
                current == null ? MessageGroupStore.TRANSACTION_ANY : current.transactionMode);
        form.addView(label("거래 여부"), topMargin(12));
        form.addView(transaction, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(current == null ? "스마트 그룹 만들기" : "스마트 그룹 수정")
                .setView(form)
                .setPositiveButton("저장", null)
                .setNegativeButton("취소", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        MessageGroupStore.Group saved = store.saveSmart(
                                current == null ? "" : current.id,
                                name.getText().toString(),
                                statusValues.get(status.getSelectedItemPosition()),
                                inactiveValues[inactive.getSelectedItemPosition()],
                                pending.isChecked(),
                                transactionValues.get(transaction.getSelectedItemPosition()));
                        int count = store.countMembers(this, saved);
                        dialog.dismiss();
                        Toast.makeText(this, "현재 조건에 맞는 고객 " + count + "명", Toast.LENGTH_LONG).show();
                        render();
                    } catch (IllegalArgumentException error) {
                        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }));
        dialog.show();
    }

    private void confirmDelete(MessageGroupStore.Group group) {
        new AlertDialog.Builder(this)
                .setTitle("그룹 삭제")
                .setMessage("‘" + group.name + "’ 그룹을 삭제할까요? 기존 캠페인 내역은 유지됩니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (dialog, which) -> {
                    store.delete(group.id);
                    render();
                })
                .show();
    }

    private Spinner spinner(List<String> labels) {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundResource(R.drawable.bg_secondary_button);
        spinner.setPadding(dp(12), 0, dp(12), 0);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
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
        input.setTextSize(15f);
        input.setSingleLine(true);
        input.setPadding(dp(14), dp(10), dp(14), dp(10));
        input.setBackgroundResource(R.drawable.bg_secondary_button);
        return input;
    }

    private TextView label(String value) {
        TextView text = title(value, 14f);
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
        text.setLineSpacing(dp(3), 1f);
        return text;
    }

    private Button button(String value, boolean primary) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14f);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (store != null) store.close();
        super.onDestroy();
    }
}
