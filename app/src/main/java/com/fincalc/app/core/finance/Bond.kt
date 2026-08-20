package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.solver.Solver
import kotlin.math.pow

/**
 * BOND 债券（说明书 CN-85~92）。
 * 计算期两形态（CN-22 Bond Date）：Date（按购买日 d1/赎回日 d2）与 Term（按票息支付数 n）。
 * 日计数基准随 Date Mode（360/365）；Periods/Y：Annual=1 / Semi=2。
 * YLD 用牛顿法（CN-90，近似值精度受条件影响——说明书警告 L2729）。
 */
object Bond {

    /** Date 模式结果：净价 PRC、应计利息 INT、含息价 CST（每 $100 票面，CN-89）。 */
    data class DateResult(val prc: Double, val int: Double, val cst: Double)

    /**
     * PRC —— Bond Date = Date（CN-88/89）。
     * RDV ≤ 0 或 CPN < 0 → Math ERROR（CN-168）。d1 ≥ d2 → Argument ERROR（本计划裁定）。
     */
    fun prcDate(
        d1: Days.Date, d2: Days.Date,
        rdv: Double, cpn: Double, yld: Double,
        paymentsPerYear: Int, days360: Boolean
    ): DateResult {
        if (rdv <= 0 || cpn < 0) mathErr("须满足 RDV > 0、CPN ≥ 0")
        val m = checkM(paymentsPerYear)
        val sched = couponSchedule(d1, d2, m)
        val prev = sched.last { it <= d1 }
        val next = sched.first { it > d1 }
        val a = Days.daysBetween(prev, d1, days360)   // A：上一票息日 → 结算日
        val d = Days.daysBetween(prev, next, days360) // D：结算日所在票息期天数
        val n = sched.count { it > d1 && it <= d2 }   // N：到期前剩余票息次数
        val b = d - a                                 // B = D − A
        val y = yld / 100 / m
        val coupon = cpn / m
        val prc = if (n <= 1) {
            // 不超过一个票息期（CN-88）
            -(rdv + coupon) / (1 + (b.toDouble() / d) * y) + (a.toDouble() / d) * coupon
        } else {
            // 一个以上票息期（CN-89）
            var s = 0.0
            for (k in 1..n) s += coupon / (1 + y).pow(k - 1 + b.toDouble() / d)
            -rdv / (1 + y).pow(n - 1 + b.toDouble() / d) - s + (a.toDouble() / d) * coupon
        }
        val int = -(a.toDouble() / d) * coupon
        return DateResult(prc, int, prc + int)
    }

    /**
     * YLD —— Bond Date = Date（牛顿法，CN-90）。
     * RDV ≤ 0 或 PRC ≥ 0 → Math ERROR（CN-168）。
     */
    fun yldDate(
        d1: Days.Date, d2: Days.Date,
        rdv: Double, cpn: Double, prc: Double,
        paymentsPerYear: Int, days360: Boolean
    ): Double {
        if (rdv <= 0 || prc >= 0) mathErr("须满足 RDV > 0、PRC < 0")
        return Solver.solve(
            f = { y -> prcDate(d1, d2, rdv, cpn, y, paymentsPerYear, days360).prc - prc },
            x0 = 5.0,
            lower = -99.99,
            upper = 10000.0
        )
    }

    /**
     * PRC —— Bond Date = Term（CN-89）。INT=0、CST=PRC。
     * 前置要求 Date Mode=360 且 Annual（CN-88 L2638）属设置层职责，引擎不强制。
     */
    fun prcTerm(n: Int, rdv: Double, cpn: Double, yld: Double, paymentsPerYear: Int): Double {
        if (rdv <= 0 || cpn < 0) mathErr("须满足 RDV > 0、CPN ≥ 0")
        if (n < 1) mathErr("n 须为正整数")
        val m = checkM(paymentsPerYear)
        val y = yld / 100 / m
        val coupon = cpn / m
        var s = 0.0
        for (k in 1..n) s += coupon / (1 + y).pow(k)
        return -rdv / (1 + y).pow(n) - s
    }

    /** YLD —— Bond Date = Term（牛顿法）。 */
    fun yldTerm(n: Int, rdv: Double, cpn: Double, prc: Double, paymentsPerYear: Int): Double {
        if (rdv <= 0 || prc >= 0) mathErr("须满足 RDV > 0、PRC < 0")
        if (n < 1) mathErr("n 须为正整数")
        val m = checkM(paymentsPerYear)
        return Solver.solve(
            f = { y -> prcTerm(n, rdv, cpn, y, m) - prc },
            x0 = 5.0,
            lower = -99.99,
            upper = 10000.0
        )
    }

    /**
     * 票息日序列：与赎回日 d2 同月日、每年 m 次（月日钳制到当月长度，modified-following 惯例）。
     * 生成范围覆盖 d1 前 2 年至 d2 当年（保证 d1 所在票息期与全部剩余票息在列）。
     */
    private fun couponSchedule(d1: Days.Date, d2: Days.Date, m: Int): List<Days.Date> {
        if (d1 >= d2) throw CalcException(CalcException.Kind.ARGUMENT, "须满足 d1 < d2")
        val step = 12 / m
        val result = mutableListOf<Days.Date>()
        for (yy in (d1.year - 2)..d2.year) {
            for (k in 0 until m) {
                val t = yy * 12 + (d2.month - 1) - k * step
                val year = t / 12
                val month = t % 12 + 1
                result += Days.Date(year, month, minOf(d2.day, Days.daysInMonth(month, year)))
            }
        }
        return result.distinct().sorted()
    }

    private fun checkM(paymentsPerYear: Int): Int {
        if (paymentsPerYear != 1 && paymentsPerYear != 2) {
            throw CalcException(CalcException.Kind.ARGUMENT, "Periods/Y 仅支持 Annual(1) 或 Semi(2)")
        }
        return paymentsPerYear
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
