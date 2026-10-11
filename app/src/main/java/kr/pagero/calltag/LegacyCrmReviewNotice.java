package kr.pagero.calltag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

/** Visible disclosure when an old shared CRM cannot safely be attributed to this account. */
public final class LegacyCrmReviewNotice {
    private static final String PREFS = "calltag_legacy_crm_review_notice";
    private LegacyCrmReviewNotice() {}

    public static void showOnce(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        String ownerId = AuthSessionStore.ownerId(activity).trim();
        if (ownerId.isEmpty() || !AuthSessionStore.hasSession(activity)) return;
        String flag = AccountDataScope.fingerprint(ownerId);
        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(flag, false)) return;
        File legacy = activity.getDatabasePath("calltag.db");
        if (!legacy.exists()) return;
        long count = 0L;
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                legacy.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
             Cursor cursor = db.rawQuery("SELECT (SELECT COUNT(*) FROM customers) + "
                     + "(SELECT COUNT(*) FROM interactions) + "
                     + "(SELECT COUNT(*) FROM follow_up_tasks)", null)) {
            if (cursor.moveToFirst()) count = cursor.getLong(0);
        } catch (RuntimeException error) {
            // Unknown or corrupt history must not be silently discarded.
            count = 1L;
        }
        if (count <= 0) return;
        if (!prefs.edit().putBoolean(flag, true).commit()) return;
        new AlertDialog.Builder(activity, R.style.Theme_CallTag_Dialog)
                .setTitle("기존 고객 기록 확인이 필요합니다")
                .setMessage("업데이트 전 공용 고객 DB에 기록이 남아 있습니다. "
                        + "다른 계정에 잘못 노출되지 않도록 새 계정별 DB와 분리했습니다. "
                        + "기존 기록은 삭제되지 않았으며, 소유권 확인 전에는 자동으로 옮기지 않습니다. "
                        + "본인 소유의 기록인지 확인한 후 설정의 '백업 및 복원'에서 구버전 고객 기록을 복구할 수 있습니다.")
                .setPositiveButton("확인", null)
                .show();
    }
}
