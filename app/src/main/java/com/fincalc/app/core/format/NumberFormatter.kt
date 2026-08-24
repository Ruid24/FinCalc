package com.fincalc.app.core.format

import com.fincalc.app.core.expr.DisplayMode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * 卡西欧显示规则（说明书 CN-25/26 + CN-165）：
 * 最多 10 位有效数字；Fix 四舍五入到指定小数位；Sci 指定有效数字位数（恒指数）；
 * Norm1：|x| < 10⁻² 或 |x| ≥ 10¹⁰ 转指数；Norm2：|x| < 10⁻⁹ 或 |x| ≥ 10¹⁰ 转指数。
 * 输出指数形态为 "mantissaE指数"（如 1.23456789E-12）。
 */
object NumberFormatter {

    fun format(value: Double, mode: DisplayMode): String {
        if (value.isNaN() || value.isInfinite()) return "Math ERROR"
        if (value == 0.0) return "0"
        return when (mode) {
            is DisplayMode.Fix -> fix(value, mode.digits)
            is DisplayMode.Sci -> sci(value, mode.digits)
            is DisplayMode.Norm1 -> norm(value, -2)
            is DisplayMode.Norm2 -> norm(value, -9)
        }
    }

    /** Fix：四舍五入到 digits 位小数。 */
    private fun fix(value: Double, digits: Int): String =
        BigDecimal.valueOf(value).setScale(digits, RoundingMode.HALF_UP).toPlainString()

    /** Sci：digits 位有效数字，恒指数。 */
    private fun sci(value: Double, digits: Int): String {
        val bd = BigDecimal.valueOf(value).round(MathContext(digits, RoundingMode.HALF_UP))
        return toScientific(bd)
    }

    /** Norm：10 位有效数字；幅度在 (10^lowExp, 10^10) 之外转指数；否则普通小数并去尾零。 */
    private fun norm(value: Double, lowExp: Int): String {
        val bd = BigDecimal.valueOf(value).round(MathContext(10, RoundingMode.HALF_UP))
        val exp = bd.precision() - bd.scale() - 1
        if (exp < lowExp || exp >= 10) return toScientific(bd)
        return bd.stripTrailingZeros().toPlainString()
    }

    /** BigDecimal → "mantissaEexp"（mantissa 去尾零）。 */
    private fun toScientific(bd: BigDecimal): String {
        val exp = bd.precision() - bd.scale() - 1
        val mantissa = bd.movePointLeft(exp).stripTrailingZeros().toPlainString()
        return "${mantissa}E$exp"
    }
}
