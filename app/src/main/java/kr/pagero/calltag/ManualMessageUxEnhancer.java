package kr.pagero.calltag;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;

/** 기존 문자 발송 로직은 유지하고 선택 템플릿의 표시와 수정 행동만 강화한다. */
public final class ManualMessageUxEnhancer {
    private static final String TAG = "manual_message_template_enhanced";

    private ManualMessageUxEnhancer() {}

    public static void enhance(ManualMessageActivity activity) {
        View root = activity.findViewById(android.R.id.content);
        TextView selected = findTemplateText(root);
        if (selected == null) return;
        refreshLabel(activity, selected);
        if (TAG.equals(selected.getTag())) return;
        selected.setTag(TAG);
        selected.setTextSize(17f);
        selected.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selected.setTextColor(activity.getColor(R.color.text_primary));

        ViewGroup parent = selected.getParent() instanceof ViewGroup
                ? (ViewGroup) selected.getParent() : null;
        if (!(parent instanceof LinearLayout)) return;
        LinearLayout card = (LinearLayout) parent;

        Button edit = new Button(activity);
        edit.setText("선택한 템플릿 수정");
        edit.setAllCaps(false);
        edit.setTextSize(14f);
        edit.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        edit.setTextColor(activity.getColor(R.color.text_primary));
        edit.setBackgroundResource(R.drawable.bg_secondary_button);
        edit.setMinWidth(0);
        edit.setVisibility(hasSelectedTemplate(activity) ? View.VISIBLE : View.GONE);
        edit.setOnClickListener(v -> openSelectedTemplate(activity));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 48));
        params.topMargin = dp(activity, 8);
        card.addView(edit, params);

        selected.addTextChangedListener(new TextWatcher() {
            private boolean applying;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable value) {
                if (applying) return;
                applying = true;
                refreshLabel(activity, selected);
                edit.setVisibility(hasSelectedTemplate(activity) ? View.VISIBLE : View.GONE);
                applying = false;
            }
        });
    }

    private static void refreshLabel(ManualMessageActivity activity, TextView selected) {
        String id = selectedTemplateId(activity);
        if (id.isEmpty()) {
            selected.setText("선택한 템플릿 없음");
            return;
        }
        MessageTemplateStore.Template template = MessageTemplateStore.get(activity, id);
        String name = template == null ? "선택한 템플릿" : template.name;
        String desired = "선택됨 · " + name;
        if (!desired.contentEquals(selected.getText())) selected.setText(desired);
    }

    private static void openSelectedTemplate(ManualMessageActivity activity) {
        String id = selectedTemplateId(activity);
        if (id.isEmpty()) {
            Toast.makeText(activity, "먼저 템플릿을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        activity.startActivity(new android.content.Intent(
                activity, MessageTemplateEditorActivity.class)
                .putExtra(MessageTemplateEditorActivity.EXTRA_TEMPLATE_ID, id));
    }

    private static boolean hasSelectedTemplate(ManualMessageActivity activity) {
        return !selectedTemplateId(activity).isEmpty();
    }

    private static String selectedTemplateId(ManualMessageActivity activity) {
        try {
            Field field = ManualMessageActivity.class.getDeclaredField("selectedTemplateId");
            field.setAccessible(true);
            Object value = field.get(activity);
            return value == null ? "" : String.valueOf(value).trim();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static TextView findTemplateText(View view) {
        if (view instanceof TextView) {
            String text = String.valueOf(((TextView) view).getText());
            if (text.contains("선택한 템플릿") || text.startsWith("선택됨 ·")) {
                return (TextView) view;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findTemplateText(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int dp(ManualMessageActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
