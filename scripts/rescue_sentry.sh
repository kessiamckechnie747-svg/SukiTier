#!/system/bin/sh
# File: scripts/rescue_sentry.sh
# Tier 0: Double Redundancy Core
# Hard-scifi industrial with fail-secure design

PATH=/sbin:/system/bin:/system/xbin:/vendor/bin
LOG_TAG="SukiSentry"
LOG_FILE="/data/suki_sentry.log"
LOCK_FILE="/dev/.suki_sentry.lock"

# ANSI color codes for industrial aesthetic
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Industrial logging with timestamp
log() {
    local level=$1
    shift
    local message="$*"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S.%3N')
    
    case $level in
        "CRITICAL") echo -e "${RED}[$timestamp] CRITICAL: $message${NC}" ;;
        "ERROR")    echo -e "${RED}[$timestamp] ERROR: $message${NC}" ;;
        "WARNING")  echo -e "${YELLOW}[$timestamp] WARNING: $message${NC}" ;;
        "INFO")     echo -e "${GREEN}[$timestamp] INFO: $message${NC}" ;;
        "DEBUG")    echo -e "${CYAN}[$timestamp] DEBUG: $message${NC}" ;;
        *)          echo -e "${BLUE}[$timestamp] $message${NC}" ;;
    esac
    
    echo "[$timestamp] [$level] $message" >> "$LOG_FILE"
}

# Idempotent lock acquisition
acquire_lock() {
    local max_retries=5
    local retry_delay=1
    
    for i in $(seq 1 $max_retries); do
        if mkdir "$LOCK_FILE" 2>/dev/null; then
            trap 'release_lock' EXIT INT TERM
            log "INFO" "Lock acquired (attempt $i)"
            return 0
        fi
        
        if [ $i -lt $max_retries ]; then
            log "WARNING" "Lock busy, retrying in ${retry_delay}s..."
            sleep $retry_delay
        fi
    done
    
    log "ERROR" "Failed to acquire lock after $max_retries attempts"
    return 1
}

# Lock release with cleanup
release_lock() {
    if [ -d "$LOCK_FILE" ]; then
        rmdir "$LOCK_FILE" 2>/dev/null
        log "DEBUG" "Lock released"
    fi
}

# Hardware key detection (Physical Override)
detect_hardware_override() {
    local timeout=3
    local start_time=$(date +%s)
    local override_detected=0
    
    log "INFO" "Scanning for hardware override (Volume Down)..."
    
    # In production, would check actual input device states
    # For simulation, we check environment variable
    if [ "$SUKI_HARDWARE_OVERRIDE" = "1" ]; then
        override_detected=1
    fi
    
    if [ $override_detected -eq 1 ]; then
        log "CRITICAL" "HARDWARE OVERRIDE ACTIVE: Manual valve engaged"
        echo "manual_override_active" > /proc/safety/state 2>/dev/null
        return 1
    fi
    
    return 0
}

# Boot completion monitor with timeout
monitor_boot_completion() {
    local timeout=120
    local check_interval=5
    local start_time=$(date +%s)
    
    log "INFO" "Boot monitor started (timeout: ${timeout}s)"
    
    while true; do
        local elapsed=$(($(date +%s) - start_time))
        
        if getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
            log "INFO" "Boot completed in ${elapsed}s"
            echo "boot_success" > /proc/safety/boot_state 2>/dev/null
            return 0
        fi
        
        if [ $elapsed -ge $timeout ]; then
            log "CRITICAL" "BOOT TIMEOUT after ${timeout}s - activating fail-secure"
            activate_fail_secure
            return 1
        fi
        
        if [ $((elapsed % 15)) -eq 0 ]; then
            log "WARNING" "Boot still in progress... (${elapsed}s elapsed)"
        fi
        
        sleep $check_interval
    done
}

# Fail-secure activation (Tier 0 emergency)
activate_fail_secure() {
    log "CRITICAL" "=== FAIL-SECURE PROTOCOL ACTIVATED ==="
    
    echo "fail_secure" > /proc/safety/state 2>/dev/null
    echo "boot_timeout" > /proc/safety/boot_state 2>/dev/null
    
    log "INFO" "Initiating emergency recovery reboot..."
    sleep 2
    
    # Atomic reboot command (if available)
    if command -v reboot >/dev/null 2>&1; then
        reboot recovery 2>/dev/null || true
    fi
}

# Atomic file operation wrapper
atomic_write() {
    local content="$1"
    local target_file="$2"
    local temp_file="${target_file}.tmp.$$"
    
    echo "$content" > "$temp_file"
    
    if mv -f "$temp_file" "$target_file"; then
        sync
        return 0
    else
        rm -f "$temp_file" 2>/dev/null
        return 1
    fi
}

# Main execution flow
main() {
    if ! acquire_lock; then
        exit 1
    fi
    
    log "INFO" "=== SUKI SENTRY TIER 0 INITIALIZED ==="
    log "INFO" "System: $(uname -a)"
    
    # Phase 1: Hardware override check (highest priority)
    if ! detect_hardware_override; then
        log "ERROR" "Hardware override detected - aborting sentry operation"
        release_lock
        exit 1
    fi
    
    # Phase 2: Safety proc interface setup
    mkdir -p /proc/safety 2>/dev/null
    atomic_write "operational" /proc/safety/state
    atomic_write "monitoring" /proc/safety/boot_state
    
    # Phase 3: Start boot monitor
    monitor_boot_completion
    local monitor_result=$?
    
    release_lock
    
    if [ $monitor_result -eq 0 ]; then
        log "INFO" "Sentry monitoring complete - SUCCESS"
        exit 0
    else
        log "ERROR" "Sentry monitoring failed"
        exit 1
    fi
}

# Dry-run mode for testing
if [ "$1" = "--dry-run" ]; then
    timeout=${2:-30}
    log "INFO" "DRY RUN MODE - Simulating boot for ${timeout}s"
    
    sleep $timeout
    log "INFO" "Dry run complete"
    exit 0
fi

# Execute main function
main "$@"
