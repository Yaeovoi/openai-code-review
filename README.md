# OpenAI Code Review

基于阿里云百炼 GLM 模型的自动化代码审查工具，集成 GitHub Actions 实现持续集成的代码质量把控。

## 功能特性

- **自动获取代码变更** - 通过 Git Diff 获取最新提交的代码差异
- **GLM 模型代码评审** - 使用阿里云百炼 GLM-4 模型进行智能代码审查
- **飞书群聊消息通知** - 将审查结果以卡片形式推送到飞书群聊，支持长内容自动拆分
- **评审结果存档** - 将审查结果保存为 Markdown 文件并推送到指定 GitHub 仓库
- **GitHub Actions 集成** - 支持 CI/CD 自动触发代码审查

## 项目结构

```
openai-code-review
├── openai-code-review-sdk           # SDK 核心模块
│   └── src/main/java/
│       └── cn/Levionyx/middleware/sdk/
│           ├── OpenAiCodeReview.java           # 主入口
│           ├── domain/
│           │   ├── model/                      # 模型定义
│           │   └── service/                    # 业务服务
│           └── infrastructure/
│               ├── feishu/                     # 飞书消息推送
│               ├── git/                        # Git 操作
│               ├── openai/                     # AI 模型接口
│               └── rag/                        # 代码评审服务
├── openai-code-review-test         # 测试模块
└── .github/workflows/              # GitHub Actions 配置
```

## 工作流程

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Git Push      │ ──▶ │  GitHub Actions │ ──▶ │   获取 Diff     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   飞书通知      │ ◀── │  结果存档 GitHub │ ◀── │  GLM 代码评审   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## 环境变量配置

| 变量名 | 说明 | 获取方式 |
|--------|------|----------|
| `GITHUB_REVIEW_LOG_URI` | 审查结果存储仓库地址 | GitHub 仓库 URL |
| `GITHUB_TOKEN` | GitHub 访问令牌 | [GitHub Settings](https://github.com/settings/tokens) |
| `COMMIT_PROJECT` | 项目名称 | 自动获取 |
| `COMMIT_BRANCH` | 分支名称 | 自动获取 |
| `COMMIT_AUTHOR` | 提交作者 | 自动获取 |
| `COMMIT_MESSAGE` | 提交信息 | 自动获取 |
| `FEISHU_APP_ID` | 飞书应用 ID | [飞书开放平台](https://open.feishu.cn/app) |
| `FEISHU_APP_SECRET` | 飞书应用密钥 | [飞书开放平台](https://open.feishu.cn/app) |
| `FEISHU_CHAT_ID` | 飞书群聊 ID | 群设置中获取 |
| `GLM_API_KEY` | 阿里云百炼 API Key | [阿里云百炼](https://bailian.console.aliyun.com/) |
| `GLM_MODEL` | GLM 模型名称（可选） | 默认: glm-4-flash |

## 快速开始（本项目）

### 1. 克隆项目

```bash
git clone https://github.com/Yaeovoi/openai-code-review.git
cd openai-code-review
```

### 2. 配置 GitHub Secrets

在 GitHub 仓库的 `Settings` > `Secrets and variables` > `Actions` 中添加以下 secrets：

- `CODE_REVIEW_LOG_URI` - 审查结果存储仓库地址
- `CODE_TOKEN` - GitHub Token
- `FEISHU_APP_ID` - 飞书应用 ID
- `FEISHU_APP_SECRET` - 飞书应用密钥
- `FEISHU_CHAT_ID` - 飞书群聊 ID
- `GLM_API_KEY` - 阿里云百炼 API Key

### 3. 推送代码触发审查

```bash
git add .
git commit -m "your commit message"
git push origin master
```

## 在其他项目中集成

如果你想在自己的项目中引入代码审查功能，只需以下几步：

### 方式一：远程 JAR 方式（推荐）

#### 步骤 1：创建 Workflow 文件

在你的项目中创建 `.github/workflows/code-review.yml`：

```yaml
name: Code Review

on:
  push:
    branches:
      - master  # 修改为你的主分支名称
  pull_request:
    branches:
      - master

jobs:
  code-review:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v2
        with:
          fetch-depth: 2

      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          distribution: 'adopt'
          java-version: '11'

      - name: Create libs directory
        run: mkdir -p ./libs

      - name: Download Code Review SDK
        run: wget -O ./libs/openai-code-review-sdk-1.0.jar https://github.com/Yaeovoi/openai-code-review/releases/download/V1.0/openai-code-review-sdk-1.0.jar

      - name: Get commit info
        run: |
          echo "REPO_NAME=${GITHUB_REPOSITORY##*/}" >> $GITHUB_ENV
          echo "BRANCH_NAME=${GITHUB_REF#refs/heads/}" >> $GITHUB_ENV
          echo "COMMIT_AUTHOR=$(git log -1 --pretty=format:'%an <%ae>')" >> $GITHUB_ENV
          echo "COMMIT_MESSAGE=$(git log -1 --pretty=format:'%s')" >> $GITHUB_ENV

      - name: Run Code Review
        run: java -jar ./libs/openai-code-review-sdk-1.0.jar
        env:
          GITHUB_REVIEW_LOG_URI: ${{ secrets.CODE_REVIEW_LOG_URI }}
          GITHUB_TOKEN: ${{ secrets.CODE_TOKEN }}
          COMMIT_PROJECT: ${{ env.REPO_NAME }}
          COMMIT_BRANCH: ${{ env.BRANCH_NAME }}
          COMMIT_AUTHOR: ${{ env.COMMIT_AUTHOR }}
          COMMIT_MESSAGE: ${{ env.COMMIT_MESSAGE }}
          FEISHU_APP_ID: ${{ secrets.FEISHU_APP_ID }}
          FEISHU_APP_SECRET: ${{ secrets.FEISHU_APP_SECRET }}
          FEISHU_CHAT_ID: ${{ secrets.FEISHU_CHAT_ID }}
          GLM_API_KEY: ${{ secrets.GLM_API_KEY }}
```

#### 步骤 2：配置 Secrets

在你的项目 GitHub 仓库中配置以下 Secrets：

| Secret 名称 | 说明 |
|------------|------|
| `CODE_REVIEW_LOG_URI` | 审查结果存储仓库地址（如：`https://github.com/your-username/code-review-logs`） |
| `CODE_TOKEN` | GitHub Personal Access Token（需要 repo 权限） |
| `FEISHU_APP_ID` | 飞书应用 ID |
| `FEISHU_APP_SECRET` | 飞书应用密钥 |
| `FEISHU_CHAT_ID` | 飞书群聊 ID |
| `GLM_API_KEY` | 阿里云百炼 API Key |

#### 步骤 3：推送代码验证

```bash
git add .
git commit -m "test: add code review workflow"
git push origin master
```

### 方式二：Maven 本地构建方式

如果你希望从源码构建 SDK，可以使用以下 Workflow：

```yaml
name: Code Review (Maven Build)

on:
  push:
    branches:
      - master

jobs:
  code-review:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v2
        with:
          fetch-depth: 2

      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          distribution: 'adopt'
          java-version: '11'

      - name: Clone SDK and Build
        run: |
          git clone https://github.com/Yaeovoi/openai-code-review.git /tmp/code-review-sdk
          cd /tmp/code-review-sdk
          mvn clean install -DskipTests
          mkdir -p $GITHUB_WORKSPACE/libs
          cp openai-code-review-sdk/target/openai-code-review-sdk-1.0.jar $GITHUB_WORKSPACE/libs/

      - name: Get commit info
        run: |
          echo "REPO_NAME=${GITHUB_REPOSITORY##*/}" >> $GITHUB_ENV
          echo "BRANCH_NAME=${GITHUB_REF#refs/heads/}" >> $GITHUB_ENV
          echo "COMMIT_AUTHOR=$(git log -1 --pretty=format:'%an <%ae>')" >> $GITHUB_ENV
          echo "COMMIT_MESSAGE=$(git log -1 --pretty=format:'%s')" >> $GITHUB_ENV

      - name: Run Code Review
        run: java -jar ./libs/openai-code-review-sdk-1.0.jar
        env:
          GITHUB_REVIEW_LOG_URI: ${{ secrets.CODE_REVIEW_LOG_URI }}
          GITHUB_TOKEN: ${{ secrets.CODE_TOKEN }}
          COMMIT_PROJECT: ${{ env.REPO_NAME }}
          COMMIT_BRANCH: ${{ env.BRANCH_NAME }}
          COMMIT_AUTHOR: ${{ env.COMMIT_AUTHOR }}
          COMMIT_MESSAGE: ${{ env.COMMIT_MESSAGE }}
          FEISHU_APP_ID: ${{ secrets.FEISHU_APP_ID }}
          FEISHU_APP_SECRET: ${{ secrets.FEISHU_APP_SECRET }}
          FEISHU_CHAT_ID: ${{ secrets.FEISHU_CHAT_ID }}
          GLM_API_KEY: ${{ secrets.GLM_API_KEY }}
```

### 前置准备

#### 1. 创建飞书应用

1. 访问 [飞书开放平台](https://open.feishu.cn/app)
2. 创建企业自建应用
3. 开通以下权限：
   - `im:message:send_as_bot` - 以应用身份发消息
4. 获取 App ID 和 App Secret

#### 2. 获取飞书群聊 ID

1. 创建飞书群聊
2. 点击群设置 > 群机器人 > 添加机器人 > 选择你的应用
3. 通过 API 或群设置获取 chat_id

#### 3. 获取阿里云百炼 API Key

1. 访问 [阿里云百炼控制台](https://bailian.console.aliyun.com/)
2. 开通服务并创建 API Key

#### 4. 创建审查结果存储仓库

1. 创建一个 GitHub 仓库用于存储审查结果（如：`code-review-logs`）
2. 创建 GitHub Personal Access Token（需要 repo 权限）

## 飞书卡片效果

审查结果将以卡片形式推送到飞书群聊：

```
┌─────────────────────────────────────┐
│  代码审查通知                    🔵 │
├─────────────────────────────────────┤
│  **项目:** your-project              │
│  **分支:** master                    │
│  **作者:** developer                 │
│  ─────────────────────────────────── │
│  (完整的代码审查意见...)              │
│                                      │
│  [查看审查详情] 按钮                  │
└─────────────────────────────────────┘
```

## 技术栈

- **Java 11** - 核心开发语言
- **Spring Boot 2.7** - 应用框架
- **JGit** - Git 操作
- **阿里云百炼 GLM-4** - AI 代码评审
- **飞书开放平台** - 消息推送
- **GitHub Actions** - CI/CD 集成

## 许可证

MIT License