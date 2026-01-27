#!/system/bin/sh
# SukiTier Rescue & Rollback Sentry
# Emergency failsafe mechanism for tier verification failures

set -u

# Configuration
TIER="${1:-TIER1_CORE}"
SNAPSHOT="${2:-}"
ROLLBACK="${3:-false}"
SUKISYSTEM_DIR="/data/susystem"
LOG_FILE="${SUKISYSTEM_DIR}/logs/rescue.log"
STATE_FILE="${SUKISYSTEM_DIR}/state.bin"
MOUNT_BASE="/mnt/sumodules"

# Logging function
log_event() {
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    echo "[$timestamp] $1" >> "${LOG_FILE}"
}

# Initialize logging
mkdir -p "$(dirname "${LOG_FILE}")"
log_event "=== RESCUE INITIATED ==="
log_event "Tier: $TIER | Snapshot: $SNAPSHOT | Rollback: $ROLLBACK"

# Kill all mount operations
kill_mount_operations() {
    log_event "Killing mount operations..."
    
    pkill -f "mount" 2>/dev/null || true
    pkill -f "fusermount" 2>/dev/null || true
}

# Unmount all SukiTier modules (reverse order)
unmount_all_modules() {
    log_event "Unmounting all modules..."
    
    if [ -d "${MOUNT_BASE}" ]; then
        # Unmount in reverse tier order
        for tier_dir in "${MOUNT_BASE}"/tier*; do
            if [ -d "${tier_dir}" ]; then
                for module_mount in "${tier_dir}"/*; do
                    if mountpoint -q "${module_mount}" 2>/dev/null; then
                        log_event "Unmounting: $module_mount"
                        umount -l "${module_mount}" 2>/dev/null || umount -f "${module_mount}" 2>/dev/null || true
                    fi
                done
            fi
        done
    fi
}

# Restore from snapshot
restore_snapshot() {
    local snapshot_path="${SUKISYSTEM_DIR}/snapshots/${SNAPSHOT}"
    
    if [ -z "${SNAPSHOT}" ] || [ ! -d "${snapshot_path}" ]; then
        log_event "ERROR: Invalid snapshot: $SNAPSHOT"
        return 1
    fi
    
    log_event "Restoring from snapshot: $SNAPSHOT"
    
    # Restore state
    if [ -f "${snapshot_path}/state.bin" ]; then
        cp "${snapshot_path}/state.bin" "${STATE_FILE}" 2>/dev/null || {
            log_event "ERROR: Failed to restore state"
            return 1
        }
    fi
    
    # Restore module manifests
    if [ -f "${snapshot_path}/modules.json" ]; then
        cp "${snapshot_path}/modules.json" "${SUKISYSTEM_DIR}/modules.json" 2>/dev/null || true
    fi
    
    log_event "Snapshot restore completed"
    return 0
}

# Verify system integrity post-rollback
verify_post_rollback() {
    log_event "Verifying post-rollback integrity..."
    
    # Check if boot partition is readable
    if ! [ -r "/dev/block/by-name/boot" ] && ! [ -r "/dev/block/mmcblk0p2" ]; then
        log_event "WARNING: Cannot verify boot partition"
    fi
    
    # Check if system partition is mounted
    if ! mountpoint -q "/system" && ! mountpoint -q "/system_root"; then
        log_event "ERROR: System partition not mounted"
        return 1
    fi
    
    log_event "Post-rollback verification passed"
    return 0
}

# Mark tier as requiring manual review
mark_tier_manual_review() {
    local review_file="${SUKISYSTEM_DIR}/pending_review.txt"
    mkdir -p "$(dirname "${review_file}")"
    echo "$TIER rollback at $(date)" >> "${review_file}"
    log_event "Marked tier $TIER for manual review"
}

# Main execution
main() {
    log_event "Starting rescue sequence..."
    
    # Step 1: Kill ongoing operations
    kill_mount_operations
    sleep 1
    
    # Step 2: Unmount all modules
    unmount_all_modules
    sleep 1
    
    # Step 3: Rollback if requested
    if [ "${ROLLBACK}" = "true" ] && [ -n "${SNAPSHOT}" ]; then
        if ! restore_snapshot; then
            log_event "ERROR: Snapshot restoration failed, attempting cold reset"
            # Cold reset - clear all tier state
            rm -f "${SUKISYSTEM_DIR}/state.bin" 2>/dev/null || true
        fi
    fi
    
    # Step 4: Verify post-rollback
    if ! verify_post_rollback; then
        log_event "CRITICAL: Post-rollback verification failed"
        mark_tier_manual_review
        exit 1
    fi
    
    log_event "Rescue sequence completed successfully"
    log_event "System will boot with $TIER disabled"
    exit 0
}

# Run main
main
