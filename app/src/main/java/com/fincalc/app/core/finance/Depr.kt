package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import kotlin.math.floor

/**
 * DEPR 折旧（说明书 CN-80~84）：直线法 SL、定率法 FP、年数总和法 SYD、余额递减法 DB。
 * I%：FP 为折旧比、DB 为折旧因子（200=加倍余额递减 DDB，L2320）；SL/SYD 不用 I%（L2335）。
 * RDVⱼ：第 j 年年终剩余可折旧值（PV−FV 口径）；YR1：折旧第一年的月数（1~12）。
 */
object Depr {

    enum class Method { SL, FP, SYD, DB }

    /** 第 j 年折旧费与第 j 年末剩余可折旧值。 */
    data class Result(val depreciation: Double, val rdv: Double)

    /**
     * 错误（CN-168）：PV/FV/I% 为负 → MATH；n > 255 或 n < 1 → MATH；
     * j > n+1（YR1≠12）或 j > n（YR1=12）→ MATH；YR1 不在 1~12 → ARGUMENT。
     */
    fun depreciate(
        method: Method,
        n: Int,
        iPercent: Double,
        pv: Double,
        fv: Double,
        j: Int,
        yr1: Int
    ): Result {
        if (pv < 0 || fv < 0 || iPercent < 0) mathErr("PV/FV/I% 不得为负")
        if (n < 1 || n > 255) mathErr("n 须为 1 至 255")
        if (yr1 < 1 || yr1 > 12) throw CalcException(CalcException.Kind.ARGUMENT, "YR1 须为 1 至 12")
        val maxJ = if (yr1 == 12) n else n + 1
        if (j < 1 || j > maxJ) mathErr("j 超出范围")
        return when (method) {
            Method.SL -> sl(n, pv, fv, j, yr1)
            Method.FP -> fp(n, iPercent, pv, fv, j, yr1)
            Method.SYD -> syd(n, pv, fv, j, yr1)
            Method.DB -> db(n, iPercent, pv, fv, j, yr1)
        }
    }

    /** 直线法（CN-82）：SL₁ = (PV−FV)/n × YR1/12；SLⱼ = (PV−FV)/n；SLₙ₊₁ = (PV−FV)/n × (12−YR1)/12。 */
    private fun sl(n: Int, pv: Double, fv: Double, j: Int, yr1: Int): Result {
        val base = (pv - fv) / n
        var rdv = pv - fv
        var dep = 0.0
        for (year in 1..j) {
            dep = when (year) {
                1 -> base * yr1 / 12
                n + 1 -> base * (12 - yr1) / 12
                else -> base
            }
            rdv -= dep
        }
        return Result(dep, rdv)
    }

    /** 定率法（CN-82/83）：FP₁ = PV×I%/100×YR1/12；FPⱼ = (RDVⱼ₋₁+FV)×I%/100；FPₙ₊₁ = RDVₙ。 */
    private fun fp(n: Int, iPercent: Double, pv: Double, fv: Double, j: Int, yr1: Int): Result {
        var rdv = pv - fv
        var dep = 0.0
        for (year in 1..j) {
            dep = when (year) {
                1 -> pv * iPercent / 100 * yr1 / 12
                n + 1 -> rdv
                else -> (rdv + fv) * iPercent / 100
            }
            rdv -= dep
        }
        return Result(dep, rdv)
    }

    /**
     * 年数总和法（CN-83/84）：Z = n(n+1)/2；n′ = n − YR1/12；Z′ = (Intg(n′)+1)(Intg(n′)+2×Frac(n′))/2；
     * SYD₁ = n/Z × YR1/12 × (PV−FV)；SYDⱼ = ((n′−j+2)/Z′)(PV−FV−SYD₁)（j≠1）；
     * SYDₙ₊₁ = ((n′−(n+1)+2)/Z′)(PV−FV−SYD₁)×(12−YR1)/12。
     */
    private fun syd(n: Int, pv: Double, fv: Double, j: Int, yr1: Int): Result {
        val z = n * (n + 1) / 2.0
        val nPrime = n - yr1 / 12.0
        val intg = floor(nPrime).toInt()
        val frac = nPrime - intg
        val zPrime = (intg + 1) * (intg + 2 * frac) / 2
        var rdv = pv - fv
        var dep = 0.0
        var syd1 = 0.0
        for (year in 1..j) {
            dep = when (year) {
                1 -> n / z * yr1 / 12.0 * (pv - fv)
                n + 1 -> ((nPrime - (n + 1) + 2) / zPrime) * (pv - fv - syd1) * (12 - yr1) / 12.0
                else -> ((nPrime - year + 2) / zPrime) * (pv - fv - syd1)
            }
            if (year == 1) syd1 = dep
            rdv -= dep
        }
        return Result(dep, rdv)
    }

    /** 余额递减法（CN-84）：DB₁ = PV×I%/(100n)×YR1/12；DBⱼ = (RDVⱼ₋₁+FV)×I%/(100n)；DBₙ₊₁ = RDVₙ。 */
    private fun db(n: Int, iPercent: Double, pv: Double, fv: Double, j: Int, yr1: Int): Result {
        var rdv = pv - fv
        var dep = 0.0
        for (year in 1..j) {
            dep = when (year) {
                1 -> pv * iPercent / (100.0 * n) * yr1 / 12
                n + 1 -> rdv
                else -> (rdv + fv) * iPercent / (100.0 * n)
            }
            rdv -= dep
        }
        return Result(dep, rdv)
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
