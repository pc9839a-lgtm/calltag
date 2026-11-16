from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

PKG = "kr.pagero.calltag"
OUT = Path("screen-proof")
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
    for _ in range(5):
        try:
            shell("uiautomator", "dump", "/sdcard/calltag.xml")
            xml = adb("exec-out", "cat", "/sdcard/calltag.xml")
            els = ET.fromstring(xml).findall(".//node")
        except (ET.ParseError, subprocess.SubprocessError):
            continue
        texts = [e.get("text", "") for e in els]
        if all(any(x in t for t in texts) for x in expected):
            break
    else:
        if "xml" in locals():
            (OUT / f"{name}.xml").write_bytes(xml)
        (OUT / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))
        raise AssertionError(f"{name}: missing {expected!r}; actual={locals().get('texts')!r}")
    (OUT / f"{name}.xml").write_bytes(xml)
    (OUT / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))
    assert not any(x in t for x in absent for t in texts), texts
    print(f"{name}: verified {expected!r}", flush=True)
    return els
def tap(nodes, label):
    b = next(e.get("bounds") for e in nodes if e.get("text") == label)
    x1,y1,x2,y2 = map(int,re.findall(r"\d+", b))
    shell("input", "tap", str((x1+x2)//2), str((y1+y2)//2))

print(adb("install", "build-under-test/app-debug.apk").decode(), flush=True)
version = shell("dumpsys", "package", PKG)
assert "versionName=0.44.67-qa" in version and "versionCode=2026111601" in version
start("AuthGateActivity")
welcome = record("01-first-launch", ["콜태그", "Google로 계속하기", "이메일로 로그인"],
                 ["회원가입", "비밀번호", "통화 후 고객관리를 더 빠르게"])
tap(welcome, "이메일로 로그인")
record("02-email", ["회원가입", "비밀번호", "비밀번호 재설정"],
       ["Google로 계속하기"])
print(adb("root").decode(), flush=True)
adb("wait-for-device")
assert "uid=0" in shell("id")
start("InitialPermissionActivity")
record("03-call-history", ["통화 후 기록", "통화기록(번호·시각·유형), 연락처", "권한 허용"],
       ["READ_CALL_LOG", "SEND_SMS", "통화 후 고객관리 자동화"])
for name in ("READ_CONTACTS", "READ_PHONE_STATE", "READ_PHONE_NUMBERS", "READ_CALL_LOG"):
    output = shell("pm", "grant", PKG, f"android.permission.{name}")
    assert "Exception" not in output and "Error" not in output, output
start("CallerIdSetupActivity")
record("04-popup", ["통화 후 팝업", "다른 앱 위에 표시", "설정 열기"], ["통화 전에 바로 확인"])
shell("appops", "set", PKG, "SYSTEM_ALERT_WINDOW", "allow")
shell("am", "force-stop", PKG)
start("CallerIdSetupActivity")
record("05-caller-id", ["발신자 정보 (선택)", "고객명과 최근 메모", "건너뛰기"],
       ["통화 전에 바로 확인"])
