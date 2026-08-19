package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException

/** SMPL 单利（说明书 CN-50~52）：由 Dys/I%/PV 顺求 SI 与 SFV（无反解）。 */
object Smpl {

    /**
     * 单利 SI = −SI′，SI′ = Dys/daysInYear × PV × i（i = I%/100，CN-52）。
     * [daysInYear]：Date Mode 设定的一年天数基准，仅 360 或 365（CN-21）。
     */
    fun si(dys: Double, iPercent: Double, pv: Double, daysInYear: Int): Double {
        checkDays(daysInYear)
        return -(dys / daysInYear) * pv * (iPercent / 100)
    }

    /** 简单终值 SFV = −(PV + SI′)。 */
    fun sfv(dys: Double, iPercent: Double, pv: Double, daysInYear: Int): Double {
        checkDays(daysInYear)
        val siPrime = (dys / daysInYear) * pv * (iPercent / 100)
        return -(pv + siPrime)
    }

    private fun checkDays(daysInYear: Int) {
        if (daysInYear != 360 && daysInYear != 365) {
            throw CalcException(CalcException.Kind.ARGUMENT, "Date Mode 仅支持 360 或 365")
        }
    }
}
