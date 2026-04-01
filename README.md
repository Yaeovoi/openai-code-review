# OpenAI Code Review

可插拔的 AI 代码审查工具，支持多模型、多通知渠道，一行配置即可接入。

## 功能特性

- **多 AI 模型支持** - 支持 Qwen、GLM、OpenAI、DeepSeek、Claude 等多种模型
- **多 API 协议** - 支持 OpenAI 协议和 Anthropic 协议
- **多通知渠道** - 支持飞书、钉钉、企业微信，可同时推送到多个渠道
- **一键接入** - 作为 GitHub Action 使用，只需一行配置
- **配置化设计** - 支持自定义审查规则、提示词
- **自动获取代码变更** - 通过 Git Diff 获取最新提交的代码差异
- **评审结果存档** - 将审查结果保存为 Markdown 文件并推送到指定 GitHub 仓库

## 支持的 AI 模型

| 模型 | 代码 | Provider | 说明 |
|------|------|----------|------|
| Qwen-Coder-Plus | `qwen-coder-plus` | qwen | 阿里云灵码，**代码专用，默认模型** |
| Qwen-Turbo | `qwen-turbo` | qwen | 阿里云灵码，速度快 |
| Qwen-Plus | `qwen-plus` | qwen | 阿里云灵码，均衡 |
| Qwen-Max | `qwen-max` | qwen | 阿里云灵码，最强 |
| GLM-4-Flash | `glm-4-flash` | glm | 阿里云百炼 GLM |
| GLM-4 | `glm-4` | glm | 阿里云百炼 GLM |
| GPT-4o | `gpt-4o` | openai | OpenAI |
| GPT-4o-mini | `gpt-4o-mini` | openai | OpenAI |
| GPT-4-turbo | `gpt-4-turbo` | openai | OpenAI |
| DeepSeek-Chat | `deepseek-chat` | deepseek | DeepSeek |
| DeepSeek-Coder | `deepseek-coder` | deepseek | DeepSeek |
| DeepSeek-Reasoner | `deepseek-reasoner` | deepseek | DeepSeek R1（含推理功能） |
| Claude-3-Opus | `claude-3-opus` | anthropic | Anthropic（需设置 API_PROTOCOL=anthropic） |
| Claude-3-Sonnet | `claude-3-sonnet` | anthropic | Anthropic（需设置 API_PROTOCOL=anthropic） |
| Claude-3-Haiku | `claude-3-haiku` | anthropic | Anthropic（需设置 API_PROTOCOL=anthropic） |

> **提示**：
> - 模型与 API 地址自动匹配，也可通过 `API_HOST` 自定义
> - 大多数模型服务兼容 **OpenAI 协议**，默认使用 `openai`
> - **Anthropic Claude** 不兼容 OpenAI 协议，需设置 `API_PROTOCOL=anthropic`

## 支持的通知渠道

- **飞书** - 卡片消息，支持长内容自动拆分
- **钉钉** - Markdown 消息
- **企业微信** - Markdown 消息

## 项目结构

```
openai-code-review
├── action/                           # GitHub Action
│   ├── action.yml                    # Action 配置
│   ├── Dockerfile                    # Docker 镜像
│   └── entrypoint.sh                 # 入口脚本
├── openai-code-review-sdk           # SDK 核心模块
│   └── src/main/java/
│       └── cn/Levionyx/middleware/sdk/
│           ├── CodeReviewRunner.java           # 主入口
│           ├── config/                         # 配置类
│           ├── domain/model/                   # 模型定义
│           └── infrastructure/
│               ├── notification/               # 通知渠道
│               ├── openai/                     # AI 模型
│               └── git/                        # Git 操作
└── .github/workflows/               # GitHub Actions 配置
```

## 快速开始

### 第一步：配置 GitHub Secrets

在目标项目的 **Settings → Secrets and variables → Actions** 中添加以下 secrets：

| Secret 名称 | 必填 | 说明 | 示例 |
|-------------|------|------|------|
| `CODE_REVIEW_LOG_URI` | 是 | 审查结果存储仓库地址 | `https://github.com/your-name/code-review-logs` |
| `CODE_TOKEN` | 是 | GitHub Token（需 repo 权限） | `ghp_xxx...` |
| `API_KEY` | 是 | AI 模型 API Key | `sk-xxx...` |
| `CHAT_MODEL` | 否 | AI 模型代码 | `qwen-coder-plus`（默认） |
| `API_HOST` | 否 | 自定义 API 地址 | `https://coding.dashscope.aliyuncs.com/v1` |
| `API_PROTOCOL` | 否 | API 协议 | `openai`（默认）或 `anthropic` |
| `API_TIMEOUT` | 否 | API 超时时间（秒） | `180`（默认 3 分钟） |
| `FEISHU_APP_ID` | 飞书必填 | 飞书应用 ID | `cli_xxx` |
| `FEISHU_APP_SECRET` | 飞书必填 | 飞书应用密钥 | `xxx` |
| `FEISHU_CHAT_ID` | 飞书必填 | 飞书群聊 ID | `oc_xxx` |

### 第二步：创建 Workflow 文件

在目标项目创建 `.github/workflows/code-review.yml`：

```yaml
name: Code Review

on:
  push:
    branches: [master, main]
  pull_request:
    branches: [master, main]

jobs:
  code-review:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 2

      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          distribution: 'adopt'
          java-version: '11'

      - name: Download Code Review SDK
        run: |
          mkdir -p ./libs
          wget -O ./libs/openai-code-review-sdk-1.0.jar https://github.com/Yaeovoi/openai-code-review/releases/download/V1.10/openai-code-review-sdk-1.0.jar

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
          # AI 模型配置
          API_KEY: ${{ secrets.API_KEY }}
          CHAT_MODEL: ${{ secrets.CHAT_MODEL }}
          API_HOST: ${{ secrets.API_HOST }}
          API_PROTOCOL: ${{ secrets.API_PROTOCOL }}
          API_TIMEOUT: ${{ secrets.API_TIMEOUT }}  # 可选，默认 180 秒
          # 飞书配置
          NOTIFICATION_CHANNEL: feishu
          FEISHU_APP_ID: ${{ secrets.FEISHU_APP_ID }}
          FEISHU_APP_SECRET: ${{ secrets.FEISHU_APP_SECRET }}
          FEISHU_CHAT_ID: ${{ secrets.FEISHU_CHAT_ID }}
```

### 第三步：推送代码测试

推送代码后，Action 会自动执行代码审查并将结果发送到飞书群聊。

---

## 使用不同通知渠道

### 钉钉通知

```yaml
env:
  NOTIFICATION_CHANNEL: dingtalk
  DINGTALK_WEBHOOK: ${{ secrets.DINGTALK_WEBHOOK }}
```

### 企业微信通知

```yaml
env:
  NOTIFICATION_CHANNEL: wecom
  WECOM_WEBHOOK: ${{ secrets.WECOM_WEBHOOK }}
```

---

## 前置准备

### 1. 创建审查结果存储仓库

创建一个 GitHub 仓库用于存储审查结果（如：`code-review-logs`）

### 2. 创建 GitHub Token

1. 访问 [GitHub Settings](https://github.com/settings/tokens)
2. 创建 Personal Access Token (Classic)，勾选 `repo` 权限
3. 将 Token 配置到 Secrets 的 `CODE_TOKEN`

### 3. 获取 AI 模型 API Key

| 平台 | 获取地址 |
|------|----------|
| 阿里云百炼（Qwen/GLM） | [百炼控制台](https://bailian.console.aliyun.com/) |
| OpenAI | [OpenAI Platform](https://platform.openai.com/) |
| DeepSeek | [DeepSeek Platform](https://platform.deepseek.com/) |
| Anthropic | [Anthropic Console](https://console.anthropic.com/) |

### 4. 配置飞书应用

1. 访问 [飞书开放平台](https://open.feishu.cn/app)
2. 创建企业自建应用
3. 开通权限：`im:message:send_as_bot`
4. 发布应用版本
5. 将应用添加到目标群聊
6. 获取 App ID、App Secret 和 Chat ID

## 飞书卡片效果

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
- **JGit** - Git 操作
- **多 AI 模型** - GLM / OpenAI / DeepSeek
- **多通知渠道** - 飞书 / 钉钉 / 企业微信
- **GitHub Actions** - CI/CD 集成

## 许可证

MIT License

## 变更日志

### V1.10 (2026-04-01)
- **可配置超时时间** - 新增 `API_TIMEOUT` 环境变量，用户可自定义 AI 模型 API 调用的超时时间（单位：秒，默认 180 秒）
- 解决之前需要反复修改代码增加超时时间的问题

### V1.9 (2026-04-01)
- 增加读取超时到 3 分钟，解决 AI 模型处理大量代码时的超时问题

### V1.8 (2026-04-01)
- 增加读取超时时间到 2 分钟，解决 AI 模型处理代码审查时的超时问题

### V1.7 (2026-04-01)
- Anthropic 继承 AbstractOpenAI 抽象基类，统一架构设计
- URL 解析使用 URISyntaxException 安全处理非法字符
- 移除日志中的敏感请求体信息

### V1.6 (2026-04-01)

**架构优化**
- 新增 `AbstractOpenAI` 抽象基类，消除 GLM、Qwen、OpenAI、DeepSeek 等实现类的重复代码
- 重构 `Anthropic` 类继承抽象基类，统一架构设计

**安全性增强**
- URL 解析使用 `URISyntaxException` 安全处理非法字符
- 移除日志中的敏感请求体信息，防止信息泄露

**健壮性改进**
- 添加 HTTP 连接超时配置（连接 30s，读取 60s）
- 增强空值安全检查，防止 NPE
- API_HOST 未配置或解析失败时，输出警告日志并回退到默认地址

**代码清理**
- 合并 `Model.java` 到 `ChatModel.java`，消除重复枚举
- 清理未使用的方法
- 统一日志级别

**破坏性变更**
- 默认模型从 `deepseek-chat` 改为 `qwen-coder-plus`
- 删除 `Model.java` 枚举类（如外部有引用需迁移到 `ChatModel`）

### V1.5 (2026-04-01)
- 自动补全 API_HOST 路径，支持不完整的地址

### V1.4 (2026-04-01)
- 修复模型选择逻辑，支持用户自定义任意模型

### V1.3 (2026-04-01)
- 修复 Qwen API 地址问题

### V1.2 (2026-04-01)
- 支持 Anthropic 协议

### V1.1 (2026-04-01)
- 支持自定义模型和 API 地址

### V1.0
- 初始版本

...
