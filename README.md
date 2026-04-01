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

### 方式一：GitHub Action（推荐）

在你的项目中创建 `.github/workflows/code-review.yml`：

```yaml
name: Code Review

on:
  push:
    branches: [master]

jobs:
  code-review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 2

      - name: Code Review
        uses: Yaeovoi/openai-code-review@v1
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          review-log-uri: ${{ secrets.CODE_REVIEW_LOG_URI }}
          api-key: ${{ secrets.API_KEY }}
          chat-model: glm-4-flash
          notification-channel: feishu
          feishu-app-id: ${{ secrets.FEISHU_APP_ID }}
          feishu-app-secret: ${{ secrets.FEISHU_APP_SECRET }}
          feishu-chat-id: ${{ secrets.FEISHU_CHAT_ID }}
```

### 方式二：使用不同通知渠道

#### 钉钉通知

```yaml
- name: Code Review
  uses: Yaeovoi/openai-code-review@v1
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
    review-log-uri: ${{ secrets.CODE_REVIEW_LOG_URI }}
    api-key: ${{ secrets.API_KEY }}
    chat-model: deepseek-chat
    notification-channel: dingtalk
    dingtalk-webhook: ${{ secrets.DINGTALK_WEBHOOK }}
```

#### 企业微信通知

```yaml
- name: Code Review
  uses: Yaeovoi/openai-code-review@v1
  with:
    github-token: ${{ secrets.GITHUB_TOKEN }}
    review-log-uri: ${{ secrets.CODE_REVIEW_LOG_URI }}
    api-key: ${{ secrets.OPENAI_API_KEY }}
    chat-model: gpt-4o-mini
    notification-channel: wecom
    wecom-webhook: ${{ secrets.WECOM_WEBHOOK }}
```

### 方式三：Java SDK 集成

```java
// 使用构建器配置
CodeReviewConfig config = new CodeReviewConfigBuilder()
    .github("https://github.com/your/review-logs", "your-token")
    .commit("project", "master", "author", "message")
    .model(ChatModel.GPT_4O_MINI)
    .api("your-openai-api-key")
    .feishu("app-id", "app-secret", "chat-id")
    .customPrompt("你是一位资深 Java 开发专家...")
    .build();

// 运行审查
new CodeReviewRunner(config).run();
```

## 配置说明

### Action 输入参数

| 参数 | 必填 | 说明 |
|------|------|------|
| `github-token` | 是 | GitHub Personal Access Token |
| `review-log-uri` | 是 | 审查结果存储仓库地址 |
| `api-key` | 是 | AI 模型 API Key |
| `chat-model` | 否 | AI 模型，默认 `qwen-coder-plus` |
| `api-host` | 否 | 自定义 API 地址（可选） |
| `api-protocol` | 否 | API 协议：`openai` 或 `anthropic`，默认 `openai` |
| `notification-channel` | 否 | 通知渠道，默认 `feishu` |
| `feishu-app-id` | 飞书必填 | 飞书应用 ID |
| `feishu-app-secret` | 飞书必填 | 飞书应用密钥 |
| `feishu-chat-id` | 飞书必填 | 飞书群聊 ID |
| `dingtalk-webhook` | 钉钉必填 | 钉钉 Webhook |
| `wecom-webhook` | 企微必填 | 企业微信 Webhook |
| `custom-prompt` | 否 | 自定义审查提示词 |

### 环境变量配置

如果使用 JAR 包方式，需要配置以下环境变量：

| 变量名 | 必填 | 说明 |
|--------|------|------|
| `GITHUB_REVIEW_LOG_URI` | 是 | 审查结果存储仓库地址 |
| `GITHUB_TOKEN` | 是 | GitHub 访问令牌 |
| `API_KEY` | 是* | AI 模型 API Key |
| `GLM_API_KEY` | 是* | AI 模型 API Key（向后兼容，与 API_KEY 二选一） |
| `CHAT_MODEL` | 否 | AI 模型代码，默认 `qwen-coder-plus` |
| `API_HOST` | 否 | 自定义 API 地址（可选） |
| `API_PROTOCOL` | 否 | API 协议：`openai` 或 `anthropic`，默认 `openai` |
| `NOTIFICATION_CHANNEL` | 否 | 通知渠道，默认 `feishu` |
| `FEISHU_APP_ID` | 飞书必填 | 飞书应用 ID |
| `FEISHU_APP_SECRET` | 飞书必填 | 飞书应用密钥 |
| `FEISHU_CHAT_ID` | 飞书必填 | 飞书群聊 ID |
| `DINGTALK_WEBHOOK` | 钉钉必填 | 钉钉 Webhook |
| `WECOM_WEBHOOK` | 企微必填 | 企业微信 Webhook |

> *`API_KEY` 和 `GLM_API_KEY` 二选一，推荐使用 `API_KEY`

### GitHub Secrets 配置

在项目的 **Settings → Secrets and variables → Actions** 中添加以下 secrets：

| Secret 名称 | 必填 | 说明 | 示例 |
|-------------|------|------|------|
| `API_KEY` | 是 | AI 模型 API Key | `sk-xxx...` |
| `CHAT_MODEL` | 否 | AI 模型代码 | `qwen-coder-plus`、`glm-4-flash`、`gpt-4o`、`claude-3-opus` |
| `API_HOST` | 否 | 自定义 API 地址 | `https://your-custom-api.com/v1/chat/completions` |
| `API_PROTOCOL` | 否 | API 协议 | `openai`（默认）或 `anthropic` |

> **如何选择 API 协议？**
> - **OpenAI 协议**（默认）：适用于 Qwen、GLM、OpenAI、DeepSeek、智谱、月之暗面等大多数模型服务
> - **Anthropic 协议**：仅用于 Anthropic Claude 模型，需要设置 `API_PROTOCOL=anthropic`

> **提示**：
> - `CHAT_MODEL` 未配置时，默认使用 `qwen-coder-plus`
> - `API_HOST` 未配置时，根据模型 provider 自动选择对应的默认 API 地址
> - `API_PROTOCOL` 未配置时，默认使用 `openai` 协议

## 前置准备

### 1. 创建审查结果存储仓库

创建一个 GitHub 仓库用于存储审查结果（如：`code-review-logs`）

### 2. 创建 GitHub Token

1. 访问 [GitHub Settings](https://github.com/settings/tokens)
2. 创建 Personal Access Token，勾选 `repo` 权限

### 3. 获取 AI 模型 API Key

- **GLM**: [阿里云百炼控制台](https://bailian.console.aliyun.com/)
- **OpenAI**: [OpenAI Platform](https://platform.openai.com/)
- **DeepSeek**: [DeepSeek Platform](https://platform.deepseek.com/)

### 4. 配置通知渠道

#### 飞书
1. 访问 [飞书开放平台](https://open.feishu.cn/app)
2. 创建企业自建应用
3. 开通 `im:message:send_as_bot` 权限
4. 将应用添加到群聊

#### 钉钉
1. 在钉钉群聊中添加自定义机器人
2. 获取 Webhook 地址

#### 企业微信
1. 在企业微信群聊中添加机器人
2. 获取 Webhook 地址

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
