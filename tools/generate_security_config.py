#!/usr/bin/env python3
"""Generate public integrity constants; never store a keystore password in sources."""
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys

project = Path(__file__).resolve().parent.parent
signer = os.environ.get("RIN23_SIGNER_SHA256", "").replace(":", "").lower()
keystore = os.environ.get("RIN23_KEYSTORE")
if keystore:
    if not os.environ.get("RIN23_STORE_PASSWORD"):
        sys.exit("Set the owner's RIN23_KEYSTORE and RIN23_STORE_PASSWORD, or the owner's public RIN23_SIGNER_SHA256 for an unsigned build.")
    cert = subprocess.run([
        "keytool", "-exportcert", "-keystore", keystore,
        "-alias", os.environ.get("RIN23_KEY_ALIAS", "rin23"),
        "-storepass:env", "RIN23_STORE_PASSWORD",
    ], check=True, stdout=subprocess.PIPE).stdout
    actual_signer = hashlib.sha256(cert).hexdigest()
    if signer and signer != actual_signer:
        sys.exit("The configured signer does not match the supplied keystore.")
    signer = actual_signer
if not re.fullmatch(r"[0-9a-f]{64}", signer):
    sys.exit("RIN23_SIGNER_SHA256 must be the SHA-256 digest of the signing certificate.")
engine = hashlib.sha256((project / "app/src/main/jniLibs/arm64-v8a/librinengine.so").read_bytes()).hexdigest()
dest = project / "build/generated/com/rin23/app/SecurityConfig.java"
dest.parent.mkdir(parents=True, exist_ok=True)
dest.write_text(
    'package com.rin23.app;\nfinal class SecurityConfig {\n'
    f' static final String SIGNER_SHA256="{signer}";\n'
    f' static final String ENGINE_SHA256="{engine}";\n'
    '}\n', encoding="utf-8")
