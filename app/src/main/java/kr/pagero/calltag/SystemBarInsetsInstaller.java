package kr.pagero.calltag;

import android.app.Activity;
import android.os.Build;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.WeakHashMap;

/**
 * Android 15+ edge-to-edge 강제 적용에서도 앱 콘텐츠가 상태바·카메라 홀·내비게이션바에
 * 가려지지 않도록 모든 Activity의 content root에 system bar inset을 한 번만 적용한다.
 */
public final class SystemBarInsetsInstaller {
    private static final WeakHashMap<View, int[]> BASE_PADDING = new WeakHashMap<>();

    private SystemBarInsetsInstaller() {}

    public static void install(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        WindowCompat.enableEdgeToEdge(activity.getWindow());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.getWindow().setNavigationBarContrastEnforced(false);
            activity.getWindow().setStatusBarContrastEnforced(false);
        }
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);

        View content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        int[] base;
        synchronized (BASE_PADDING) {
            base = BASE_PADDING.get(content);
            if (base == null) {
                base = new int[]{content.getPaddingLeft(), content.getPaddingTop(),
                        content.getPaddingRight(), content.getPaddingBottom()};
                BASE_PADDING.put(content, base);
            }
        }
        final int[] original = base;
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            int mask = WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout();
            Insets bars = insets.getInsets(mask);
            view.setPadding(
                    original[0] + bars.left,
                    original[1] + bars.top,
                    original[2] + bars.right,
                    original[3] + bars.bottom);
            return new WindowInsetsCompat.Builder(insets)
                    .setInsets(mask, Insets.NONE)
                    .build();
        });
        ViewCompat.requestApplyInsets(content);
    }
}
