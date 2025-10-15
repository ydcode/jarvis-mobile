package com.jarvis.ui.components

/**
 * 时间线状态相关的数据结构和枚举
 */

/**
 * 时间尺度级别
 * 从细到粗排列
 */
enum class TimeScaleLevel(
    val displayName: String,
    val ratioToNext: Float  // 与下一个更粗刻度的比例
) {
    MINUTE("1分钟", 5f),            // 1分钟 : 5分钟 = 1:5
    FIVE_MINUTES("5分钟", 2f),      // 5分钟 : 10分钟 = 1:2
    TEN_MINUTES("10分钟", 3f),      // 10分钟 : 半小时 = 1:3
    HALF_HOUR("半小时", 2f),        // 半小时 : 小时 = 1:2
    HOUR("小时", 24f),              // 小时 : 天 = 1:24
    DAY("天", 7f),                  // 天 : 周 = 1:7
    WEEK("周", 4f),                 // 周 : 月 = 1:4 (近似)
    MONTH("月", 3f),                // 月 : 季度 = 1:3
    QUARTER("季度", 4f),            // 季度 : 年 = 1:4
    YEAR("年", 10f),                // 年 : 10年 = 1:10
    DECADE("10年", 10f),            // 10年 : 100年 = 1:10
    CENTURY("100年", 1f);           // 100年，最粗刻度

    /**
     * 获取下一个更细的刻度
     */
    fun getFinerScale(): TimeScaleLevel? {
        val prevOrdinal = ordinal - 1
        return if (prevOrdinal >= 0) entries[prevOrdinal] else null
    }

    /**
     * 获取下一个更粗的刻度
     */
    fun getCoarserScale(): TimeScaleLevel? {
        val nextOrdinal = ordinal + 1
        return if (nextOrdinal < entries.size) entries[nextOrdinal] else null
    }

    /**
     * 转换位置到相邻刻度
     * @param position 当前刻度的位置（单位数）
     * @param targetScale 目标刻度
     * @return 目标刻度的位置（单位数）
     */
    fun convertPositionTo(position: Float, targetScale: TimeScaleLevel): Float {
        return when {
            // 放大到更细刻度
            targetScale == this.getFinerScale() -> {
                when (this) {
                    // 简单比例刻度（原点都是0）
                    MINUTE -> position * 5f  // 1分钟 -> 5分钟：1单位 = 5单位
                    FIVE_MINUTES -> position * 2f  // 5分钟 -> 10分钟
                    TEN_MINUTES -> position * 3f  // 10分钟 -> 半小时
                    HALF_HOUR -> position * 2f  // 半小时 -> 小时
                    HOUR -> position * 24f  // 小时 -> 天
                    DAY -> position * 7f  // 天 -> 周
                    WEEK -> position * 4f  // 周 -> 月（近似）
                    MONTH -> position * 3f  // 月 -> 季度

                    // 特殊处理：季度->年（原点变化：0->10）
                    // 年i=10是当前年，季度i=0是当前季度
                    // 因此：(yearPos - 10) * 4 可以得到季度位置
                    QUARTER -> (position - 10f) * 4f  // 年 -> 季度

                    // 特殊处理：年 -> 10年
                    // 年刻度: i=10是当前年(2025)
                    // 10年刻度: i=10是当前10年（2020-2029）的起始位置
                    // position=10(2025年) 应该对应到 10年刻度的 i=10.5（2025在2020-2029的中间）
                    // position=11(2026年) 应该对应到 i=10.6
                    // 公式：10 + (position - 10) / 10
                    YEAR -> 10f + (position - 10f) / 10f  // 年 -> 10年

                    // 特殊处理：10年 -> 100年
                    // 10年刻度: i=10是当前10年(2020-2029)
                    // 100年刻度: i=10是当前100年(2000-2099)的起始
                    // position=10(2020-2029) 应该对应到 100年刻度的 i=10.2（2020在2000-2099的前面）
                    DECADE -> 10f + (position - 10f) / 10f  // 10年 -> 100年
                    CENTURY -> position  // 已经是最粗刻度
                }
            }
            // 缩小到更粗刻度
            targetScale == this.getCoarserScale() -> {
                when (this) {
                    // 简单比例刻度（原点都是0）
                    MINUTE -> position / 5f  // 1分钟 -> 5分钟
                    FIVE_MINUTES -> position / 2f  // 5分钟 -> 10分钟
                    TEN_MINUTES -> position / 3f  // 10分钟 -> 半小时
                    HALF_HOUR -> position / 2f  // 半小时 -> 小时
                    HOUR -> position / 24f  // 小时 -> 天
                    DAY -> position / 7f  // 天 -> 周
                    WEEK -> position / 4f  // 周 -> 月（近似）
                    MONTH -> position / 3f  // 月 -> 季度

                    // 特殊处理：季度->年（原点变化：0->10）
                    // 季度i=0是当前季度，年i=10是当前年
                    // 因此：quarterPos / 4 需要加上10的偏移
                    QUARTER -> position / 4f + 10f  // 季度 -> 年

                    // 特殊处理：10年 -> 年
                    // 10年刻度: i=10是当前10年（2020-2029）
                    // 年刻度: i=10是当前年(2025)
                    // position=10.5(2025在2020-2029中) 应该对应到 年刻度的 i=10（2025年）
                    // 公式：10 + (position - 10) * 10
                    YEAR -> 10f + (position - 10f) * 10f  // 10年 -> 年

                    // 特殊处理：100年 -> 10年
                    DECADE -> 10f + (position - 10f) * 10f  // 100年 -> 10年
                    CENTURY -> position  // 已经是最粗刻度
                }
            }
            // 跨越多个刻度
            else -> {
                // 递归转换
                var currentPos = position
                var currentScale = this

                // 判断转换方向
                if (targetScale.ordinal > this.ordinal) {
                    // 向粗刻度转换
                    while (currentScale != targetScale) {
                        val nextScale = currentScale.getCoarserScale() ?: break
                        currentPos = currentScale.convertPositionTo(currentPos, nextScale)
                        currentScale = nextScale
                    }
                } else {
                    // 向细刻度转换
                    while (currentScale != targetScale) {
                        val nextScale = currentScale.getFinerScale() ?: break
                        currentPos = currentScale.convertPositionTo(currentPos, nextScale)
                        currentScale = nextScale
                    }
                }
                currentPos
            }
        }
    }
}
