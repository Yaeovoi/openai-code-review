package cn.Levionyx.middleware.sdk;

import cn.Levionyx.middleware.sdk.domain.service.impl.OpenAiCodeReviewService;
import cn.Levionyx.middleware.sdk.infrastructure.git.GitCommand;
import cn.Levionyx.middleware.sdk.infrastructure.openai.IOpenAI;
import cn.Levionyx.middleware.sdk.infrastructure.openai.impl.GLM;
import cn.Levionyx.middleware.sdk.infrastructure.rag.IRAGService;
import cn.Levionyx.middleware.sdk.infrastructure.rag.impl.RAGServiceImpl;
import cn.Levionyx.middleware.sdk.infrastructure.weixin.WeiXin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAiCodeReview {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiCodeReview.class);

    // 阿里云百炼 API 配置
    private static final String DEFAULT_GLM_API_HOST = "https://coding.dashscope.aliyuncs.com/v1/chat/completions";
    private static final String DEFAULT_GLM_MODEL = "glm-4-flash";

    public static void main(String[] args) throws Exception {
        // Git 操作配置
        GitCommand gitCommand = new GitCommand(
                getEnv("GITHUB_REVIEW_LOG_URI"),
                getEnv("GITHUB_TOKEN"),
                getEnv("COMMIT_PROJECT"),
                getEnv("COMMIT_BRANCH"),
                getEnv("COMMIT_AUTHOR"),
                getEnv("COMMIT_MESSAGE")
        );

        // 微信配置
        WeiXin weiXin = new WeiXin(
                getEnv("WEIXIN_APPID"),
                getEnv("WEIXIN_SECRET"),
                getEnv("WEIXIN_TOUSER"),
                getEnv("WEIXIN_TEMPLATE_ID")
        );

        // 阿里云百炼 GLM 配置
        String glmApiKey = getEnv("GLM_API_KEY");
        String glmApiHost = getEnvOrDefault("GLM_API_HOST", DEFAULT_GLM_API_HOST);
        String glmModel = getEnvOrDefault("GLM_MODEL", DEFAULT_GLM_MODEL);

        IOpenAI openAI = new GLM(glmApiHost, glmApiKey);
        IRAGService ragService = new RAGServiceImpl(openAI, glmModel);

        OpenAiCodeReviewService openAiCodeReviewService = new OpenAiCodeReviewService(gitCommand, weiXin, ragService);
        openAiCodeReviewService.exec();

        logger.info("glm-code-review done!");
    }

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (null == value || value.isEmpty()) {
            throw new RuntimeException("环境变量 " + key + " 未配置");
        }
        return value;
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (null == value || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}