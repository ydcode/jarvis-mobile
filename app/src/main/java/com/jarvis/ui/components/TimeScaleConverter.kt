package com.jarvis.ui.components

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * 时间尺度转换工具类
 * 负责在时间单位和Calendar之间进行转换
 */

/**
 * 将刻度单位转换为绝对时间（Calendar）
 * @param scale 当前刻度级别
 * @param units 刻度单位数
 * @param baseCalendar 基准Calendar（当前时间）
 * @return 对应的Calendar时间
 */
fun scaleUnitsToCalendar(scale: TimeScaleLevel, units: Float, baseCalendar: Calendar): Calendar {
    val result = baseCalendar.clone() as Calendar

    when (scale) {
        TimeScaleLevel.MINUTE -> {
            // 单位是分钟，i=0是今天00:00
            val totalMinutes = units.toInt()
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.MINUTE, totalMinutes)
        }
        TimeScaleLevel.FIVE_MINUTES -> {
            val totalMinutes = (units * 5).toInt()
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.MINUTE, totalMinutes)
        }
        TimeScaleLevel.TEN_MINUTES -> {
            val totalMinutes = (units * 10).toInt()
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.MINUTE, totalMinutes)
        }
        TimeScaleLevel.HALF_HOUR -> {
            val totalMinutes = (units * 30).toInt()
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.MINUTE, totalMinutes)
        }
        TimeScaleLevel.HOUR -> {
            // 单位是小时，i=0是今天00:00
            val hours = units
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.MINUTE, (hours * 60).toInt())
        }
        TimeScaleLevel.DAY -> {
            // 单位是天，i=0是今天，支持小数（表示天内的具体时刻）
            val daysPart = units.toInt()  // 整数部分：天数
            val fractionPart = units - daysPart  // 小数部分：天内的比例（0.0-1.0）
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.DAY_OF_MONTH, daysPart)
            // 添加小数部分对应的分钟数（1天 = 1440分钟）
            result.add(Calendar.MINUTE, (fractionPart * 24 * 60).toInt())
        }
        TimeScaleLevel.WEEK -> {
            // 单位是周，i=0是本周，支持小数（表示周内的具体时刻）
            val weeksPart = units.toInt()  // 整数部分：周数
            val fractionPart = units - weeksPart  // 小数部分：周内的比例（0.0-1.0）
            result.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.WEEK_OF_YEAR, weeksPart)
            // 添加小数部分对应的分钟数（1周 = 7 * 24 * 60分钟）
            result.add(Calendar.MINUTE, (fractionPart * 7 * 24 * 60).toInt())
        }
        TimeScaleLevel.MONTH -> {
            // 单位是月，i=0是当前月，支持小数（表示月内的具体时刻）
            val monthsPart = units.toInt()  // 整数部分：月数
            val fractionPart = units - monthsPart  // 小数部分：月内的比例（0.0-1.0）
            result.set(Calendar.DAY_OF_MONTH, 1)
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.MONTH, monthsPart)
            // 计算当前月的天数，然后添加小数部分对应的分钟数
            val daysInMonth = result.getActualMaximum(Calendar.DAY_OF_MONTH)
            result.add(Calendar.MINUTE, (fractionPart * daysInMonth * 24 * 60).toInt())
        }
        TimeScaleLevel.QUARTER -> {
            // 单位是季度，i=0是当前季度，支持小数（表示季度内的具体时刻）
            val quartersPart = units.toInt()  // 整数部分：季度数
            val fractionPart = units - quartersPart  // 小数部分：季度内的比例（0.0-1.0）
            val currentQuarter = result.get(Calendar.MONTH) / 3
            val quarterStartMonth = currentQuarter * 3
            result.set(Calendar.MONTH, quarterStartMonth)
            result.set(Calendar.DAY_OF_MONTH, 1)
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            result.add(Calendar.MONTH, quartersPart * 3)
            // 计算当前季度的天数，然后添加小数部分对应的分钟数
            val quarterEndCal = (result.clone() as Calendar).apply {
                add(Calendar.MONTH, 3)
            }
            val daysInQuarter = ((quarterEndCal.timeInMillis - result.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
            result.add(Calendar.MINUTE, (fractionPart * daysInQuarter * 24 * 60).toInt())
        }
        TimeScaleLevel.YEAR -> {
            // 单位是年，i=10是当前年，支持小数（表示年内的具体时刻）
            val currentYear = result.get(Calendar.YEAR)
            val yearOffset = units - 10f
            val yearsPart = yearOffset.toInt()  // 整数部分：年数偏移
            val fractionPart = yearOffset - yearsPart  // 小数部分：年内的比例（0.0-1.0）
            result.set(Calendar.YEAR, currentYear + yearsPart)
            result.set(Calendar.DAY_OF_YEAR, 1)
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)
            // 计算当前年的天数，然后添加小数部分对应的分钟数
            val daysInYear = result.getActualMaximum(Calendar.DAY_OF_YEAR)
            result.add(Calendar.MINUTE, (fractionPart * daysInYear * 24 * 60).toInt())
        }
        TimeScaleLevel.DECADE -> {
            // 单位是10年，units=10是当前10年的起始年，units的小数表示10年内的位置
            // 例如：units=10.0 表示当前10年的起始年（如2020）
            //       units=10.5 表示当前10年+5年（如2025）
            val currentYear = baseCalendar.get(Calendar.YEAR)
            val currentDecade = (currentYear / 10) * 10  // 当前10年的起始年（如2020）

            // units映射到实际10年：units=10 → currentDecade, units=11 → currentDecade+10
            val decadeOffset = (units - 10f)  // 相对于当前10年的偏移（单位：10年）
            val targetDecadeStart = currentDecade + (decadeOffset * 10f)  // 目标10年的起始年（带小数）

            val yearsPart = targetDecadeStart.toInt()  // 整数部分：目标年份
            val fractionPart = targetDecadeStart - yearsPart  // 小数部分：年内的比例（0.0-1.0）

            // 先设置到目标年的1月1日00:00:00
            result.set(Calendar.YEAR, yearsPart)
            result.set(Calendar.DAY_OF_YEAR, 1)
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)

            // 然后添加小数部分对应的时间
            val daysInYear = result.getActualMaximum(Calendar.DAY_OF_YEAR)
            result.add(Calendar.MINUTE, (fractionPart * daysInYear * 24 * 60).toInt())
        }
        TimeScaleLevel.CENTURY -> {
            // 单位是100年，units=10是当前100年的起始年，units的小数表示100年内的位置
            // 例如：units=10.0 表示当前100年的起始年（如2000）
            //       units=10.5 表示当前100年+50年（如2050）
            val currentYear = baseCalendar.get(Calendar.YEAR)
            val currentCentury = (currentYear / 100) * 100  // 当前100年的起始年（如2000）

            // units映射到实际100年：units=10 → currentCentury, units=11 → currentCentury+100
            val centuryOffset = (units - 10f)  // 相对于当前100年的偏移（单位：100年）
            val targetCenturyStart = currentCentury + (centuryOffset * 100f)  // 目标100年的起始年（带小数）

            val yearsPart = targetCenturyStart.toInt()  // 整数部分：目标年份
            val fractionPart = targetCenturyStart - yearsPart  // 小数部分：年内的比例（0.0-1.0）

            // 先设置到目标年的1月1日00:00:00
            result.set(Calendar.YEAR, yearsPart)
            result.set(Calendar.DAY_OF_YEAR, 1)
            result.set(Calendar.HOUR_OF_DAY, 0)
            result.set(Calendar.MINUTE, 0)
            result.set(Calendar.SECOND, 0)
            result.set(Calendar.MILLISECOND, 0)

            // 然后添加小数部分对应的时间
            val daysInYear = result.getActualMaximum(Calendar.DAY_OF_YEAR)
            result.add(Calendar.MINUTE, (fractionPart * daysInYear * 24 * 60).toInt())
        }
    }

    return result
}

/**
 * 将绝对时间（Calendar）转换为刻度单位
 * @param scale 目标刻度
 * @param time 时间点
 * @param baseCalendar 基准Calendar（当前时间）
 * @return 在该刻度下的单位数
 */
fun calendarToScaleUnits(scale: TimeScaleLevel, time: Calendar, baseCalendar: Calendar): Float {
    return when (scale) {
        TimeScaleLevel.MINUTE -> {
            // 计算time相对于今天00:00的分钟数
            val today = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((time.timeInMillis - today.timeInMillis) / (60 * 1000)).toFloat()
        }
        TimeScaleLevel.FIVE_MINUTES -> {
            val today = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((time.timeInMillis - today.timeInMillis) / (5 * 60 * 1000)).toFloat()
        }
        TimeScaleLevel.TEN_MINUTES -> {
            val today = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((time.timeInMillis - today.timeInMillis) / (10 * 60 * 1000)).toFloat()
        }
        TimeScaleLevel.HALF_HOUR -> {
            val today = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((time.timeInMillis - today.timeInMillis) / (30 * 60 * 1000)).toFloat()
        }
        TimeScaleLevel.HOUR -> {
            val today = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((time.timeInMillis - today.timeInMillis) / (60 * 60 * 1000)).toFloat()
        }
        TimeScaleLevel.DAY -> {
            val today = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((time.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000f))
        }
        TimeScaleLevel.WEEK -> {
            val thisWeek = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            ((time.timeInMillis - thisWeek.timeInMillis) / (7 * 24 * 60 * 60 * 1000f))
        }
        TimeScaleLevel.MONTH -> {
            val thisMonth = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // 计算月份差（整数部分）
            val yearDiff = time.get(Calendar.YEAR) - thisMonth.get(Calendar.YEAR)
            val monthDiff = time.get(Calendar.MONTH) - thisMonth.get(Calendar.MONTH)
            val wholeMonths = yearDiff * 12 + monthDiff

            // 计算小数部分：在当前月内的位置
            val monthStart = (time.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val monthEnd = (monthStart.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }
            val monthDurationMs = monthEnd.timeInMillis - monthStart.timeInMillis
            val positionInMonthMs = time.timeInMillis - monthStart.timeInMillis
            val fractionPart = positionInMonthMs.toFloat() / monthDurationMs.toFloat()

            wholeMonths.toFloat() + fractionPart
        }
        TimeScaleLevel.QUARTER -> {
            val currentQuarter = baseCalendar.get(Calendar.MONTH) / 3
            val thisQuarter = (baseCalendar.clone() as Calendar).apply {
                set(Calendar.MONTH, currentQuarter * 3)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // 计算季度差（整数部分）
            val yearDiff = time.get(Calendar.YEAR) - thisQuarter.get(Calendar.YEAR)
            val quarterDiff = time.get(Calendar.MONTH) / 3 - currentQuarter
            val wholeQuarters = yearDiff * 4 + quarterDiff

            // 计算小数部分：在当前季度内的位置
            val timeQuarter = time.get(Calendar.MONTH) / 3
            val quarterStart = (time.clone() as Calendar).apply {
                set(Calendar.MONTH, timeQuarter * 3)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val quarterEnd = (quarterStart.clone() as Calendar).apply {
                add(Calendar.MONTH, 3)
            }
            val quarterDurationMs = quarterEnd.timeInMillis - quarterStart.timeInMillis
            val positionInQuarterMs = time.timeInMillis - quarterStart.timeInMillis
            val fractionPart = positionInQuarterMs.toFloat() / quarterDurationMs.toFloat()

            wholeQuarters.toFloat() + fractionPart
        }
        TimeScaleLevel.YEAR -> {
            // i=10是当前年
            val currentYear = baseCalendar.get(Calendar.YEAR)
            val targetYear = time.get(Calendar.YEAR)
            val wholeYears = targetYear - currentYear

            // 计算小数部分：在当前年内的位置
            val yearStart = (time.clone() as Calendar).apply {
                set(Calendar.MONTH, 0)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val yearEnd = (yearStart.clone() as Calendar).apply {
                add(Calendar.YEAR, 1)
            }
            val yearDurationMs = yearEnd.timeInMillis - yearStart.timeInMillis
            val positionInYearMs = time.timeInMillis - yearStart.timeInMillis
            val fractionPart = positionInYearMs.toFloat() / yearDurationMs.toFloat()

            10f + wholeYears.toFloat() + fractionPart
        }
        TimeScaleLevel.DECADE -> {
            // units=10对应当前10年的起始年
            val currentYear = baseCalendar.get(Calendar.YEAR)
            val currentDecade = (currentYear / 10) * 10  // 当前10年的起始年
            val targetYear = time.get(Calendar.YEAR)

            // 计算目标年份相对于当前10年起始年的偏移（单位：年，带小数）
            val decadeStartYear = (targetYear / 10) * 10
            val decadeStart = (time.clone() as Calendar).apply {
                set(Calendar.YEAR, decadeStartYear)
                set(Calendar.MONTH, 0)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val decadeEnd = (decadeStart.clone() as Calendar).apply {
                add(Calendar.YEAR, 10)
            }
            val decadeDurationMs = decadeEnd.timeInMillis - decadeStart.timeInMillis
            val positionInDecadeMs = time.timeInMillis - decadeStart.timeInMillis
            val positionInDecade = positionInDecadeMs.toFloat() / decadeDurationMs.toFloat()  // 0.0-1.0

            // 计算目标10年相对于当前10年的偏移（单位：10年）
            val decadeOffset = (decadeStartYear - currentDecade) / 10f

            // 返回：10 + 10年偏移 + 10年内位置（转换为units）
            10f + decadeOffset + positionInDecade
        }
        TimeScaleLevel.CENTURY -> {
            // units=10对应当前100年的起始年
            val currentYear = baseCalendar.get(Calendar.YEAR)
            val currentCentury = (currentYear / 100) * 100  // 当前100年的起始年
            val targetYear = time.get(Calendar.YEAR)

            // 计算目标年份相对于当前100年起始年的偏移（单位：年，带小数）
            val centuryStartYear = (targetYear / 100) * 100
            val centuryStart = (time.clone() as Calendar).apply {
                set(Calendar.YEAR, centuryStartYear)
                set(Calendar.MONTH, 0)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val centuryEnd = (centuryStart.clone() as Calendar).apply {
                add(Calendar.YEAR, 100)
            }
            val centuryDurationMs = centuryEnd.timeInMillis - centuryStart.timeInMillis
            val positionInCenturyMs = time.timeInMillis - centuryStart.timeInMillis
            val positionInCentury = positionInCenturyMs.toFloat() / centuryDurationMs.toFloat()  // 0.0-1.0

            // 计算目标100年相对于当前100年的偏移（单位：100年）
            val centuryOffset = (centuryStartYear - currentCentury) / 100f

            // 返回：10 + 100年偏移 + 100年内位置（转换为units）
            10f + centuryOffset + positionInCentury
        }
    }
}
