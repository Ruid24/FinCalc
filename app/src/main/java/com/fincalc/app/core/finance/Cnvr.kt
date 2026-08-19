package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import kotlin.math.pow

/** CNVR 名义利率(APR) ⇄ 实际利率(EFF)（说明书 CN-71~73）。n = 年复利计算数。 */
object Cnvr {

    /** EFF = ((1 + APR/100/n)^n − 1) × 100（CN-73）。n ≤ 0 → Math ERROR（CN-168）。 */
    fun eff(apr: Double, n: Double): Double {
        checkN(n)
        return ((1 + apr / 100 / n).pow(n) - 1) * 100
    }

    /** APR = ((1 + EFF/100)^(1/n) − 1) × n × 100（CN-73）。 */
    fun apr(eff: Double, n: Double): Double {
        checkN(n)
        return ((1 + eff / 100).pow(1 / n) - 1) * n * 100
    }

    private fun checkN(n: Double) {
        if (n <= 0) throw CalcException(CalcException.Kind.MATH, "n 必须为正数")
    }
}
