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
assert "versionName=0.44.72-qa" in shell("dumpsys", "package", PKG)
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
screen("05-new-detail", "고객 공유", "고객 삭제")
shell("input", "keyevent", "4")
home = screen("06-home-after-registration", "고객 추가", "홈")
tap("통계", home)
stats = screen("07-stats", "통계", "홈")
assert node("홈", stats).get("selected") == "false"
shell("input", "keyevent", "4")
home = screen("08-home-after-back", "고객 추가", "홈")
assert node("홈", home).get("selected") == "true"
tap("고객", home)
customers = screen("09-customer-list", "QA Customer")
assert node("홈", customers).get("selected") == "false"
tap("QA Customer", customers)
quick = screen("10-quick-edit", "고객 수정", "고객 공유", "고객 삭제")
tap("고객 공유", quick)
screen("11-share-dialog", "QA Customer")
shell("input", "keyevent", "4")
tap("고객 삭제", screen("12-back-in-edit", "고객 수정", "고객 삭제"))
confirm = screen("13-delete-confirm", "고객 삭제", "취소", "삭제")
tap("취소", confirm)
tap("고객 삭제", screen("14-edit-after-cancel", "고객 수정", "QA Customer"))
confirm = screen("15-confirm-delete", "고객 삭제", "취소", "삭제")
tap("삭제", confirm)
customers = screen("16-after-deletion", "고객", "홈")
assert node("QA Customer", customers) is None
shell("input", "keyevent", "4")
home = screen("17-home-after-list-back", "고객 추가", "홈")
assert node("홈", home).get("selected") == "true"
