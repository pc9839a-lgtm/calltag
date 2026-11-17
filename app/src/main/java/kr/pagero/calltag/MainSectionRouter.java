package kr.pagero.calltag;

import android.view.View;
import android.widget.TextView;

/** Switches MainActivity sections without synthetic click callbacks. */
public final class MainSectionRouter {
    private MainSectionRouter() {}

    public static void showHome(MainActivity activity) {
        show(activity, R.id.sectionToday, R.id.navToday, "home");
    }

    public static void showCustomers(MainActivity activity) {
        show(activity, R.id.sectionCustomers, R.id.navCustomers, "customers");
    }

    private static void show(MainActivity activity, int sectionId, int navId, String source) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        select(activity.getWindow().getDecorView(), sectionId, navId);
        CrashTelemetryStore.record(activity, "main_section", "show", source);
    }

    static void select(View root, int sectionId, int navId) {
        int[] sections = {R.id.sectionToday, R.id.sectionCustomers,
                R.id.sectionConsultations, R.id.sectionStats, R.id.sectionMore};
        int[] navs = {R.id.navToday, R.id.navCustomers, R.id.navConsultations, R.id.navStats, R.id.navMore};
        for (int id : sections) {
            View view = root.findViewById(id);
            if (view != null) view.setVisibility(id == sectionId ? View.VISIBLE : View.GONE);
        }
        int active = root.getContext().getColor(R.color.primary);
        int inactive = root.getContext().getColor(R.color.nav_inactive);
        for (int id : navs) {
            View view = root.findViewById(id);
            if (view instanceof HomeNavItemTextView) {
                view.setSelected(id == navId);
            } else if (view instanceof TextView) {
                ((TextView) view).setTextColor(id == navId ? active : inactive);
            }
        }
    }
}
