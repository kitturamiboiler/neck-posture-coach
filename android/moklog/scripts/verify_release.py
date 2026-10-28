"""Inspect a real AAB and merged manifest; never infer store approval from a build."""
import argparse
import hashlib
import json
import struct
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument("bundle", type=Path)
parser.add_argument("--manifest", required=True, type=Path)
parser.add_argument("--output", type=Path)
args = parser.parse_args()
ns = "{http://schemas.android.com/apk/res/android}"
root = ET.parse(args.manifest).getroot()
application = root.find("application")
sdk = root.find("uses-sdk")
permissions = sorted(x.get(ns + "name") for x in root.findall("uses-permission"))
errors = []
if root.get("package") != "com.moklog.app":
    errors.append("Unexpected application ID")
if int(sdk.get(ns + "targetSdkVersion", "0")) < 36:
    errors.append("Target API must be at least 36")
if application.get(ns + "debuggable", "false") != "false":
    errors.append("Release must not be debuggable")
if application.get(ns + "allowBackup") != "false":
    errors.append("Local health records must not be automatically backed up")
for permission in permissions:
    if any(word in permission for word in ("EXTERNAL_STORAGE", "READ_MEDIA", "AD_ID", "LOCATION", "RECORD_AUDIO")):
        errors.append("Unnecessary sensitive permission: " + permission)
native = []
with zipfile.ZipFile(args.bundle) as archive:
    signed = any(x.upper().startswith("META-INF/") and x.upper().endswith((".RSA", ".EC", ".DSA"))
                 for x in archive.namelist())
    for name in archive.namelist():
        if not name.endswith(".so"):
            continue
        data = archive.read(name)
        if data[:4] != b"\x7fELF" or data[5] != 1:
            errors.append("Unsupported ELF: " + name)
            continue
        is64 = data[4] == 2
        offset = struct.unpack_from("<Q" if is64 else "<I", data, 32 if is64 else 28)[0]
        size, count = struct.unpack_from("<HH", data, 54 if is64 else 42)
        loads = []
        for i in range(count):
            header = offset + size * i
            if struct.unpack_from("<I", data, header)[0] == 1:
                loads.append(struct.unpack_from("<Q" if is64 else "<I", data, header + (48 if is64 else 28))[0])
        aligned = bool(loads) and min(loads) >= 16384
        native.append({"path": name, "bits": 64 if is64 else 32, "load_alignments": loads, "aligned_16kb": aligned})
        if is64 and not aligned:
            errors.append("64-bit native library lacks 16 KB alignment: " + name)
report = {
    "bundle": args.bundle.name,
    "sha256": hashlib.sha256(args.bundle.read_bytes()).hexdigest(),
    "application_id": root.get("package"),
    "target_sdk": sdk.get(ns + "targetSdkVersion"),
    "permissions": permissions,
    "signed": signed,
    "native_libraries": native,
    "technical_errors": errors,
    "technical_checks_passed": not errors,
    "submission_ready": False,
    "owner_steps": ["Verify operator/contact and public privacy-policy URL", "Create Play account and complete declarations",
                    "Configure owner-controlled upload signing key" if not signed else "Verify upload certificate in Play Console",
                    "Complete required closed testing and production-access review"],
    "limitations": ["Static ELF alignment is not a 16 KB runtime test",
                   "Signing, Play account, store review and health accuracy are not proven by a successful build"],
}
encoded = json.dumps(report, ensure_ascii=False, indent=2)
if args.output:
    args.output.write_text(encoded + "\n", encoding="utf-8")
print(encoded)
raise SystemExit(1 if errors else 0)
