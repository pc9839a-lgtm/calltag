package kr.pagero.calltag;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SimProfileManager {
    public static final class Profile {
        public final int subscriptionId;
        public final int slotIndex;
        public final String carrierName;
        public final String phoneNumber;
        public final boolean embedded;

        Profile(int subscriptionId, int slotIndex, String carrierName,
                String phoneNumber, boolean embedded) {
            this.subscriptionId = subscriptionId;
            this.slotIndex = slotIndex;
            this.carrierName = carrierName;
            this.phoneNumber = phoneNumber;
            this.embedded = embedded;
        }

        public String label() {
            String type = embedded ? "eSIM" : "SIM";
            String carrier = carrierName == null || carrierName.trim().isEmpty()
                    ? "회선 " + (slotIndex + 1) : carrierName.trim();
            String number = mask(phoneNumber);
            return number.isEmpty() ? carrier + " · " + type : carrier + " · " + type + " · " + number;
        }
    }

    private SimProfileManager() {}

    public static List<Profile> activeProfiles(Context context) {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return Collections.emptyList();
        }
        SubscriptionManager manager = context.getSystemService(SubscriptionManager.class);
        if (manager == null) return Collections.emptyList();

        List<SubscriptionInfo> infos;
        try {
            infos = manager.getActiveSubscriptionInfoList();
        } catch (SecurityException | UnsupportedOperationException e) {
            return Collections.emptyList();
        }
        if (infos == null || infos.isEmpty()) return Collections.emptyList();

        List<Profile> result = new ArrayList<>();
        for (SubscriptionInfo info : infos) {
            if (info == null) continue;
            String number = "";
            try {
                number = info.getNumber();
            } catch (SecurityException ignored) {
            }
            boolean embedded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.isEmbedded();
            result.add(new Profile(
                    info.getSubscriptionId(),
                    info.getSimSlotIndex(),
                    info.getCarrierName() == null ? "" : info.getCarrierName().toString(),
                    number == null ? "" : number,
                    embedded));
        }
        return result;
    }

    public static int selectedOrDefault(Context context) {
        List<Profile> profiles = activeProfiles(context);
        int systemDefault = SubscriptionManager.getDefaultSmsSubscriptionId();
        if (SubscriptionManager.isValidSubscriptionId(systemDefault)) return systemDefault;
        return profiles.isEmpty()
                ? SubscriptionManager.INVALID_SUBSCRIPTION_ID
                : profiles.get(0).subscriptionId;
    }

    private static String mask(String raw) {
        String value = PhoneNumberNormalizer.normalize(raw);
        if (value.length() < 8) return "";
        int length = value.length();
        return value.substring(0, Math.min(3, length)) + "-****-" + value.substring(length - 4);
    }
}
