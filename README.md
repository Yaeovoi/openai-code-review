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

## 快速开始

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