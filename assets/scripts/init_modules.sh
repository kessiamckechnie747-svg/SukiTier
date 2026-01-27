#!/bin/bash
# SukiTier Module Installation Helper
# Installs and configures module directory structure

SUKISYSTEM_DIR="/data/susystem"
MODULES_DIR="${SUKISYSTEM_DIR}/modules"
PATCHES_DIR="${SUKISYSTEM_DIR}/patches"
SNAPSHOTS_DIR="${SUKISYSTEM_DIR}/snapshots"
LOGS_DIR="${SUKISYSTEM_DIR}/logs"

echo "[SukiTier] Initializing module directories..."

# Create directory structure
mkdir -p "${MODULES_DIR}/tier1"/{kernel-patch,boot-module}
mkdir -p "${MODULES_DIR}/tier2"/{selinux-patch,system-mod}
mkdir -p "${MODULES_DIR}/tier3"/{experimental-feat}
mkdir -p "${PATCHES_DIR}"/{tier1,tier2,tier3}
mkdir -p "${SNAPSHOTS_DIR}"
mkdir -p "${LOGS_DIR}"

# Set permissions
chmod 755 "${SUKISYSTEM_DIR}"
chmod 755 "${MODULES_DIR}"
chmod 755 "${PATCHES_DIR}"
chmod 755 "${SNAPSHOTS_DIR}"
chmod 755 "${LOGS_DIR}"

# Copy rescue script
mkdir -p "${SUKISYSTEM_DIR}/scripts"
cp "$(dirname "$0")/rescue_sentry.sh" "${SUKISYSTEM_DIR}/scripts/"
chmod 755 "${SUKISYSTEM_DIR}/scripts/rescue_sentry.sh"

# Initialize metadata files
cat > "${SUKISYSTEM_DIR}/module_manifest.json" <<EOF
{
  "version": "1.0.0",
  "device": "generic",
  "gki_version": "6.1",
  "android_version": "16",
  "tiers": {
    "tier1": {
      "name": "Core Foundation",
      "required": true,
      "modules": ["kernel-patch", "boot-module"]
    },
    "tier2": {
      "name": "System Patches",
      "required": false,
      "depends_on": ["tier1"],
      "modules": ["selinux-patch", "system-mod"]
    },
    "tier3": {
      "name": "Experimental Features",
      "required": false,
      "depends_on": ["tier1", "tier2"],
      "modules": ["experimental-feat"]
    }
  }
}
EOF

echo "[SukiTier] Module directories initialized successfully"
