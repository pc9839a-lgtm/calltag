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
        int[] sections = {R.id.sectionToday, R.id.sectionCustomers,
                R.id.sectionConsultations, R.id.sectionMore};
        int[] navs = {R.id.navToday, R.id.navCustomers, R.id.navConsultations, R.id.navMore};
        for (int id : sections) {
            View view = activity.findViewById(id);
            if (view != null) view.setVisibility(id == sectionId ? View.VISIBLE : View.GONE);
        }
        int active = activity.getColor(R.color.primary);
        int inactive = activity.getColor(R.color.nav_inactive);
        for (int id : navs) {
            View view = activity.findViewById(id);
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(id == navId ? active : inactive);
            }
        }
        CrashTelemetryStore.record(activity, "main_section", "show", source);
    }
}
