package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.solver.Solver
import kotlin.math.pow

/** CASH 现金流（说明书 CN-60~64）：NPV、IRR、NFV、PBP。列表第 1 项为 CF₀。 */
object Cash {

    /** 最大数据项数 80（CF₀~CF₇₉，CN-63）。 */
    const val MAX_ITEMS = 80

    /** NPV = CF₀ + Σ CFₖ/(1+i)^k，i = I%/100（CN-63）。 */
    fun npv(iPercent: Double, cashFlows: List<Double>): Double {
        checkFlows(cashFlows)
        val i = iPercent / 100
        return cashFlows.foldIndexed(0.0) { k, acc, cf -> acc + cf / (1 + i).pow(k) }
    }

    /** NFV = NPV × (1+i)^n，n = 项数 − 1（CN-64）。 */
    fun nfv(iPercent: Double, cashFlows: List<Double>): Double {
        checkFlows(cashFlows)
        return npv(iPercent, cashFlows) * (1 + iPercent / 100).pow(cashFlows.size - 1)
    }

    /**
     * IRR（牛顿法，CN-63/64）：NPV = 0 的 i，返回百分比。
     * 全部现金流同号 → Math ERROR；结果 ≤ −50 → Math ERROR（CN-168）。
     */
    fun irr(cashFlows: List<Double>): Double {
        checkFlows(cashFlows)
        if (cashFlows.all { it >= 0 } || cashFlows.all { it <= 0 }) {
            throw CalcException(CalcException.Kind.MATH, "所有收入/付款值符号相同")
        }
        val i = Solver.solve(
            f = { r -> cashFlows.foldIndexed(0.0) { k, acc, cf -> acc + cf / (1 + r).pow(k) } },
            x0 = 0.1,
            lower = -0.9999,   // 避开 i=-1 奇异点（Solver KDoc 注意事项）
            upper = 10.0
        )
        val percent = i * 100
        if (percent <= -50) throw CalcException(CalcException.Kind.MATH, "IRR ≤ −50")
        return percent
    }

    /**
     * PBP（贴现回收期，CN-64，含线性内插；I=0 时为单回收期 SPP，CN-60）：
     * CF₀ ≥ 0 → 0；否则取首个满足 NPVₙ ≤ 0 ≤ NPVₙ₊₁ 的非负整数 n，
     * PBP = n − NPVₙ/(NPVₙ₊₁ − NPVₙ)。NPV 永不变号 → Math ERROR（本计划裁定）。
     */
    fun pbp(iPercent: Double, cashFlows: List<Double>): Double {
        checkFlows(cashFlows)
        if (cashFlows[0] >= 0) return 0.0
        val i = iPercent / 100
        var acc = 0.0
        var prev = 0.0
        for (k in cashFlows.indices) {
            acc += cashFlows[k] / (1 + i).pow(k)
            if (k > 0 && prev <= 0 && acc >= 0) {
                return (k - 1) - prev / (acc - prev)
            }
            prev = acc
        }
        throw CalcException(CalcException.Kind.MATH, "回收期不存在（NPV 未变号）")
    }

    private fun checkFlows(cashFlows: List<Double>) {
        if (cashFlows.isEmpty() || cashFlows.size > MAX_ITEMS) {
            throw CalcException(CalcException.Kind.ARGUMENT, "现金流项数须为 1 至 $MAX_ITEMS")
        }
    }
}
