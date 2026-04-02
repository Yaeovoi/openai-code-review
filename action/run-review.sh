#!/bin/bash
set -e

# ============================================
# OpenAI Code Review - 运行脚本
# ============================================

CACHE_DIR="${HOME}/.cache/openai-code-review"
JAR_FILE="${CACHE_DIR}/openai-code-review-sdk.jar"
VERSION_FILE="${CACHE_DIR}/.version"
CHECKSUM_FILE="${CACHE_DIR}/.checksum"
JAR_URL="https://github.com/Yaeovoi/openai-code-review/releases/latest/download/openai-code-review-sdk.jar"
CHECKSUM_URL="https://github.com/Yaeovoi/openai-code-review/releases/latest/download/openai-code-review-sdk.jar.sha256"
RELEASE_API="https://api.github.com/repos/Yaeovoi/openai-code-review/releases/latest"

# 创建缓存目录
mkdir -p "${CACHE_DIR}"

# 获取最新版本号
get_latest_version() {
    curl -s --connect-timeout 10 "${RELEASE_API}" | grep '"tag_name"' | sed -E 's/.*"tag_name": *"([^"]+)".*/\1/'
}

# 下载文件（带进度显示）
download_file() {
    local url="$1"
    local output="$2"
    echo "下载: ${url}"
    curl -sL --connect-timeout 30 --max-time 300 -o "${output}" "${url}"
}

# 计算 SHA256
compute_sha256() {
    local file="$1"
    if command -v sha256sum &> /dev/null; then
        sha256sum "${file}" | cut -d' ' -f1
    else
        shasum -a 256 "${file}" | cut -d' ' -f1
    fi
}

# 验证 SHA256
verify_checksum() {
    local jar_file="$1"
    local expected_checksum="$2"
    local actual_checksum=$(compute_sha256 "${jar_file}")

    if [ "${actual_checksum}" != "${expected_checksum}" ]; then
        echo "错误：SHA256 校验失败！"
        echo "期望: ${expected_checksum}"
        echo "实际: ${actual_checksum}"
        return 1
    fi
    echo "SHA256 校验通过"
}

# 主逻辑：检查并下载 JAR
download_and_verify_jar() {
    local need_download=false
    local latest_tag=""

    # 检查缓存是否存在
    if [ ! -f "${JAR_FILE}" ]; then
        echo "缓存不存在，需要下载 JAR..."
        need_download=true
    else
        echo "发现缓存 JAR，检查是否有新版本..."

        latest_tag=$(get_latest_version)

        if [ -z "${latest_tag}" ]; then
            echo "警告：无法获取最新版本信息，使用缓存版本"
            return 0
        fi

        local cached_tag=""
        if [ -f "${VERSION_FILE}" ]; then
            cached_tag=$(cat "${VERSION_FILE}")
        fi

        echo "缓存版本: ${cached_tag:-未知}"
        echo "最新版本: ${latest_tag}"

        if [ "${cached_tag}" != "${latest_tag}" ]; then
            echo "发现新版本，需要更新..."
            need_download=true
        else
            echo "已是最新版本，使用缓存"
            return 0
        fi
    fi

    # 下载 JAR 和 checksum
    if [ "${need_download}" = true ]; then
        local tmp_jar="${JAR_FILE}.tmp"
        local tmp_checksum="${CHECKSUM_FILE}.tmp"

        # 下载 JAR
        download_file "${JAR_URL}" "${tmp_jar}"

        # 下载 SHA256 checksum 文件
        download_file "${CHECKSUM_URL}" "${tmp_checksum}"

        # 验证文件大小（> 1MB）
        local file_size=$(wc -c < "${tmp_jar}" 2>/dev/null || echo 0)
        if [ "${file_size}" -lt 1048576 ]; then
            echo "错误：下载的 JAR 文件无效 (大小: ${file_size} bytes)"
            rm -f "${tmp_jar}" "${tmp_checksum}"
            exit 1
        fi

        # SHA256 校验
        if [ -f "${tmp_checksum}" ]; then
            local expected_checksum=$(cat "${tmp_checksum}" | tr -d ' ')
            if ! verify_checksum "${tmp_jar}" "${expected_checksum}"; then
                rm -f "${tmp_jar}" "${tmp_checksum}"
                exit 1
            fi
            mv "${tmp_checksum}" "${CHECKSUM_FILE}"
        else
            echo "警告：未找到 checksum 文件，跳过校验（不推荐）"
        fi

        # 移动文件到最终位置
        mv "${tmp_jar}" "${JAR_FILE}"
        echo "${latest_tag}" > "${VERSION_FILE}"
        echo "JAR 下载成功，版本: ${latest_tag}"
    fi
}

# 下载并验证 JAR
download_and_verify_jar

echo ""
echo "=== Code Review Action ==="
echo "Model: ${CHAT_MODEL:-qwen-coder-plus}"
echo "Notification Channel: ${NOTIFICATION_CHANNEL:-feishu}"

# 获取提交信息
REPO_NAME="${GITHUB_REPOSITORY##*/}"
BRANCH_NAME="${GITHUB_REF#refs/heads/}"
COMMIT_AUTHOR=$(git log -1 --pretty=format:'%an <%ae>')
COMMIT_MESSAGE=$(git log -1 --pretty=format:'%s')

echo "Repository: ${REPO_NAME}"
echo "Branch: ${BRANCH_NAME}"
echo "Author: ${COMMIT_AUTHOR}"

# 设置环境变量
export COMMIT_PROJECT="${REPO_NAME}"
export COMMIT_BRANCH="${BRANCH_NAME}"
export COMMIT_AUTHOR="${COMMIT_AUTHOR}"
export COMMIT_MESSAGE="${COMMIT_MESSAGE}"

# 兼容旧的环境变量名称
if [ -z "$API_KEY" ] && [ -n "$GLM_API_KEY" ]; then
    export API_KEY="$GLM_API_KEY"
    echo "使用兼容模式: GLM_API_KEY -> API_KEY"
fi

# 运行代码审查
java -jar "${JAR_FILE}"

echo "=== Code Review Complete ==="