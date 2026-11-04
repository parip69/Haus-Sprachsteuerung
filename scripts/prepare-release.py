#!/usr/bin/env python3
"""Verify the signed APK and generate GitHub release assets. Never reads signing secrets."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
APK = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else ROOT / "app/build/outputs/apk/release/app-release.apk"
SDK = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
TOOLS = SDK / "build-tools/35.0.0"
EXPECTED = "c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564"

def run(*args):
    return subprocess.check_output([str(a) for a in args], text=True)

certs = run(TOOLS / "apksigner", "verify", "--print-certs", APK)
signers = re.findall(r"certificate SHA-256 digest: ([0-9a-f]+)", certs)
assert signers == [EXPECTED], "Unexpected signing certificate"
badging = run(TOOLS / "aapt", "dump", "badging", APK)
package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
assert package, "Missing APK package/version"
application_id, code, version = package.groups()
assert application_id == "de.parip69.homesprachsteuerung", "Wrong application ID"
gradle = (ROOT / "app/build.gradle.kts").read_text()
assert int(code) == int(re.search(r"versionCode = (\d+)", gradle)[1]), "Build is stale"
assert version == re.search(r'versionName = "([^"]+)"', gradle)[1], "Build is stale"
assert re.fullmatch(r"[A-Za-z0-9._-]+", version), "Unsafe version filename"
short_version = version.rsplit(".", 1)[-1]
name = f"Alfonso-{short_version}.apk"
out = ROOT / "app/build/outputs/github-release"
out.mkdir(parents=True, exist_ok=True)
shutil.copyfile(APK, out / name)
with (out / name).open("rb") as stream:
    digest = hashlib.file_digest(stream, "sha256").hexdigest()
metadata = {"applicationId": application_id, "versionCode": int(code), "versionName": version,
            "apkFileName": name, "sha256": digest, "channel": "stable",
            "size": (out / name).stat().st_size,
            "downloadUrl": f"https://raw.githubusercontent.com/parip69/Haus-Sprachsteuerung/main/downloads/{name}",
            "notes": (ROOT / f"docs/releases/{version}.md").read_text() if (ROOT / f"docs/releases/{version}.md").exists() else f"Alfonso {version}"}
(out / "alfonso-update.json").write_text(json.dumps(metadata, indent=2) + "\n")
print(f"Verified release assets: {out / name}\n{out / 'alfonso-update.json'}")
