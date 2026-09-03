#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
SERVER_DIR="$PROJECT_DIR/server"
PC_DIR="$PROJECT_DIR/petlink-pc"
MOBILE_DIR="$PROJECT_DIR/petlink-mobile"
DELIVERY_DIR="$(cd -- "$PROJECT_DIR/.." && pwd)"

if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  candidates=(
    "${HOME:-}/.cache/petlink/jdk17-corretto/Contents/Home"
    "${HOME:-}/.cache/petlink-jdk17/runtime/amazon-corretto-17.jdk/Contents/Home"
    "/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
    "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home"
  )
  if [[ "$(uname -s)" == "Darwin" ]] && [[ -x "/usr/libexec/java_home" ]]; then
    java_home_17="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    if [[ -n "$java_home_17" ]]; then
      candidates+=("$java_home_17")
    fi
  fi
  for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate/bin/java" ]]; then
      JAVA_HOME="$candidate"
      break
    fi
  done
fi

if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  echo "未找到 JDK 17。请先设置 JAVA_HOME 后重试。" >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "未找到 python3。" >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "未找到 npm。" >&2
  exit 1
fi

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
export PETLINK_JWT_SECRET="${PETLINK_JWT_SECRET:-petlink-local-test-secret-2026-09-02-abcdefghijklmnop}"

echo "[1/5] 后端 Maven 全量测试"
(cd "$SERVER_DIR" && ./mvnw test)

echo "[2/5] 数据库结构与 M02～M08 静态门禁"
(cd "$SERVER_DIR" && for verifier in \
  verify_schema.py verify_m02_static.py verify_m03_static.py verify_m04_static.py \
  verify_m05_static.py verify_m06_static.py verify_m07_static.py verify_m08_static.py; do
  python3 "scripts/$verifier"
done)

echo "[3/5] PC 测试与生产构建"
(cd "$PC_DIR" && npm test && npm run build)

echo "[4/5] Mobile 测试与 H5 生产构建"
(cd "$MOBILE_DIR" && npm test && npm run build:h5)

echo "[5/5] 前端静态契约检查与交付包哈希"
(cd "$PROJECT_DIR" && python3 scripts/verify_frontend.py)
if [[ -f "$DELIVERY_DIR/PetLink-v1.0-Frozen-Chinese-Delivery-20260902.zip" \
   && -f "$DELIVERY_DIR/PetLink-v1.0-Frozen-Chinese-Delivery-20260902.zip.sha256" ]]; then
  if command -v shasum >/dev/null 2>&1; then
    (cd "$DELIVERY_DIR" && shasum -a 256 -c PetLink-v1.0-Frozen-Chinese-Delivery-20260902.zip.sha256)
  elif command -v sha256sum >/dev/null 2>&1; then
    (cd "$DELIVERY_DIR" && sha256sum -c PetLink-v1.0-Frozen-Chinese-Delivery-20260902.zip.sha256)
  else
    echo "未找到 shasum 或 sha256sum，无法核对交付包哈希。" >&2
    exit 1
  fi
else
  echo "未找到交付包哈希文件，跳过该项。"
fi

echo "全部本地自动化检查通过。真实 MySQL 冒烟、浏览器/真机和正式部署仍按验收清单执行。"
