#!/bin/bash
set -e

echo "=== Code Review Action ==="
echo "Model: ${CHAT_MODEL:-glm-4-flash}"
echo "Notification Channel: ${NOTIFICATION_CHANNEL:-feishu}"

# 获取提交信息
cd /github/workspace

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
if [ -n "$FEISHU_APP_ID" ]; then
    export API_KEY="${API_KEY:-$GLM_API_KEY}"
fi

# 运行代码审查
java -jar /app/openai-code-review-sdk.jar

echo "=== Code Review Complete ==="