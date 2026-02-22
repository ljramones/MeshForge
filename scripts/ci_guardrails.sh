#!/usr/bin/env bash

set -euo pipefail

echo "Boundary guardrails: scanning meshforge core imports..."

FORBIDDEN_REGEX='import[[:space:]]+org\.lwjgl|import[[:space:]]+org\.lwjgl\.vulkan|import[[:space:]]+org\.lwjgl\.util\.shaderc|import[[:space:]]+org\.lwjgl\.glfw'
TARGET_DIR="meshforge/src/main"

if [[ ! -d "${TARGET_DIR}" ]]; then
  echo "ERROR: target dir not found: ${TARGET_DIR}"
  exit 1
fi

if rg -n "${FORBIDDEN_REGEX}" "${TARGET_DIR}" > /tmp/meshforge_guardrail_hits.txt; then
  echo "ERROR: Forbidden renderer/shader imports detected in MeshForge core:"
  cat /tmp/meshforge_guardrail_hits.txt
  exit 1
fi

echo "PASS: MeshForge core remains renderer-agnostic (no forbidden imports found)."

