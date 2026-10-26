#!/usr/bin/env python3
"""Static Play SMS/Call Log policy safeguards (not a Google policy approval)."""
from pathlib import Path
P=Path("app/src/main/java/kr/pagero/calltag")
init=(P/"InitialPermissionActivity.java").read_text(encoding="utf-8")
settings=(P/"MessageAutomationSettingsActivity.java").read_text(encoding="utf-8")
store=(P/"MessageAutomationStore.java").read_text(encoding="utf-8")
manifest=Path("app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
gate=(P/"FeatureAccessGate.java").read_text(encoding="utf-8")
assert "postDelayed(this::startPermissionFlow" not in init
assert "requestButton.setOnClickListener(v -> startPermissionFlow());" in init
assert "READ_CALL_LOG" in init
assert "SEND_SMS" in init
assert 'title.setText("통화 후 고객관리 자동화")' in init
assert "if (!p.contains(KEY_ENABLED)) editor.putBoolean(KEY_ENABLED, false);" in store
assert "return prefs(context).getBoolean(KEY_ENABLED, false);" in store
assert "getBoolean(KEY_CONNECTED_ENABLED, false)" in store
assert "getBoolean(KEY_MISSED_ENABLED, false)" in store
assert "getBoolean(KEY_DELAYED_ENABLED, false)" in store
assert "boolean wantsAutomaticSms = enabled.isChecked()" in settings
assert "requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS)" in settings
assert "onRequestPermissionsResult(int requestCode" in settings
assert "saveApprovedSettings();" in settings
assert settings.index("if (wantsAutomaticSms && checkSelfPermission") < settings.index("saveApprovedSettings();")
assert "requestPermissions(" not in gate, "Navigation must not request SMS permission"
assert "SetupRequirements.hasSms(context)" not in gate
lead_settings=(P/"PageroLeadMessageSettings.java").read_text(encoding="utf-8")
lead_ui=(P/"PageroLeadMessageSettingsActivity.java").read_text(encoding="utf-8")
manual=(P/"ManualMessageActivity.java").read_text(encoding="utf-8")
assert "AccountDataScope.preferenceName(context, PREFS)" in lead_settings
assert "AuthSessionStore.hasSession(context)" in lead_settings
assert "return PageroLeadMessageSettings.enabled(context);" in store
assert lead_ui.index("if (enabled.isChecked() && checkSelfPermission") < lead_ui.index("PageroLeadMessageSettings.setEnabled")
assert "saveApprovedSettings(body, delay);" in lead_ui
assert "requestSmsPermission(true);" in manual
assert "else if (resumeSchedule)" in manual
assert "android.permission.READ_CALL_LOG" in manifest
assert "android.permission.SEND_SMS" in manifest
assert "android.permission.READ_SMS" not in manifest
assert "android.permission.WRITE_CALL_LOG" not in manifest
assert "android.permission.RECEIVE_SMS" not in manifest
print("CallTag explicit Call Log prompt, default-off automation, conditional SMS prompt: PASS")
