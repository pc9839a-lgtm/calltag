from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

PKG = "kr.pagero.calltag"
OUT = Path("editor-proof")
OUT.mkdir(exist_ok=True)
def adb(*args):
    return subprocess.check_output(("adb", *args), stderr=subprocess.STDOUT, timeout=35)
def shell(*args):
    return adb("shell", *args).decode(errors="replace").strip()
def start(name):
    o = shell("am", "start", "-W", "-n", f"{PKG}/.{name}")
    assert "Error" not in o and "Exception" not in o, o
def retrieve(labels):
    for _ in range(5):
        try:
            shell("uiautomator", "dump", "/sdcard/editor.xml")
            xml = adb("exec-out", "cat", "/sdcard/editor.xml")
            nodes = {e.get("text"): e for e in ET.fromstring(xml).iter("node")}
            if all(x in nodes for x in labels):
                return xml, nodes
        except (ET.ParseError, subprocess.SubprocessError):
            pass
    (OUT / "failed.png").write_bytes(adb("exec-out", "screencap", "-p"))
    raise AssertionError(f"Missing {labels!r}; actual={list(locals().get('nodes', {}))}")
def bounds(node):
    return tuple(map(int, re.findall(r"\d+", node.get("bounds", ""))))
def tap(node):
    l,t,r,b = bounds(node)
    shell("input", "tap", str((l+r)//2), str((t+b)//2))
def proof(name, title, action):
    xml, nodes = retrieve([title, "취소", action])
    (OUT / f"{name}.xml").write_bytes(xml)
    (OUT / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))
    a, b = bounds(nodes["취소"]), bounds(nodes[action])
    scale = int(re.findall(r"\d+", shell("wm", "density"))[-1]) / 160
    horizontal = min(a[3], b[3]) > max(a[1], b[1])
    gap = max(b[0]-a[2], a[0]-b[2]) if horizontal else max(b[1]-a[3], a[1]-b[3])
    assert gap >= scale * (11 if horizontal else 1), (a,b)
    assert min(a[3]-a[1], b[3]-b[1]) >= scale*47, (a,b)
    print(f"{name}: {gap/scale:.1f} dp gap", flush=True)
    return nodes

adb("install", "app/build/outputs/apk/debug/app-debug.apk")
assert "versionName=0.44.70-qa" in shell("dumpsys", "package", PKG)
adb("root")
adb("wait-for-device")
start("CustomerAddActivity")
tap(retrieve(["직접 입력"])[1]["직접 입력"])
nodes = retrieve(["고객·업체명", "전화번호", "등록"])[1]
tap(nodes["고객·업체명"])
shell("input", "text", "QA-Customer")
tap(retrieve(["전화번호"])[1]["전화번호"])
shell("input", "text", "2025550143")
tap(retrieve(["등록"])[1]["등록"])
retrieve(["QA-Customer"])
shell("wm", "size", "920x1800")
shell("settings", "put", "system", "font_scale", "1.4")
start("HomeTaskEditorActivity")
tap(retrieve(["QA-Customer"])[1]["QA-Customer"])
tap(retrieve(["전화하기"])[1]["전화하기"])
tap(proof("01-task-date", "날짜 선택", "이 날짜로 선택")["이 날짜로 선택"])
tap(proof("02-task-time", "시간 선택", "등록")["취소"])
start("MessageAutomationSettingsActivity")
try:
    row = retrieve(["발송 조건·회선"])[1]["발송 조건·회선"]
except AssertionError:
    shell("input", "swipe", "400", "1440", "400", "520", "330")
    row = retrieve(["발송 조건·회선"])[1]["발송 조건·회선"]
tap(row)
tap(proof("03-sms-settings", "발송 조건·회선", "적용")["취소"])
