package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException

/** COST 成本/售价/毛利（说明书 CN-74/75）。MRG 为毛利（占售价的百分比），非加价率。 */
object Cost {

    /** CST = SEL × (1 − MRG/100) */
    fun cst(sel: Double, mrg: Double): Double = sel * (1 - mrg / 100)

    /** SEL = CST ÷ (1 − MRG/100)。MRG=100 时除零 → Math ERROR。 */
    fun sel(cst: Double, mrg: Double): Double {
        val d = 1 - mrg / 100
        if (d == 0.0) throw CalcException(CalcException.Kind.MATH, "除以 0")
        return cst / d
    }

    /** MRG(%) = (1 − CST/SEL) × 100。SEL=0 时除零 → Math ERROR。 */
    fun mrg(cst: Double, sel: Double): Double {
        if (sel == 0.0) throw CalcException(CalcException.Kind.MATH, "除以 0")
        return (1 - cst / sel) * 100
    }
}
