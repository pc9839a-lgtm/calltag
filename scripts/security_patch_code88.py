from pathlib import Path


def replace(path, old, new, count=1):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f"missing pattern in {path}: {old[:100]!r}")
    p.write_text(text.replace(old, new, count))


replace("app/build.gradle", "versionCode 87", "versionCode 88")
replace("app/build.gradle", "versionName '0.44.9'", "versionName '0.44.10'")

manifest = Path("app/src/main/AndroidManifest.xml")
text = manifest.read_text()
text = text.replace('''    <!-- Transitional only: older builds created CallTag RawContacts. New code never requests
         this permission; it is retained so already-granted upgrades can remove those legacy rows. -->
    <uses-permission android:name="android.permission.WRITE_CONTACTS" />
''', '')
text = text.replace('''        android:allowBackup="false"
        android:label="@string/app_name"''', '''        android:allowBackup="false"
        android:usesCleartextTraffic="false"
        android:networkSecurityConfig="@xml/network_security_config"
        android:label="@string/app_name"''')
old_login = '''        <activity
            android:name=".LoginActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:scheme="calltag"
                    android:host="auth"
                    android:path="/google" />
            </intent-filter>
        </activity>'''
new_login = '''        <activity
            android:name=".LoginActivity"
            android:exported="false"
            android:launchMode="singleTop"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize" />
        <activity
            android:name=".GoogleAuthCallbackActivity"
            android:excludeFromRecents="true"
            android:exported="true"
            android:noHistory="true"
            android:theme="@style/Theme.CallTag">
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data
                    android:scheme="calltag"
                    android:host="auth"
                    android:path="/google" />
            </intent-filter>
        </activity>'''
if old_login not in text:
    raise SystemExit("LoginActivity manifest block changed")
text = text.replace(old_login, new_login, 1)
text = text.replace('''            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="calltag" android:host="referral" />
            </intent-filter>
''', '')
text = text.replace('''            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="https" android:host="pagero.kr" android:pathPrefix="/r/" />''', '''            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="https" android:host="pagero.kr" android:pathPrefix="/r/" />''')
text = text.replace('<receiver android:name=".BootReceiver" android:enabled="true" android:exported="true">', '<receiver android:name=".BootReceiver" android:enabled="true" android:exported="false">')
manifest.write_text(text)

Path("app/src/main/java/kr/pagero/calltag/GoogleAuthFlowStore.java").write_text(r'''package kr.pagero.calltag;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.SecureRandom;

/** One-shot marker proving that an OAuth callback follows a recent user-initiated login attempt. */
public final class GoogleAuthFlowStore {
    private static final String PREFS = "calltag_google_auth_flow";
    private static final String KEY_MARKER = "marker";
    private static final String KEY_STARTED_AT = "started_at";
    private static final long MAX_AGE_MS = 10L * 60L * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GoogleAuthFlowStore() {}

    public static void begin(Context context) {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        StringBuilder marker = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) marker.append(String.format("%02x", value & 0xff));
        prefs(context).edit()
                .putString(KEY_MARKER, marker.toString())
                .putLong(KEY_STARTED_AT, System.currentTimeMillis())
                .commit();
    }

    public static boolean consumeIfActive(Context context) {
        SharedPreferences prefs = prefs(context);
        String marker = prefs.getString(KEY_MARKER, "");
        long startedAt = prefs.getLong(KEY_STARTED_AT, 0L);
        prefs.edit().clear().commit();
        long age = System.currentTimeMillis() - startedAt;
        return marker != null && marker.length() == 48 && startedAt > 0L
                && age >= 0L && age <= MAX_AGE_MS;
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
''')

Path("app/src/main/java/kr/pagero/calltag/GoogleAuthCallbackActivity.java").write_text(r'''package kr.pagero.calltag;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/** Minimal exported OAuth trampoline. The actual login UI remains non-exported. */
public final class GoogleAuthCallbackActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent source = getIntent();
        Uri uri = source == null ? null : source.getData();
        if (!isTrustedShape(source, uri) || !GoogleAuthFlowStore.consumeIfActive(this)) {
            GoogleAuthFlowStore.clear(this);
            Toast.makeText(this, "Google 로그인 요청이 만료되었거나 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
            return;
        }
        startActivity(new Intent(this, LoginActivity.class)
                .setData(uri)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    private boolean isTrustedShape(Intent intent, Uri uri) {
        if (intent == null || uri == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return false;
        if (!"calltag".equalsIgnoreCase(uri.getScheme())
                || !"auth".equalsIgnoreCase(uri.getHost())
                || !"/google".equals(uri.getPath())
                || uri.getPort() != -1
                || uri.getUserInfo() != null
                || uri.getFragment() != null) return false;
        String error = clean(uri.getQueryParameter("error"));
        String message = clean(uri.getQueryParameter("message"));
        String ticket = clean(uri.getQueryParameter("ticket"));
        if (error.length() > 80 || message.length() > 200 || ticket.length() > 256) return false;
        return !error.isEmpty() || !ticket.isEmpty();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
''')

replace("app/src/main/java/kr/pagero/calltag/LoginActivity.java", '''        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AuthApiClient.googleLoginUrl())));
        } catch (RuntimeException error) {
            showNotice("Google 로그인 화면을 열지 못했습니다.", true);
        }''', '''        try {
            GoogleAuthFlowStore.begin(this);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AuthApiClient.googleLoginUrl())));
        } catch (RuntimeException error) {
            GoogleAuthFlowStore.clear(this);
            showNotice("Google 로그인 화면을 열지 못했습니다.", true);
        }''')

Path("app/src/main/java/kr/pagero/calltag/PendingReferralStore.java").write_text(r'''package kr.pagero.calltag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.util.Locale;

/** 로그인 전 HTTPS 추천 링크만 보관하고 로그인 후 서버 등록에 사용한다. */
public final class PendingReferralStore {
    private static final String PREFS = "calltag_pending_referral";
    private static final String KEY_CODE = "code";
    private static final String KEY_CAPTURED_AT = "captured_at";
    private static final long MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L;

    private PendingReferralStore() {}

    public static boolean capture(Context context, Intent intent) {
        if (context == null || intent == null) return false;
        String code = referralCode(intent.getData());
        intent.removeExtra("referralCode");
        if (code.isEmpty()) return false;
        prefs(context).edit()
                .putString(KEY_CODE, code)
                .putLong(KEY_CAPTURED_AT, System.currentTimeMillis())
                .apply();
        intent.setData(null);
        return true;
    }

    public static String peek(Context context) {
        SharedPreferences value = prefs(context);
        long capturedAt = value.getLong(KEY_CAPTURED_AT, 0L);
        long age = System.currentTimeMillis() - capturedAt;
        if (capturedAt <= 0L || age < 0L || age > MAX_AGE_MS) {
            clear(context);
            return "";
        }
        String code = normalize(value.getString(KEY_CODE, ""));
        if (code.isEmpty()) clear(context);
        return code;
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static String referralCode(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return "";
        String host = lower(uri.getHost());
        if (!("pagero.kr".equals(host) || "www.pagero.kr".equals(host))) return "";
        if (uri.getPort() != -1 || uri.getUserInfo() != null || uri.getFragment() != null) return "";
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (!path.startsWith("/r/") || path.length() <= 3 || path.indexOf('/', 3) >= 0) return "";
        return normalize(path.substring(3));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String cleaned = value.trim().toUpperCase(Locale.ROOT);
        return cleaned.matches("[A-Z0-9]{4,20}") ? cleaned : "";
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
''')

replace("app/src/main/java/kr/pagero/calltag/BootReceiver.java", '''    public void onReceive(Context context, Intent intent) {
        Context app = context.getApplicationContext();
        String action = intent == null ? "" : intent.getAction();
        boolean packageReplaced = Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);''', '''    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        Context app = context.getApplicationContext();
        boolean packageReplaced = Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);''')

replace("app/src/main/java/kr/pagero/calltag/CallerOverlayManager.java", '''                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,''', '''                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_SECURE,''')
replace("app/src/main/java/kr/pagero/calltag/CallerOverlayManager.java", '''            CrashTelemetryStore.record(app, "caller_overlay",
                    shown ? "shown" : "show_failed", "customer=" + customer.id);''', '''            CrashTelemetryStore.record(app, "caller_overlay",
                    shown ? "shown" : "show_failed", "");''')
replace("app/src/main/java/kr/pagero/calltag/CrashTelemetryStore.java", '''            String detail = error == null ? "unknown"
                    : error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());''', '''            String detail = error == null ? "unknown" : error.getClass().getSimpleName();''')

p = Path("app/src/main/java/kr/pagero/calltag/PageroLeadSyncManager.java")
s = p.read_text()
s = s.replace('Log.w(TAG, "PageRo sync API unavailable: " + error.status + "/" + error.code);', 'Log.w(TAG, "PageRo sync API unavailable");')
s = s.replace('Log.e(TAG, "PageRo lead sync failed", error);', 'Log.e(TAG, "PageRo lead sync failed: " + error.getClass().getSimpleName());')
s = s.replace('Log.w(TAG, "Unable to reject invalid PageRo lead " + lead.id, ackError);', 'Log.w(TAG, "Unable to reject invalid PageRo lead");')
p.write_text(s)

Path("app/src/main/res/xml/network_security_config.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
''')

p = Path(".github/workflows/calltag-hotfix-build.yml")
s = p.read_text()
s = s.replace("CallTag 0.44.9 dialog and small-screen regression", "CallTag 0.44.10 security and UI regression")
s = s.replace("Verify 0.44.9 contracts", "Verify 0.44.10 contracts")
s = s.replace("versionCode 87", "versionCode 88")
s = s.replace("versionName '0.44.9'", "versionName '0.44.10'")
s = s.replace("calltag-v0.44.9-code87-debug-apk", "calltag-v0.44.10-code88-debug-apk")
s = s.replace("calltag-v0.44.9-code87-instrumentation-results", "calltag-v0.44.10-code88-instrumentation-results")
security_contract = '''
          # Security hardening contract.
          grep -F 'android:usesCleartextTraffic="false"' app/src/main/AndroidManifest.xml
          grep -F 'android:networkSecurityConfig="@xml/network_security_config"' app/src/main/AndroidManifest.xml
          grep -F 'cleartextTrafficPermitted="false"' app/src/main/res/xml/network_security_config.xml
          assert_absent 'android.permission.WRITE_CONTACTS' app/src/main/AndroidManifest.xml
          grep -A2 -F 'android:name=".LoginActivity"' app/src/main/AndroidManifest.xml | grep -F 'android:exported="false"'
          grep -F 'android:name=".GoogleAuthCallbackActivity"' app/src/main/AndroidManifest.xml
          grep -F 'GoogleAuthFlowStore.begin(this)' app/src/main/java/kr/pagero/calltag/LoginActivity.java
          grep -F 'GoogleAuthFlowStore.consumeIfActive(this)' app/src/main/java/kr/pagero/calltag/GoogleAuthCallbackActivity.java
          grep -F 'android:name=".BootReceiver" android:enabled="true" android:exported="false"' app/src/main/AndroidManifest.xml
          grep -F 'Intent.ACTION_BOOT_COMPLETED.equals(action)' app/src/main/java/kr/pagero/calltag/BootReceiver.java
          grep -F 'WindowManager.LayoutParams.FLAG_SECURE' app/src/main/java/kr/pagero/calltag/CallerOverlayManager.java
          assert_absent 'getStringExtra("referralCode")' app/src/main/java/kr/pagero/calltag/PendingReferralStore.java
          assert_absent 'String.valueOf(error.getMessage())' app/src/main/java/kr/pagero/calltag/CrashTelemetryStore.java
          assert_absent 'FLAG_MUTABLE' app/src/main/java
          assert_absent 'addJavascriptInterface' app/src/main/java
          assert_absent 'HostnameVerifier' app/src/main/java
          assert_absent 'X509TrustManager' app/src/main/java
          assert_absent 'MODE_WORLD_' app/src/main/java
'''
needle = "          # Play-safe phone CRM contract.\n"
if "# Security hardening contract." not in s:
    s = s.replace(needle, security_contract + "\n" + needle, 1)
p.write_text(s)

Path(".github/workflows/calltag-security-hardening.yml").write_text('''name: CallTag Android security hardening

on:
  pull_request:
    branches:
      - agent/play-internal-v0430-run2
  workflow_dispatch:

permissions:
  contents: read

jobs:
  security-gate:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      - name: Verify Android security boundaries
        shell: bash
        run: |
          set -euo pipefail
          absent() { local pattern="$1"; shift; if grep -R -F -- "$pattern" "$@"; then echo "::error::Forbidden security pattern: $pattern"; exit 1; fi; }
          grep -F 'versionCode 88' app/build.gradle
          grep -F "versionName '0.44.10'" app/build.gradle
          grep -F 'android:usesCleartextTraffic="false"' app/src/main/AndroidManifest.xml
          grep -F 'android:networkSecurityConfig="@xml/network_security_config"' app/src/main/AndroidManifest.xml
          grep -F 'cleartextTrafficPermitted="false"' app/src/main/res/xml/network_security_config.xml
          absent 'android.permission.WRITE_CONTACTS' app/src/main/AndroidManifest.xml
          absent 'android.permission.WRITE_CALL_LOG' app/src/main/AndroidManifest.xml
          absent 'android.permission.USE_FULL_SCREEN_INTENT' app/src/main/AndroidManifest.xml
          grep -A2 -F 'android:name=".LoginActivity"' app/src/main/AndroidManifest.xml | grep -F 'android:exported="false"'
          grep -F 'android:name=".GoogleAuthCallbackActivity"' app/src/main/AndroidManifest.xml
          grep -F 'GoogleAuthFlowStore.begin(this)' app/src/main/java/kr/pagero/calltag/LoginActivity.java
          grep -F 'GoogleAuthFlowStore.consumeIfActive(this)' app/src/main/java/kr/pagero/calltag/GoogleAuthCallbackActivity.java
          grep -F 'android:name=".BootReceiver" android:enabled="true" android:exported="false"' app/src/main/AndroidManifest.xml
          grep -F 'WindowManager.LayoutParams.FLAG_SECURE' app/src/main/java/kr/pagero/calltag/CallerOverlayManager.java
          absent 'getStringExtra("referralCode")' app/src/main/java/kr/pagero/calltag/PendingReferralStore.java
          absent 'String.valueOf(error.getMessage())' app/src/main/java/kr/pagero/calltag/CrashTelemetryStore.java
          absent 'FLAG_MUTABLE' app/src/main/java
          absent 'addJavascriptInterface' app/src/main/java
          absent 'HostnameVerifier' app/src/main/java
          absent 'X509TrustManager' app/src/main/java
          absent 'MODE_WORLD_' app/src/main/java
          absent 'http://' app/src/main/java
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.13'
      - name: Compile security-hardened debug build
        run: gradle :app:assembleDebug --stacktrace
''')

Path("docs/SECURITY_HARDENING_V04410.md").write_text('''# CallTag v0.44.10 / code88 security hardening

보안 우선 릴리스. 신규 기능보다 Android 공격면 축소를 먼저 적용한다.

## 적용
- cleartext HTTP 전면 차단 (`usesCleartextTraffic=false`, network security config)
- 더 이상 사용하지 않는 `WRITE_CONTACTS` 권한 제거
- `LoginActivity` 비공개화; 외부 Google OAuth 진입은 최소 전용 trampoline Activity로 분리
- Google OAuth 콜백은 최근 사용자 시작 flow가 존재할 때만 1회 허용하며 URI 구조/길이를 검증
- 추천코드는 외부 Intent extra 및 `calltag://`/HTTP 입력을 거부하고 `https://pagero.kr/r/...` 계열만 엄격히 허용
- `BootReceiver` 비공개화 + BOOT_COMPLETED/MY_PACKAGE_REPLACED action allowlist
- 수신 고객정보 overlay에 `FLAG_SECURE` 적용해 일반 스크린샷/화면 캡처 방지
- crash telemetry에서 raw Throwable message 저장 제거
- PageRo 동기화 logcat에서 lead ID와 raw Throwable 출력 제거
- PendingIntent mutable 사용, WebView JS bridge, custom TrustManager/HostnameVerifier, world-readable storage를 CI 금지 항목으로 고정
- 기존 Android Keystore AES-GCM 로그인 세션 암호화와 `allowBackup=false` 유지

## 남은 경계
현재 Google OAuth 서버 복귀 방식은 `calltag://auth/google` custom scheme이다. code88은 비로그인 임의 콜백 주입과 exported UI 노출을 크게 줄이지만, 동일 custom scheme을 선점한 악성 앱이 사용자가 실제 로그인 중일 때 콜백을 가로채는 위험을 플랫폼 수준에서 완전히 제거하지는 못한다. 완전 제거는 Google Play 앱 서명 인증서 지문으로 `pagero.kr/.well-known/assetlinks.json`을 배포한 뒤 검증된 HTTPS App Link OAuth callback으로 전환해야 한다.
''')

for cleanup in [
    ".github/workflows/v04410-security-patch.yml",
    ".github/workflows/v04410-security-trigger.yml",
    "scripts/security_patch_code88.py",
]:
    p = Path(cleanup)
    if p.exists():
        p.unlink()
