package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException

/**
 * DAYS 天数计算（说明书 CN-76~79）：d1、d2、Dys 知二求一。
 * Date Mode（CN-21）：365 按实际天数；360 按 30/360（含 31 日规则，CN-77 L2225）。
 * Date Input（CN-22）：MDY（月日年）或 DMY（日月年）。
 * 日期范围 1901-01-01 ~ 2099-12-31（CN-165）。
 */
object Days {

    enum class DateFormat { MDY, DMY }

    data class Date(val year: Int, val month: Int, val day: Int) : Comparable<Date> {
        override fun compareTo(other: Date): Int =
            compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
    }

    /** 紧凑数字 → Date（MDY 11052022 / DMY 05112022）。非法日期或超范围 → Argument ERROR。 */
    fun parse(input: Double, format: DateFormat): Date {
        if (input < 0 || input != kotlin.math.floor(input)) argErr("日期输入须为非负整数")
        val v = input.toLong()
        val year = (v % 10000).toInt()
        val md = (v / 10000).toInt()
        val month: Int
        val day: Int
        when (format) {
            DateFormat.MDY -> { month = md / 100; day = md % 100 }
            DateFormat.DMY -> { day = md / 100; month = md % 100 }
        }
        return validate(Date(year, month, day))
    }

    /** Date → 紧凑数字（MDY 11052022 / DMY 05112022）。 */
    fun format(date: Date, format: DateFormat): Double {
        val v = when (format) {
            DateFormat.MDY -> date.month * 1000000L + date.day * 10000L + date.year
            DateFormat.DMY -> date.day * 1000000L + date.month * 10000L + date.year
        }
        return v.toDouble()
    }

    /** 日期合法性 + 范围（1901-01-01 ~ 2099-12-31）校验。 */
    fun validate(date: Date): Date {
        if (date.year !in 1901..2099) argErr("年份超出 1901~2099")
        val dim = daysInMonth(date.month, date.year)
        if (dim == 0 || date.day !in 1..dim) argErr("非法日期")
        return date
    }

    fun isLeap(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    fun daysInMonth(month: Int, year: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeap(year)) 29 else 28
        else -> 0
    }

    /** Dys：365 按实际天数（JDN 差）；360 按 30/360（含 31 日规则）。 */
    fun daysBetween(d1: Date, d2: Date, days360: Boolean): Int {
        validate(d1)
        validate(d2)
        return if (days360) {
            val dd1 = if (d1.day == 31) 30 else d1.day
            var y2 = d2.year
            var m2 = d2.month
            var dd2 = d2.day
            if (dd2 == 31) {
                dd2 = 1
                m2++
                if (m2 == 13) { m2 = 1; y2++ }
            }
            360 * (y2 - d1.year) + 30 * (m2 - d1.month) + (dd2 - dd1)
        } else {
            toJdn(d2) - toJdn(d1)
        }
    }

    /** d2 = d1 + Dys（实际历法；说明书未给 360 模式的日期加减公式，例题均用 365）。 */
    fun plusDays(d1: Date, dys: Int): Date {
        validate(d1)
        return validate(fromJdn(toJdn(d1) + dys))
    }

    /** d1 = d2 − Dys（实际历法）。 */
    fun minusDays(d2: Date, dys: Int): Date {
        validate(d2)
        return validate(fromJdn(toJdn(d2) - dys))
    }

    /** 格里历 → JDN（Fliegel–Van Flandern，整数精确）。 */
    internal fun toJdn(date: Date): Int {
        val a = (14 - date.month) / 12
        val y = date.year + 4800 - a
        val m = date.month + 12 * a - 3
        return date.day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    /** JDN → 格里历（Fliegel–Van Flandern 逆变换）。 */
    internal fun fromJdn(jdn: Int): Date {
        var a = jdn + 32044
        val b = (4 * a + 3) / 146097
        a -= 146097 * b / 4
        val c = (4 * a + 3) / 1461
        a -= 1461 * c / 4
        val m = (5 * a + 2) / 153
        val day = a - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = 100 * b + c - 4800 + m / 10
        return Date(year, month, day)
    }

    private fun argErr(msg: String): Nothing = throw CalcException(CalcException.Kind.ARGUMENT, msg)
}
