package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;

final class CustomerActions {
    private CustomerActions() {}

    static void share(Activity activity, Customer customer) {
        if (customer == null) return;
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, customer.displayName + "\n" + customer.primaryPhone);
        activity.startActivity(Intent.createChooser(send, "고객 공유"));
    }

    static void confirmDeletion(Activity activity, Customer customer, int requestCode) {
        if (customer == null) return;
        activity.startActivityForResult(new Intent(activity, CustomerDeleteActivity.class)
                .putExtra(CustomerDeleteActivity.EXTRA_CUSTOMER_ID, customer.id), requestCode);
    }
}
