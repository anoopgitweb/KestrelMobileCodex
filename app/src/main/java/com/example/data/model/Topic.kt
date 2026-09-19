package com.example.data.model

data class Topic(
    val id: String,
    val name: String,
    val query: String,
    val isDefault: Boolean = false,
    val iconName: String = "ai",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAllTopic: Boolean
        get() = id == ALL_TOPIC_ID

    companion object {
        const val ALL_TOPIC_ID = "all_topics"

        val ALL_TOPIC = Topic(
            id = ALL_TOPIC_ID,
            name = "All",
            query = "",
            isDefault = true,
            iconName = "all_inclusive",
            orderIndex = -1
        )

        val DEFAULT_TOPICS = listOf(
            Topic(
                id = "ai",
                name = "Artificial Intelligence",
                query = "Artificial Intelligence",
                isDefault = true,
                iconName = "psychology",
                orderIndex = 0
            ),
            Topic(
                id = "ml",
                name = "Machine Learning",
                query = "Machine Learning",
                isDefault = true,
                iconName = "hub",
                orderIndex = 1
            ),
            Topic(
                id = "openai",
                name = "OpenAI",
                query = "OpenAI",
                isDefault = true,
                iconName = "auto_awesome",
                orderIndex = 2
            ),
            Topic(
                id = "anthropic",
                name = "Anthropic",
                query = "Anthropic",
                isDefault = true,
                iconName = "smart_toy",
                orderIndex = 3
            )
        )
    }
}
