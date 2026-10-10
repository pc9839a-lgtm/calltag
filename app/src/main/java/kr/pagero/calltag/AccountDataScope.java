package kr.pagero.calltag;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Derive on-device SQLite filenames exclusively from an authenticated server owner ID. */
public final class AccountDataScope {
    private static final String[] DATABASES = {
            "calltag.db", "calltag_groups.db", "calltag_messages.db",
            "calltag_campaigns.db", "calltag_task_types.db", "calltag_pending.db"
    };
    private AccountDataScope() {}

    public static String requireOwner(Context context) {
        if (context == null || !AuthSessionStore.hasSession(context)) {
            throw new IllegalStateException("로그인 확인 전 고객 DB 접근을 차단했습니다.");
        }
        String owner = AuthSessionStore.ownerId(context).trim();
        if (owner.isEmpty()) {
            throw new IllegalStateException("서버 계정 ID 없이 고객 DB를 열 수 없습니다.");
        }
        return owner;
    }

    public static String name(Context context, String legacyName) {
        return nameForOwner(requireOwner(context), legacyName);
    }

    public static String nameForOwner(String ownerId, String legacyName) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("서버 계정 ID가 없습니다.");
        }
        if (legacyName == null || !legacyName.matches("[a-zA-Z0-9_-]+\\.db")) {
            throw new IllegalArgumentException("허용되지 않은 SQLite 파일 이름입니다.");
        }
        return legacyName.substring(0, legacyName.length() - 3)
                + "-owner-" + fingerprint(ownerId.trim()) + ".db";
    }

    public static String currentCrmName(Context context) { return name(context, "calltag.db"); }

    public static String preferenceName(Context context, String original) {
        if (original == null || !original.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException("잘못된 계정 설정 이름입니다.");
        }
        return original + "-owner-" + fingerprint(requireOwner(context));
    }

    public static void assertCurrent(Context context, String openedName, String legacyName) {
        if (!name(context, legacyName).equals(openedName)) {
            throw new IllegalStateException("계정 변경 후 이전 계정 DB 접근을 차단했습니다.");
        }
    }

    public static List<String> currentAccountDatabases(Context context) {
        String owner = requireOwner(context);
        List<String> names = new ArrayList<>();
        for (String db : DATABASES) names.add(nameForOwner(owner, db));
        return names;
    }

    public static String fingerprint(String ownerId) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("서버 계정 ID가 없습니다.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    ownerId.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                hex.append(String.format(Locale.ROOT, "%02x", digest[i] & 0xff));
            }
            return hex.toString();
        } catch (Exception error) {
            throw new IllegalStateException("계정 DB 식별자 생성에 실패했습니다.", error);
        }
    }
}
