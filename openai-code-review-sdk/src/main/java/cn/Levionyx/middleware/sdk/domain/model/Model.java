package cn.Levionyx.middleware.sdk.domain.model;

public enum Model {


    DEEPSEEK_V3("deepseek-chat","DeepSeek-V3模型，不包括思考功能"),
    DEEPSEEK_R1("deepseek-reasoner","DeepSeek-R1模型，包括思考和推理功能"),
    GLM_4("glm-4-flash","智谱GLM-4模型，通过阿里云百炼平台调用"),
    ;
    private final String code;
    private final String info;

    Model(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

}
