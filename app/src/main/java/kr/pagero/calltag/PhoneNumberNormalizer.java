package kr.pagero.calltag;

public final class PhoneNumberNormalizer {
    private PhoneNumberNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null) return "";

        String value = raw.trim().replaceAll("[^0-9+]", "");
        if (value.startsWith("+82")) {
            value = "0" + value.substring(3);
        } else if (value.startsWith("0082")) {
            value = "0" + value.substring(4);
        }
        return value.replaceAll("[^0-9]", "");
    }
}
