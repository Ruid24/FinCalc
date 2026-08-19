package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.solver.Solver
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * CMPD 复利（说明书 CN-53~58）：n、I%、PV、PMT、FV 任求其一。
 * 前置设置：Payment（期初/期末，CN-20）、P/Y（年付款数）、C/Y（年复利数）、
 * dn（奇数期利息算法 CI/SI，CN-21；奇周期假定在首个完整付款期之前，CN-56）。
 */
object Cmpd {

    enum class Payment { BEGIN, END }
    enum class OddPeriod { CI, SI }

    /**
     * 每期实际利率 i（CN-57）：P/Y=C/Y=1 时 i = I%/100；
     * 否则 i = (1 + I%/(100·C/Y))^(C/Y÷P/Y) − 1。I% ≤ −100 → Math ERROR（CN-168）。
     */
    fun periodRate(iPercent: Double, py: Int, cy: Int): Double {
        if (iPercent <= -100) mathErr("I% ≤ −100")
        checkPyCy(py, cy)
        return if (py == 1 && cy == 1) {
            iPercent / 100
        } else {
            (1 + iPercent / (100.0 * cy)).pow(cy.toDouble() / py) - 1
        }
    }

    private class Coeffs(val alpha: Double, val beta: Double, val gamma: Double)

    /**
     * α/β/γ 系数（CN-57/58 L1669）。n ≤ 0 → Math ERROR（CN-168）。
     * i=0 时 α 取极限 Intg(n)（供求 I% 的残差函数在 0 邻域连续）。
     */
    private fun coeffs(n: Double, i: Double, payment: Payment, dn: OddPeriod): Coeffs {
        if (n <= 0) mathErr("n ≤ 0")
        val s = if (payment == Payment.BEGIN) 1.0 else 0.0
        val intg = floor(n)
        val frac = n - intg
        val beta = (1 + i).pow(-intg)
        val gamma = if (dn == OddPeriod.CI) (1 + i).pow(frac) else 1 + i * frac
        val alpha = if (i != 0.0) (1 + i * s) * (1 - beta) / i else intg
        return Coeffs(alpha, beta, gamma)
    }

    /** 求 PV（CN-57）：PV = (−α·PMT − β·FV)/γ；I%=0 时 PV = −(PMT×n + FV)（CN-58）。n ≤ 0 → Math ERROR（CN-168）。 */
    fun solvePV(
        n: Double, iPercent: Double, pmt: Double, fv: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        if (n <= 0) mathErr("n ≤ 0")
        if (iPercent == 0.0) return -(pmt * n + fv)
        val c = coeffs(n, periodRate(iPercent, py, cy), payment, dn)
        return (-c.alpha * pmt - c.beta * fv) / c.gamma
    }

    /** 求 PMT（CN-57）：PMT = (−γ·PV − β·FV)/α；I%=0 时 PMT = −(PV+FV)/n。n ≤ 0 → Math ERROR。 */
    fun solvePMT(
        n: Double, iPercent: Double, pv: Double, fv: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        if (n <= 0) mathErr("n ≤ 0")
        if (iPercent == 0.0) return -(pv + fv) / n
        val c = coeffs(n, periodRate(iPercent, py, cy), payment, dn)
        return (-c.gamma * pv - c.beta * fv) / c.alpha
    }

    /** 求 FV（CN-57）：FV = (−γ·PV − α·PMT)/β；I%=0 时 FV = −(PMT×n + PV)。n ≤ 0 → Math ERROR。 */
    fun solveFV(
        n: Double, iPercent: Double, pv: Double, pmt: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        if (n <= 0) mathErr("n ≤ 0")
        if (iPercent == 0.0) return -(pmt * n + pv)
        val c = coeffs(n, periodRate(iPercent, py, cy), payment, dn)
        return (-c.gamma * pv - c.alpha * pmt) / c.beta
    }

    /**
     * 求 n（CN-57）：n = log{((1+iS)PMT − FV·i) / ((1+iS)PMT + PV·i)} / log(1+i)。
     * I%=0 时 n = −(PV+FV)/PMT。真数或分母非法、结果非有限或不为正 → Math ERROR。
     */
    fun solveN(
        iPercent: Double, pv: Double, pmt: Double, fv: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        if (iPercent == 0.0) {
            if (pmt == 0.0) mathErr("除以 0")
            val n0 = -(pv + fv) / pmt
            if (!n0.isFinite() || n0 <= 0) mathErr("n 无解")
            return n0
        }
        val i = periodRate(iPercent, py, cy)
        val s = if (payment == Payment.BEGIN) 1.0 else 0.0
        val num = (1 + i * s) * pmt - fv * i
        val den = (1 + i * s) * pmt + pv * i
        if (den == 0.0 || num / den <= 0) mathErr("n 无解")
        val result = ln(num / den) / ln(1 + i)
        if (!result.isFinite() || result <= 0) mathErr("n 无解")
        return result
    }

    /**
     * 求 I%（牛顿法，CN-58）：解 γ×PV + α×PMT + β×FV = 0，再换算回名义利率。
     * 说明书警告（L1700-1702）：近似值，精度受计算条件影响。结果 ≤ −100 → Math ERROR（CN-168）。
     */
    fun solveI(
        n: Double, pv: Double, pmt: Double, fv: Double,
        py: Int, cy: Int, payment: Payment, dn: OddPeriod
    ): Double {
        checkPyCy(py, cy)
        val i = Solver.solve(
            f = { r ->
                val c = coeffs(n, r, payment, dn)
                c.gamma * pv + c.alpha * pmt + c.beta * fv
            },
            x0 = 0.05,
            lower = -0.9999,   // 避开 i=-1 奇异点（Solver KDoc 注意事项）
            upper = 10.0
        )
        val percent = if (py == 1 && cy == 1) {
            i * 100
        } else {
            ((1 + i).pow(py.toDouble() / cy) - 1) * cy * 100
        }
        if (percent <= -100) mathErr("I% ≤ −100")
        return percent
    }

    private fun checkPyCy(py: Int, cy: Int) {
        if (py < 1 || py > 9999 || cy < 1 || cy > 9999) {
            throw CalcException(CalcException.Kind.ARGUMENT, "P/Y、C/Y 须为 1 至 9999 的自然数")
        }
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
