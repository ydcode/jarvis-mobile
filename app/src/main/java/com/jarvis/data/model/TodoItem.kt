package com.jarvis.data.model

/**
 * Todo 数据模型
 * @param id 唯一标识符
 * @param title 标题
 * @param content 内容详情
 * @param isCompleted 是否完成
 * @param timestamp 时间戳
 * @param avatarUrl 头像 URL（保持类似聊天的视觉效果）
 */
data class TodoItem(
    val id: String,
    val title: String,
    val content: String,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val avatarUrl: String? = null
)

/**
 * 示例数据
 */
val sampleTodoItems = listOf(
    TodoItem(
        id = "1",
        title = "完成项目报告",
        content = "需要在周五前完成Q4的项目总结报告",
        isCompleted = false,
        timestamp = System.currentTimeMillis() - 3600000
    ),
    TodoItem(
        id = "2",
        title = "团队会议",
        content = "下午3点参加产品讨论会",
        isCompleted = false,
        timestamp = System.currentTimeMillis() - 7200000
    ),
    TodoItem(
        id = "3",
        title = "代码审查",
        content = "审查新功能的代码提交",
        isCompleted = true,
        timestamp = System.currentTimeMillis() - 86400000
    ),
    TodoItem(
        id = "4",
        title = "健身打卡",
        content = "今天的运动目标：跑步5公里",
        isCompleted = false,
        timestamp = System.currentTimeMillis() - 10800000
    ),
    TodoItem(
        id = "5",
        title = "学习新技术",
        content = "深入学习 Jetpack Compose 的高级特性",
        isCompleted = false,
        timestamp = System.currentTimeMillis() - 14400000
    )
)
