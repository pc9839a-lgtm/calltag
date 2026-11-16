from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

PKG = "kr.pagero.calltag"
OUT = Path("ux-proof")
OUT.mkdir(exist_ok=True)
def adb(*args):
    return subprocess.check_output(("adb", *args), stderr=subprocess.STDOUT, timeout=70)
def shell(*args):
    return adb("shell", *args).decode(errors="replace").strip()
def start(name):
    result = shell("am", "start", "-W", "-n", f"{PKG}/.{name}")
    assert "Error" not in result and "Exception" not in result, result
def node(key, nodes):
    return next((n for n in nodes if n.get("text") == key or n.get("content-desc") == key),
                next((n for n in nodes if key in (n.get("text") or "")), None))
def screen(name, *labels):
    for _ in range(5):
        try:
            shell("uiautomator", "dump", "/sdcard/ux.xml")
            xml = adb("exec-out", "cat", "/sdcard/ux.xml")
            nodes = list(ET.fromstring(xml).iter("node"))
            if all(node(label, nodes) is not None for label in labels):
                break
        except (ET.ParseError, subprocess.SubprocessError):
            continue
    else:
        (OUT / f"{name}-failed.png").write_bytes(adb("exec-out", "screencap", "-p"))
        raise AssertionError(f"{name}: expected {labels}; actual={[n.get('text') for n in locals().get('nodes', [])]}")
    (OUT / f"{name}.xml").write_bytes(xml)
    (OUT / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))
    return nodes
def tap(label, nodes):
    target = node(label, nodes)
    assert target is not None, label
    l,t,r,b = map(int, re.findall(r"\d+", target.get("bounds", "")))
    shell("input", "tap", str((l+r)//2), str((t+b)//2))

adb("install", "app/build/outputs/apk/debug/app-debug.apk")
adb("install", "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk")
assert "versionName=0.44.71-qa" in shell("dumpsys", "package", PKG)
adb("root")
adb("wait-for-device")
for perm in ("READ_CONTACTS", "READ_PHONE_STATE", "READ_PHONE_NUMBERS", "READ_CALL_LOG", "POST_NOTIFICATIONS"):
    shell("pm", "grant", PKG, f"android.permission.{perm}")
shell("appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
shell("svc", "wifi", "disable")
shell("svc", "data", "disable")
instrument = shell("am", "instrument", "-w", "-e", "class",
                   f"{PKG}.RecentCallPreviewFixtureTest", f"{PKG}.test/androidx.test.runner.AndroidJUnitRunner")
assert "OK (1 test)" in instrument, instrument
print(instrument, flush=True)
start("MainActivity")
home = screen("01-home", "고객 추가", "홈")
assert node("홈", home).get("selected") == "true"
tap("고객 추가", home)
tap("최근 통화에서 선택", screen("02-customer-add", "최근 통화에서 선택"))
tap("QA Customer", screen("03-recent-calls", "최근 통화", "QA Customer"))
tap("등록", screen("04-prefilled", "QA Customer", "2025550143", "등록"))
details = screen("05-customer-actions", "고객 공유", "고객 삭제")
tap("고객 공유", details)
screen("06-share-dialog", "QA Customer")
shell("input", "keyevent", "4")
tap("고객 삭제", screen("07-customer", "고객 공유", "고객 삭제"))
delete = screen("08-delete-confirm", "고객 삭제", "취소", "삭제")
tap("취소", delete)
screen("09-cancelled", "QA Customer", "고객 삭제")
start("MessageAutomationSettingsActivity")
sms = screen("10-sms-simple", "언제 보낼까요?", "통화 후", "부재중", "며칠 뒤", "시간·회선 설정")
tap("며칠 뒤", sms)
follow = screen("11-follow-up-options", "문구 바꾸기", "발송일 바꾸기")
tap("발송일 바꾸기", follow)
screen("12-delay-choices", "1일 후", "3일 후")
