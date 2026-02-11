package zw.co.zivai.core_backend.dtos.users;

public final class PhoneNumber {
    private PhoneNumber() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("0")) {
            digits = "263" + digits.substring(1);
        }
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        return digits;
    }
}
