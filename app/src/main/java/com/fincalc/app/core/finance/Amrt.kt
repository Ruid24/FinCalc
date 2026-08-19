package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import kotlin.math.abs

/**
 * AMRT 年限摊销（说明书 CN-65~70）。
 * BAL：PM2 付款完毕时的本金余额；INT/PRN：PM1 那笔的利息/本金部分；
 * ΣINT/ΣPRN：PM1 至 PM2 的合计。变量 n、I%、PV、PMT、FV、P/Y、C/Y 与 CMPD 共享（CN-67）。
 */
object Amrt {

    data class Result(
        val bal: Double,
        val int: Double,
        val prn: Double,
        val sumInt: Double,
        val sumPrn: Double
    )

    /**
     * 递推（CN-69/70）：BAL₀ = PV；
     * INTⱼ = |BALⱼ₋₁ × i| × (PMT 符号)；PRNⱼ = PMT + BALⱼ₋₁ × i；BALⱼ = BALⱼ₋₁ + PRNⱼ。
     * 期初（Payment.BEGIN）特例：INT₁ = 0、PRN₁ = PMT。
     * 利率与 CMPD 相同：名义利率 → 每期实际利率（CN-70）。
     */
    fun amortize(
        pm1: Int,
        pm2: Int,
        iPercent: Double,
        pv: Double,
        pmt: Double,
        py: Int,
        cy: Int,
        payment: Cmpd.Payment
    ): Result {
        if (pm1 < 1 || pm2 < 1 || pm1 > 9999 || pm2 > 9999 || pm1 >= pm2) {
            throw CalcException(CalcException.Kind.ARGUMENT, "PM1、PM2 须为 1 至 9999 的整数且 PM1 < PM2")
        }
        val i = Cmpd.periodRate(iPercent, py, cy)
        val pmtSign = if (pmt >= 0) 1.0 else -1.0
        var bal = pv
        var intPm1 = 0.0
        var prnPm1 = 0.0
        var sumInt = 0.0
        var sumPrn = 0.0
        for (j in 1..pm2) {
            val intJ: Double
            val prnJ: Double
            if (j == 1 && payment == Cmpd.Payment.BEGIN) {
                intJ = 0.0
                prnJ = pmt
            } else {
                intJ = abs(bal * i) * pmtSign
                prnJ = pmt + bal * i
            }
            bal += prnJ
            if (j == pm1) {
                intPm1 = intJ
                prnPm1 = prnJ
            }
            if (j >= pm1) {
                sumInt += intJ
                sumPrn += prnJ
            }
        }
        return Result(bal, intPm1, prnPm1, sumInt, sumPrn)
    }
}
