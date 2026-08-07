package kr.pagero.calltag;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.WeakHashMap;

/** Makes home task/customer cards open the linked customer directly in edit mode. */
public final class MainActivityCardInteractionFix {
    private static final WeakHashMap<MainActivity, ViewTreeObserver.OnGlobalLayoutListener> LISTENERS =
            new WeakHashMap<>();

    private MainActivityCardInteractionFix() {}

    public static void install(MainActivity activity) {
        if (activity == null || activity.isFinishing() || LISTENERS.containsKey(activity)) return;
        View root = activity.findViewById(R.id.rootApp);
        if (root == null) return;

        WeakReference<MainActivity> reference = new WeakReference<>(activity);
        ViewTreeObserver.OnGlobalLayoutListener listener = () -> {
            MainActivity current = reference.get();
            if (current == null || current.isFinishing() || current.isDestroyed()) return;
            bindTaskCards(current);
            bindCustomerCards(current);
        };
        LISTENERS.put(activity, listener);
        root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        bindTaskCards(activity);
        bindCustomerCards(activity);
    }

    public static void uninstall(MainActivity activity) {
        if (activity == null) return;
        ViewTreeObserver.OnGlobalLayoutListener listener = LISTENERS.remove(activity);
        View root = activity.findViewById(R.id.rootApp);
        if (root != null && listener != null && root.getViewTreeObserver().isAlive()) {
            root.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    private static void bindTaskCards(MainActivity activity) {
        LinearLayout list = activity.findViewById(R.id.todayTaskList);
        if (list == null) return;
        CallTagDbHelper db = new CallTagDbHelper(activity);
        try {
            List<FollowUpTask> tasks = db.listPendingTasks();
            int count = Math.min(list.getChildCount(), tasks.size());
            for (int index = 0; index < count; index++) {
                View card = list.getChildAt(index);
                FollowUpTask task = tasks.get(index);
                bindCustomerOpen(activity, card, task.customerId, task.phone, "home_task_card");
            }
        } finally {
            db.close();
        }
    }

    private static void bindCustomerCards(MainActivity activity) {
        LinearLayout list = activity.findViewById(R.id.customerList);
        if (list == null) return;
        CallTagDbHelper db = new CallTagDbHelper(activity);
        try {
            for (int index = 0; index < list.getChildCount(); index++) {
                View card = list.getChildAt(index);
                String phone = findPhone(card);
                if (phone.isEmpty()) continue;
                Customer customer = db.findByPhone(phone);
                if (customer != null) {
                    bindCustomerOpen(activity, card, customer.id, customer.primaryPhone,
                            "home_customer_card");
                }
            }
        } finally {
            db.close();
        }
    }

    private static void bindCustomerOpen(MainActivity activity, View card,
                                         long customerId, String phone, String source) {
        if (card == null || customerId <= 0L) return;
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            boolean opened = CustomerLaunchRouter.openForEdit(
                    activity, customerId, phone, source);
            if (!opened) {
                Toast.makeText(activity, "고객 수정 화면을 열지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String findPhone(View view) {
        if (view instanceof TextView) {
            String text = ((TextView) view).getText() == null
                    ? "" : ((TextView) view).getText().toString().trim();
            String normalized = PhoneNumberNormalizer.normalize(text);
            if (normalized.length() >= 8) return text;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                String found = findPhone(group.getChildAt(index));
                if (!found.isEmpty()) return found;
            }
        }
        return "";
    }
}
