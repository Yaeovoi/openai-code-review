FROM openjdk:11-jdk-slim

# 安装 git
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件
COPY openai-code-review-sdk-1.0.jar /app/openai-code-review-sdk.jar

# 复制入口脚本
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# 设置入口点
ENTRYPOINT ["/entrypoint.sh"]