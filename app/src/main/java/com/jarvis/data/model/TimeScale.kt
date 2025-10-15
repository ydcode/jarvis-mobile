package com.jarvis.data.model

/**
 * 时间刻度级别
 * @param minutes 每个刻度代表的分钟数
 * @param hourHeight 每小时的显示高度（dp）
 * @param label 刻度名称
 */
enum class TimeScale(
    val minutes: Int,
    val hourHeight: Float,
    val label: String
) {
    HOUR(60, 60f, "1小时"),
    HALF_HOUR(30, 120f, "30分钟"),
    TEN_MINUTES(10, 360f, "10分钟"),
    ONE_MINUTE(1, 3600f, "1分钟");

    /**
     * 获取下一个更细的刻度
     */
    fun zoomIn(): TimeScale {
        return when (this) {
            HOUR -> HALF_HOUR
            HALF_HOUR -> TEN_MINUTES
            TEN_MINUTES -> ONE_MINUTE
            ONE_MINUTE -> ONE_MINUTE // 已经是最小刻度
        }
    }

    /**
     * 获取上一个更粗的刻度
     */
    fun zoomOut(): TimeScale {
        return when (this) {
            HOUR -> HOUR // 已经是最大刻度
            HALF_HOUR -> HOUR
            TEN_MINUTES -> HALF_HOUR
            ONE_MINUTE -> TEN_MINUTES
        }
    }

    /**
     * 判断是否可以继续放大
     */
    fun canZoomIn(): Boolean = this != ONE_MINUTE

    /**
     * 判断是否可以继续缩小
     */
    fun canZoomOut(): Boolean = this != HOUR

    /**
     * 获取时间标签的间隔（每隔多少分钟显示一个标签）
     */
    fun getLabelInterval(): Int {
        return when (this) {
            HOUR -> 60
            HALF_HOUR -> 30
            TEN_MINUTES -> 30
            ONE_MINUTE -> 10
        }
    }

    /**
     * 获取主刻度线的间隔（粗线）
     */
    fun getMajorLineInterval(): Int {
        return when (this) {
            HOUR -> 60
            HALF_HOUR -> 60
            TEN_MINUTES -> 30
            ONE_MINUTE -> 10
        }
    }

    /**
     * 获取次刻度线的间隔（细线）
     */
    fun getMinorLineInterval(): Int {
        return when (this) {
            HOUR -> 30
            HALF_HOUR -> 15
            TEN_MINUTES -> 10
            ONE_MINUTE -> 5
        }
    }
}
