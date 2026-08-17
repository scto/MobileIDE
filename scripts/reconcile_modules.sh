#!/bin/sh
# scripts/reconcile_modules.sh
# MobileIDE Module, Asset & Settings Reconciliation Guard Script
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

ERRORS=0

echo "[reconcile_modules] Running Module, Asset & Settings Reconciliation Guard..."

# Check 1: TerminalEnvironmentSelector duplicate check
CONTAINERS=$(find app core features -name "TerminalEnvironmentSelector.kt" 2>/dev/null | wc -l)
if [ "$CONTAINERS" -gt 1 ]; then
    echo "[ERROR] Duplicate TerminalEnvironmentSelector.kt found in multiple modules!"
    find app core features -name "TerminalEnvironmentSelector.kt"
    ERRORS=$((ERRORS + 1))
else
    echo "[OK] Single TerminalEnvironmentSelector.kt owner verified."
fi

# Check 2: Terminal Assets single-holder check
ASSET_MODULES=$(find app core features -path "*/src/main/assets/terminal" -type d 2>/dev/null)
ASSET_COUNT=$(echo "$ASSET_MODULES" | grep -v '^$' | wc -l)
if [ "$ASSET_COUNT" -ne 1 ]; then
    echo "[ERROR] Multiple assets/terminal directories detected! Must be exactly 1."
    echo "$ASSET_MODULES"
    ERRORS=$((ERRORS + 1))
else
    echo "[OK] Single assets/terminal owner verified: $ASSET_MODULES"
fi

# Check 3: Hardcoded debian distro list check in SettingsScreen.kt
if grep -q 'listOf("ubuntu", "debian")' app/src/main/java/com/scto/mobile/ide/ui/settings/SettingsScreen.kt 2>/dev/null; then
    echo "[ERROR] Found legacy hardcoded debian distro list in SettingsScreen.kt!"
    ERRORS=$((ERRORS + 1))
else
    echo "[OK] Distro selection in SettingsScreen.kt uses clean enum source."
fi

# Check 4: Check for old deprecated app/src/main/assets/terminal path references in code
LEGACY_ASSET_REFS=$(grep -rn "app/src/main/assets/terminal" app/src/main core/ features/ 2>/dev/null | wc -l)
if [ "$LEGACY_ASSET_REFS" -gt 0 ]; then
    echo "[ERROR] Found $LEGACY_ASSET_REFS reference(s) to legacy app/src/main/assets/terminal path!"
    grep -rn "app/src/main/assets/terminal" app/src/main core/ features/
    ERRORS=$((ERRORS + 1))
else
    echo "[OK] No legacy app/src/main/assets/terminal path references in code."
fi

if [ "$ERRORS" -gt 0 ]; then
    echo "[FAIL] Reconciliation check failed with $ERRORS error(s)."
    exit 1
else
    echo "[SUCCESS] All module, asset and settings consolidation checks passed."
    exit 0
fi
