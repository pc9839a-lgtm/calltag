from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

PACKAGE = "kr.pagero.calltag"
OUT = Path("registration-proof")
OUT.mkdir(exist_ok=True)
def adb(*args):
    return subprocess.check_output(("adb", *args), stderr=subprocess.STDOUT, timeout=35)
def shell(*args):
    return adb("shell", *args).decode(errors="replace").strip()
def read_screen():
    shell("uiautomator", "dump", "/sdcard/registration.xml")
    xml = adb("exec-out", "cat", "/sdcard/registration.xml")
    return xml, ET.fromstring(xml).findall(".//node")
def find_nodes(labels):
    for _ in range(5):
        try:
            xml, el = read_screen()
            nodes = {e.get("text"): e for e in el}
            if all(label in nodes for label in labels):
                return xml, nodes
        except (ET.ParseError, subprocess.SubprocessError):
            pass
    raise AssertionError(f"Missing {labels} on screen")
def bounds(node):
    return tuple(map(int,re.findall(r"\d+", node.get("bounds", ""))))
def tap(node):
    l,t,r,b = bounds(node)
    shell("input", "tap", str((l+r)//2), str((t+b)//2))
def show_dialog():
    shell("am", "force-stop", PACKAGE)
    output = shell("am", "start", "-W", "-n", f"{PACKAGE}/.CustomerAddActivity")
    assert "Error" not in output and "Exception" not in output, output
    _, nodes = find_nodes(["직접 입력"])
    tap(nodes["직접 입력"])
    return find_nodes(["고객 등록", "취소", "등록"])
def proof(name, check):
    xml, nodes = show_dialog()
    (OUT / f"{name}.xml").write_bytes(xml)
    (OUT / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))
    cancel, register = nodes["취소"], nodes["등록"]
    cl,ct,cr,cb = bounds(cancel)
    rl,rt,rr,rb = bounds(register)
    density = int(re.findall(r"\d+", shell("wm", "density"))[-1]) / 160
    horizontal = min(cb, rb) > max(ct, rt)
    gap = max(rl-cr, cl-rr) if horizontal else max(rt-cb, ct-rb)
    print(f"{name}: gap={gap/density:.1f}dp, cancel height={(cb-ct)/density:.1f}dp, register height={(rb-rt)/density:.1f}dp", flush=True)
    if check:
        assert gap >= density * (11 if horizontal else 7), (cancel.attrib, register.attrib)
        for node in (cancel, register):
            assert node.get("clickable") == "true", node.attrib
            left,top,right,bottom = bounds(node)
            assert bottom-top >= density*47 and right-left >= density*47, node.attrib
    return nodes

print(adb("root").decode(), flush=True)
adb("wait-for-device")
assert "uid=0" in shell("id")
adb("install", "previous-build/app-debug.apk")
proof("01-before", False)
adb("uninstall", PACKAGE)
adb("install", "app/build/outputs/apk/debug/app-debug.apk")
version = shell("dumpsys", "package", PACKAGE)
assert "versionName=0.44.69-qa" in version and "versionCode=2026111603" in version
nodes = proof("02-after", True)
tap(nodes["등록"])
_, nodes = find_nodes(["고객 등록", "등록", "취소"])
tap(nodes["취소"])
find_nodes(["직접 입력"])
shell("wm", "size", "800x1600")
shell("settings", "put", "system", "font_scale", "1.5")
proof("03-narrow-large-text", True)
