package kr.pagero.calltag;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/** Single defensive entry point for customer edit launches. */
public final class CustomerLaunchRouter {
    private static final long DUPLICATE_WINDOW_MS = 900L;

    private CustomerLaunchRouter() {}

    public static boolean openForEdit(Context context, long customerId,
                                      String fallbackPhone, String source) {
        if (context == null) return false;
        Resolved resolved = resolve(context, customerId, fallbackPhone);
        if (resolved == null) {
            CrashTelemetryStore.record(context, source, "customer_resolve_failed",
                    "id=" + customerId + ",phone=***" + phoneSuffix(fallbackPhone));
            return openHome(context, source + ":fallback_home");
        }

        String guardKey = "customer_edit:" + resolved.customerId;
        if (!UiLaunchGuard.tryAcquire(guardKey, DUPLICATE_WINDOW_MS)) {
            CrashTelemetryStore.record(context, source, "duplicate_suppressed", guardKey);
            return true;
        }

        try {
            context.startActivity(buildEditIntent(context, resolved.customerId, resolved.phone));
            CrashTelemetryStore.record(context, source, "launch_accepted",
                    "customer=" + resolved.customerId);
            return true;
        } catch (RuntimeException error) {
            UiLaunchGuard.release(guardKey);
            CrashTelemetryStore.record(context, source, "launch_failed",
                    error.getClass().getSimpleName() + ":" + String.valueOf(error.getMessage()));
            return false;
        }
    }

    public static PendingIntent pendingIntentForEdit(Context context, long customerId,
                                                     String fallbackPhone, int requestCode,
                                                     String source) {
        Resolved resolved = resolve(context, customerId, fallbackPhone);
        long id = resolved == null ? customerId : resolved.customerId;
        String phone = resolved == null ? safe(fallbackPhone) : resolved.phone;
        Intent target = resolved == null
                ? new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                : buildEditIntent(context, id, phone);
        CrashTelemetryStore.record(context, source,
                resolved == null ? "pending_fallback_home" : "pending_ready",
                "customer=" + id);
        return PendingIntent.getActivity(context, requestCode, target,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static Intent buildEditIntent(Context context, long customerId, String fallbackPhone) {
        Intent intent = new Intent(context, CustomerQuickEditActivity.class)
                .putExtra(CustomerQuickEditActivity.EXTRA_CUSTOMER_ID, customerId)
                .putExtra(CustomerQuickEditActivity.EXTRA_FALLBACK_PHONE, safe(fallbackPhone))
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private static Resolved resolve(Context context, long customerId, String fallbackPhone) {
        CallTagDbHelper db = new CallTagDbHelper(context);
        try {
            Customer customer = customerId > 0L ? db.findCustomerById(customerId) : null;
            if (customer == null && !safe(fallbackPhone).isEmpty()) {
                customer = db.findByPhone(fallbackPhone);
            }
            return customer == null ? null : new Resolved(customer.id, customer.primaryPhone);
        } catch (RuntimeException error) {
            CrashTelemetryStore.record(context, "customer_router", "resolve_exception",
                    error.getClass().getSimpleName());
            return null;
        } finally {
            db.close();
        }
    }

    private static boolean openHome(Context context, String source) {
        String key = "main_fallback";
        if (!UiLaunchGuard.tryAcquire(key, DUPLICATE_WINDOW_MS)) return true;
        try {
            Intent intent = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (!(context instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            CrashTelemetryStore.record(context, source, "launch_accepted", "");
            return true;
        } catch (RuntimeException error) {
            UiLaunchGuard.release(key);
            CrashTelemetryStore.record(context, source, "launch_failed",
                    error.getClass().getSimpleName());
            return false;
        }
    }

    private static String phoneSuffix(String value) {
        String digits = PhoneNumberNormalizer.normalize(value);
        if (digits.length() <= 4) return digits;
        return digits.substring(digits.length() - 4);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class Resolved {
        final long customerId;
        final String phone;

        Resolved(long customerId, String phone) {
            this.customerId = customerId;
            this.phone = safe(phone);
        }
    }
}
