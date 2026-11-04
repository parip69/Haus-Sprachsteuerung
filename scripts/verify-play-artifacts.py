#!/usr/bin/env python3
"""Verify release artifacts without installing, publishing or reading signing secrets."""
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
BUNDLETOOL, AAB, APK = (Path(arg).resolve() for arg in sys.argv[1:4])
SDK = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
TOOLS = SDK / "build-tools/35.0.0"
NS = "{http://schemas.android.com/apk/res/android}"
PACKAGE = "de.parip69.homesprachsteuerung"
CERT = "c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564"
gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
code = re.search(r"versionCode = (\d+)", gradle)[1]
version = re.search(r'versionName = "([^"]+)"', gradle)[1]


def run(*args):
    return subprocess.check_output([str(arg) for arg in args], text=True, encoding="utf-8")


run("java", "-jar", BUNDLETOOL, "validate", f"--bundle={AAB}")
manifest = ET.fromstring(run("java", "-jar", BUNDLETOOL, "dump", "manifest", f"--bundle={AAB}"))
assert manifest.get("package") == PACKAGE, "Wrong bundle package"
assert manifest.get(NS + "versionCode") == code, "Stale bundle versionCode"
assert manifest.get(NS + "versionName") == version, "Stale bundle versionName"
app = manifest.find("application")
assert app.get(NS + "debuggable", "false") == "false", "Debug bundle"
metadata = {item.get(NS + "name"): item for item in app.findall("meta-data")}
assert metadata["com.google.android.gms.car.application"].get(NS + "resource") == "@xml/automotive_app_desc"
assert metadata["androidx.car.app.minCarApiLevel"].get(NS + "value") == "6"
service = next(item for item in app.findall("service")
               if item.get(NS + "name") == PACKAGE + ".car.AlfonsoCarAppService")
assert service.get(NS + "exported") == "true"
assert any(any(a.get(NS + "name") == "androidx.car.app.CarAppService" for a in f.findall("action"))
           and any(c.get(NS + "name") == "androidx.car.app.category.IOT" for c in f.findall("category"))
           for f in service.findall("intent-filter")), "Missing CarAppService/IOT filter"
print(run("java", "--class-path", BUNDLETOOL, ROOT / "scripts/VerifyBundleSignature.java", AAB).strip())
apksigner = TOOLS / ("apksigner.bat" if os.name == "nt" else "apksigner")
certs = run(apksigner, "verify", "--print-certs", APK)
assert re.findall(r"certificate SHA-256 digest: ([0-9a-f]+)", certs) == [CERT], "Wrong APK certificate"
badging = run(TOOLS / ("aapt.exe" if os.name == "nt" else "aapt"), "dump", "badging", APK)
assert re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging).groups() == (PACKAGE, code, version)
assert "application-debuggable" not in badging, "Debug APK"
print(f"PASS: {PACKAGE}, versionName {version}, versionCode {code}; signed release APK/AAB; Auto manifest.")
for artifact in (AAB, APK):
    print(f"{artifact.name}: {artifact.stat().st_size} bytes; SHA-256 {hashlib.sha256(artifact.read_bytes()).hexdigest()}")
