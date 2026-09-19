package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Topic

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val name: String,
    val query: String,
    val isDefault: Boolean,
    val iconName: String,
    val orderIndex: Int,
    val createdAt: Long
) {
    fun toDomain(): Topic = Topic(
        id = id,
        name = name,
        query = query,
        isDefault = isDefault,
        iconName = iconName,
        orderIndex = orderIndex,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(topic: Topic): TopicEntity = TopicEntity(
            id = topic.id,
            name = topic.name,
            query = topic.query,
            isDefault = topic.isDefault,
            iconName = topic.iconName,
            orderIndex = topic.orderIndex,
            createdAt = topic.createdAt
        )
    }
}
