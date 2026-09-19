package com.example.data.model

enum class AppSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val sheetTabName: String
) {
    AI_NEWS(
        id = "ai_news",
        title = "AI News",
        subtitle = "Latest on AI, ML, OpenAI, Anthropic & Frontier Tech",
        sheetTabName = "AI News"
    ),
    WORK_ACCOUNTS(
        id = "work_accounts",
        title = "Work Accounts News",
        subtitle = "Updates & news tailored to your enterprise work accounts",
        sheetTabName = "Work Accounts"
    ),
    ADVISORY_FIRMS(
        id = "advisory_firms",
        title = "News from Advisory Firms",
        subtitle = "Analyst insights from Forrester, Everest, Gartner & McKinsey",
        sheetTabName = "Advisory Firms"
    ),
    FORTUNE_500(
        id = "fortune_500",
        title = "Fortune 500 Index",
        subtitle = "Live index metrics, market cap, earnings & leader rankings",
        sheetTabName = "Fortune 500"
    ),
    LLM_RANKINGS(
        id = "llm_rankings",
        title = "LLM Rankings",
        subtitle = "Elo scores, MMLU benchmarks, latency, cost & leaderboard",
        sheetTabName = "LLM Rankings"
    );

    companion object {
        fun fromId(id: String): AppSection = entries.find { it.id == id } ?: AI_NEWS
    }
}
