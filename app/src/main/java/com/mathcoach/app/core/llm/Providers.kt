package com.mathcoach.app.core.llm

enum class Provider(
    val id: String,
    val label: String,
    val defaultBaseUrl: String,
    val defaultVisionModel: String,
    val defaultReasoningModel: String,
    val modelHint: String
) {
    MOONSHOT(
        id = "moonshot",
        label = "Moonshot",
        defaultBaseUrl = "https://api.moonshot.cn/v1",
        defaultVisionModel = "kimi-k2.6",
        defaultReasoningModel = "kimi-k2.6",
        modelHint = "推荐: kimi-k2.6, kimi-k2-0905-preview, moonshot-v1-8k"
    ),
    KIMI_CODING(
        id = "kimi_coding",
        label = "Kimi Coding",
        defaultBaseUrl = "https://api.kimi.com/coding/v1",
        defaultVisionModel = "kimi-coding-latest",
        defaultReasoningModel = "kimi-coding-latest",
        modelHint = "推荐: kimi-coding-latest, kimi-k2-0905-preview"
    ),
    DEEPSEEK(
        id = "deepseek",
        label = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultVisionModel = "deepseek-chat",
        defaultReasoningModel = "deepseek-chat",
        modelHint = "推荐: deepseek-v4-flash, deepseek-v4-pro"
    ),
    OPENAI(
        id = "openai",
        label = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultVisionModel = "gpt-4o",
        defaultReasoningModel = "gpt-4o",
        modelHint = "推荐: gpt-4o, gpt-4o-mini, gpt-4-turbo"
    ),
    QWEN(
        id = "qwen",
        label = "通义千问",
        defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        defaultVisionModel = "qwen-vl-max",
        defaultReasoningModel = "qwen-vl-max",
        modelHint = "推荐: qwen-vl-max(看图), qwen2.5-math-72b-instruct(数学), qwq-32b-preview(推理)"
    ),
    CUSTOM(
        id = "custom",
        label = "自定义",
        defaultBaseUrl = "",
        defaultVisionModel = "",
        defaultReasoningModel = "",
        modelHint = "请输入完整的模型名称"
    );

    companion object {
        fun fromId(id: String): Provider = entries.firstOrNull { it.id == id } ?: QWEN
    }
}
