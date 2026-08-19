package com.fincalc.app.core.solver

import com.fincalc.app.core.expr.CalcException
import kotlin.math.abs

/**
 * 数值求根：牛顿法为主、二分法兜底（设计文档 §4）。
 * 用于 IRR、CMPD 求 I%、债券 YLD 等无解析解场景（说明书：这些计算在真机上也是牛顿法近似）。
 * 失败抛 CalcException(MATH)，与真机报错一致。
 */
object Solver {

    /**
     * 求 f(x)=0 在 [lower, upper] 内的一个根。
     * 先以 [x0] 起跑牛顿法（中心差分导数）；出界、导数为 0、|f| 停滞或迭代超限则切换二分法（要求端点异号）。
     *
     * 注意：区间端点处 f 必须取有限值。含奇异点的函数（如 NPV 在 i=-1 发散）请把端点内缩（如 lower=-0.9999），
     * 否则二分兜底会在端点求值失败而误报求解失败。
     */
    fun solve(
        f: (Double) -> Double,
        x0: Double = 1.0,
        lower: Double = -1.0e9,
        upper: Double = 1.0e9,
        tol: Double = 1.0e-12,
        maxIterations: Int = 100
    ): Double {
        newton(f, x0.coerceIn(lower, upper), lower, upper, tol, maxIterations)?.let { return it }
        return bisection(f, lower, upper, tol, maxIterations)
    }

    private fun newton(
        f: (Double) -> Double,
        x0: Double,
        lower: Double,
        upper: Double,
        tol: Double,
        maxIter: Int
    ): Double? {
        var x = x0
        var best = Double.MAX_VALUE
        var stalls = 0
        repeat(maxIter) {
            val fx = f(x)
            if (!fx.isFinite()) return null
            val ax = abs(fx)
            if (ax <= tol) return x
            if (ax >= best - tol) {
                stalls++
                if (stalls >= 10) return null
            } else {
                stalls = 0
                best = ax
            }
            val h = 1e-7 * maxOf(1.0, abs(x))
            val d = (f(x + h) - f(x - h)) / (2 * h)
            if (!d.isFinite() || d == 0.0) return null
            val next = x - fx / d
            if (!next.isFinite() || next < lower || next > upper) return null
            if (abs(next - x) <= tol * maxOf(1.0, abs(next))) return next
            x = next
        }
        return null
    }

    private fun bisection(
        f: (Double) -> Double,
        lower: Double,
        upper: Double,
        tol: Double,
        maxIter: Int
    ): Double {
        var lo = lower
        var hi = upper
        var flo = f(lo)
        val fhi = f(hi)
        if (!flo.isFinite() || !fhi.isFinite()) fail("区间端点函数值非法")
        if (flo == 0.0) return lo
        if (fhi == 0.0) return hi
        if (flo * fhi > 0) fail("区间内无符号变化")
        repeat(maxIter) {
            val mid = (lo + hi) / 2
            val fm = f(mid)
            if (!fm.isFinite()) fail("函数值非法")
            if (abs(fm) <= tol || (hi - lo) / 2 <= tol * maxOf(1.0, abs(mid))) return mid
            if (flo * fm < 0) {
                hi = mid
            } else {
                lo = mid
                flo = fm
            }
        }
        fail("达到最大迭代次数仍未收敛")
    }

    private fun fail(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
