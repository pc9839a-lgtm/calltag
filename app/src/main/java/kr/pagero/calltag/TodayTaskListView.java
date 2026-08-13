package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** 오늘 할 일 카드에 작은 X 삭제 아이콘만 추가한다. */
public final class TodayTaskListView extends LinearLayout {
    private int renderIndex;

    public TodayTaskListView(Context context) {
        super(context);
    }

    public TodayTaskListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TodayTaskListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void removeAllViews() {
        renderIndex = 0;
        super.removeAllViews();
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        FollowUpTask task = taskAt(renderIndex++);
        if (task != null && child instanceof LinearLayout) {
            installDeleteIcon((LinearLayout) child, task);
        }
        super.addView(child, params);
    }

    private FollowUpTask taskAt(int index) {
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            List<FollowUpTask> tasks = db.listPendingTasks();
            return index >= 0 && index < tasks.size() ? tasks.get(index) : null;
        } finally {
            db.close();
        }
    }

    private void installDeleteIcon(LinearLayout card, FollowUpTask task) {
        LinearLayout actions = null;
        for (int i = card.getChildCount() - 1; i >= 0; i--) {
            View child = card.getChildAt(i);
            if (child instanceof LinearLayout
                    && ((LinearLayout) child).getOrientation() == HORIZONTAL) {
                actions = (LinearLayout) child;
                break;
            }
        }
        if (actions == null) return;

        Button delete = new Button(getContext());
        delete.setText("");
        delete.setContentDescription("할 일 삭제");
        delete.setMinWidth(0);
        delete.setMinimumWidth(0);
        delete.setMinHeight(0);
        delete.setMinimumHeight(0);
        delete.setPadding(0, 0, 0, 0);
        delete.setGravity(Gravity.CENTER);
        delete.setBackgroundResource(R.drawable.bg_clickable_row);
        delete.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_close, 0, 0, 0);
        delete.setOnClickListener(v -> confirmDelete(card, task));

        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(44), dp(46));
        deleteParams.leftMargin = dp(7);
        actions.addView(delete, deleteParams);
    }

    private void confirmDelete(View card, FollowUpTask task) {
        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.Theme_CallTag_Dialog)
                .setTitle("할 일 삭제")
                .setMessage("‘" + task.title + "’ 일정을 삭제합니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (value, which) -> deleteTask(card, task))
                .create();
        dialog.setOnShowListener(ignored -> CallTagDialogStyler.apply(dialog));
        dialog.show();
    }

    private void deleteTask(View card, FollowUpTask task) {
        CallTagDbHelper db = new CallTagDbHelper(getContext());
        try {
            db.deleteTask(task.id);
            ViewParent parent = new ViewParent(card.getParent());
            if (parent.list != null) parent.list.removeView(card);
            refreshCounts(db);
            Toast.makeText(getContext(), "삭제했습니다.", Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    private void refreshCounts(CallTagDbHelper db) {
        Activity activity = activity();
        if (activity == null) return;
        TextView due = activity.findViewById(R.id.todayDueCount);
        TextView overdue = activity.findViewById(R.id.overdueCount);
        TextView empty = activity.findViewById(R.id.todayEmpty);
        if (due != null) due.setText(db.countDueTodayTasks() + "\n오늘 할 일");
        if (overdue != null) overdue.setText(db.countOverdueTasks() + "\n기한 지남");
        if (empty != null) empty.setVisibility(getChildCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private Activity activity() {
        Context current = getContext();
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) return (Activity) current;
            Context next = ((ContextWrapper) current).getBaseContext();
            if (next == current) break;
            current = next;
        }
        return current instanceof Activity ? (Activity) current : null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class ViewParent {
        final LinearLayout list;
        ViewParent(android.view.ViewParent parent) {
            list = parent instanceof LinearLayout ? (LinearLayout) parent : null;
        }
    }
}
