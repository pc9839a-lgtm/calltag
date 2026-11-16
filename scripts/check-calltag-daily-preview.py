from pathlib import Path
import re, time
import subprocess
import xml.etree.ElementTree as ET

PKG = "kr.pagero.calltag"
OUT = Path("daily-proof")
OUT.mkdir(exist_ok=True)
def adb(*args):
    return subprocess.check_output(("adb", *args), stderr=subprocess.STDOUT, timeout=35)
def shell(*args):
    return adb("shell", *args).decode(errors="replace").strip()
def start(name):
    output = shell("am", "start", "-W", "-n", f"{PKG}/.{name}")
    print(output, flush=True)
    assert "Error" not in output and "Exception" not in output, output
def record(name, expected, absent=()):
    xml = b""
    ok = False
    for _ in range(5):
        try:
            shell("uiautomator", "dump", "/sdcard/calltag.xml")
            xml = adb("exec-out", "cat", "/sdcard/calltag.xml")
            els = ET.fromstring(xml).findall(".//node")
        except (ET.ParseError, subprocess.SubprocessError):
            continue
        texts = [e.get("text", "") for e in els] + [e.get("content-desc", "") for e in els]
        if all(any(x in t for t in texts) for x in expected):
            ok = True
            break
    (OUT / f"{name}.xml").write_bytes(xml)
    (OUT / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))
    assert ok, f"{name}: missing {expected!r}; actual={locals().get('texts')!r}"
    assert not any(x in t for x in absent for t in texts), texts
    print(f"{name}: verified {expected!r}", flush=True)
    return els
def tap(nodes, label):
    b = next(e.get("bounds") for e in nodes if e.get("text") == label)
    x1,y1,x2,y2 = map(int,re.findall(r"\d+", b))
    shell("input", "tap", str((x1+x2)//2), str((y1+y2)//2))

print(adb("install", "app/build/outputs/apk/debug/app-debug.apk").decode(), flush=True)
version = shell("dumpsys", "package", PKG)
assert "versionName=0.44.68-qa" in version and "versionCode=2026111602" in version
start("AuthGateActivity")
record("01-first-launch", ["Google로 계속하기", "이메일로 로그인"])
print(adb("root").decode(), flush=True)
adb("wait-for-device")
assert "uid=0" in shell("id")
start("MainActivity")
home = record("02-home", ["고객 추가", "오늘 할 일", "+ 할 일 추가", "더보기"],
              ["통화 감지", "고객 목록", "수신 전화 고객정보 표시"])
tap(home, "더보기")
more = record("03-settings", ["통화 후 자동문자", "전화 설정", "통화 감지", "발신자 정보"])
tap(more, "통화 후 자동문자")
try:
    record("04-auto-sms", ["자동문자 사용", "발송 시점", "부재중"],
           ["선택된 템플릿", "템플릿 선택", "켜 둔 조건에 해당할 때만"])
except AssertionError:
    start("MessageAutomationSettingsActivity")
    record("04-auto-sms", ["자동문자 사용", "발송 시점", "부재중"],
           ["선택된 템플릿", "템플릿 선택", "켜 둔 조건에 해당할 때만"])
