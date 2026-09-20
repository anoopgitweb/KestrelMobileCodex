package com.example.data.model

data class LlmModelRanking(
    val rank: Int,
    val modelName: String,
    val provider: String,
    val arenaElo: Int,
    val mmluScore: Double = 0.0,
    val codingScore: Double = 0.0,
    val contextWindowTokens: String,
    val costPerMillionInputTokens: Double = 0.0,
    val costPerMillionOutputTokens: Double = 0.0,
    val releaseDate: String,
    val strengths: String,
    val license: String
) {
    companion object {
        const val DATA_AS_OF = "September 20, 2026"
        const val SOURCE_NAME = "Artificial Analysis Intelligence Index v4.3.2"

        val TOP_LLM_LEADERBOARD = listOf(
            LlmModelRanking(1, "Claude Fable 5.1 (max with fallback)", "Anthropic", 53, contextWindowTokens = "1M", releaseDate = "2026", strengths = "Joint leader for current frontier intelligence and agentic knowledge work.", license = "Proprietary"),
            LlmModelRanking(2, "Claude Fable 5.1 (xhigh with fallback)", "Anthropic", 53, contextWindowTokens = "1M", releaseDate = "2026", strengths = "Top-tier adaptive reasoning with a very large context window.", license = "Proprietary"),
            LlmModelRanking(3, "GPT-6 Astra (max)", "OpenAI", 53, contextWindowTokens = "1.05M", releaseDate = "2026", strengths = "Joint leader with strong reasoning and efficient output-token use.", license = "Proprietary"),
            LlmModelRanking(4, "GPT-6 Astra (xhigh)", "OpenAI", 52, contextWindowTokens = "1.05M", releaseDate = "2026", strengths = "Frontier reasoning, coding, and long-context analysis.", license = "Proprietary"),
            LlmModelRanking(5, "Claude Opus 5 (max)", "Anthropic", 51, contextWindowTokens = "1M", releaseDate = "2026", strengths = "High-end reasoning, coding, agents, and computer-use performance.", license = "Proprietary"),
            LlmModelRanking(6, "Claude Fable 5 (with fallback)", "Anthropic", 50, contextWindowTokens = "1M", releaseDate = "2026", strengths = "Strong general reasoning and agentic task performance.", license = "Proprietary"),
            LlmModelRanking(7, "Muse Spark 1.3 (max)", "Meta", 48, contextWindowTokens = "Current", releaseDate = "2026", strengths = "High-ranking general intelligence model from Meta.", license = "Proprietary"),
            LlmModelRanking(8, "GPT-5.6 Sol (max)", "OpenAI", 47, contextWindowTokens = "Current", releaseDate = "2026", strengths = "Strong agentic and general-purpose intelligence with competitive efficiency.", license = "Proprietary"),
            LlmModelRanking(9, "GLM-5.3 (max)", "Z.AI", 45, contextWindowTokens = "Current", releaseDate = "2026", strengths = "Highest-ranked open-weights model in the current source snapshot.", license = "Open weights"),
            LlmModelRanking(10, "Kimi K3 (max)", "Moonshot AI", 44, contextWindowTokens = "Current", releaseDate = "2026", strengths = "Leading open-weights reasoning model with strong agentic capability.", license = "Open weights")
        )
    }
}
