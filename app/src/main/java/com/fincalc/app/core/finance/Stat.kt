package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * STAT 统计（说明书 CN-130~160）：1-VAR 单变量统计 + 2-VAR 七种回归。
 * 数据行 y 为 null 表示 1-VAR 数据；FREQ 为频率（重复次数，CN-131 L4034-4038）。
 * 数据行数上限（80/40/26）由 UI 编辑器层约束。
 * 回归模型（CN-130）：A+BX、_+CX²、ln X、e^X、A•B^X、A•X^B、1/X。
 */
object Stat {

    /** 数据行：1-VAR 时 y 为 null；freq 为频率（默认 1）。单类型设计避免 JVM 泛型签名冲突。 */
    data class Entry(val x: Double, val y: Double? = null, val freq: Double = 1.0)

    enum class RegType { LINEAR, QUADRATIC, LOG, EXP, AB_EXP, POWER, RECIPROCAL }

    /** 回归结果：c 仅二次回归（CN-147）；r 除二次回归外（二次回归 Reg 菜单无 r，CN-149）。 */
    data class RegResult(val a: Double, val b: Double, val c: Double? = null, val r: Double? = null)

    // ---------- 1-VAR（CN-137，忽略 y） ----------

    /** n = Σf。 */
    fun count(data: List<Entry>): Double {
        check1(data)
        return data.sumOf { it.freq }
    }

    /** Σx（频率加权）。 */
    fun sumX(data: List<Entry>): Double = sumW(data) { x }

    /** Σx²（频率加权）。 */
    fun sumX2(data: List<Entry>): Double = sumW(data) { x * x }

    /** x̄ = Σx/n。 */
    fun meanX(data: List<Entry>): Double {
        val n = count(data)
        if (n == 0.0) mathErr("无有效数据")
        return sumX(data) / n
    }

    /** 总体标准差 xσn = √(Σ(x−x̄)²/n)（定义式两遍法）。 */
    fun stdXn(data: List<Entry>): Double {
        val n = count(data)
        if (n == 0.0) mathErr("无有效数据")
        val m = meanX(data)
        return sqrt(data.sumOf { it.freq * (it.x - m) * (it.x - m) } / n)
    }

    /** 样本标准差 xσn−1 = √(Σ(x−x̄)²/(n−1))。n ≤ 1 → Math ERROR。 */
    fun stdXn1(data: List<Entry>): Double {
        val n = count(data)
        if (n <= 1) mathErr("样本数不足")
        val m = meanX(data)
        return sqrt(data.sumOf { it.freq * (it.x - m) * (it.x - m) } / (n - 1))
    }

    fun minX(data: List<Entry>): Double {
        check1(data)
        return data.minOf { it.x }
    }

    fun maxX(data: List<Entry>): Double {
        check1(data)
        return data.maxOf { it.x }
    }

    // ---------- 2-VAR（要求 y 非空，CN-141~146） ----------

    fun sumY(data: List<Entry>): Double = sumW2(data) { y!! }
    fun sumY2(data: List<Entry>): Double = sumW2(data) { y!! * y!! }
    fun sumXY(data: List<Entry>): Double = sumW2(data) { x * y!! }
    fun sumX3(data: List<Entry>): Double = sumW(data) { x * x * x }
    fun sumX2Y(data: List<Entry>): Double = sumW2(data) { x * x * y!! }
    fun sumX4(data: List<Entry>): Double = sumW(data) { x * x * x * x }

    fun meanY(data: List<Entry>): Double {
        val n = count(data)
        if (n == 0.0) mathErr("无有效数据")
        return sumY(data) / n
    }

    fun stdYn(data: List<Entry>): Double {
        check2(data)
        val m = meanY(data)
        return sqrt(data.sumOf { it.freq * (it.y!! - m) * (it.y!! - m) } / count(data))
    }

    fun stdYn1(data: List<Entry>): Double {
        val n = count(data)
        if (n <= 1) mathErr("样本数不足")
        check2(data)
        val m = meanY(data)
        return sqrt(data.sumOf { it.freq * (it.y!! - m) * (it.y!! - m) } / (n - 1))
    }

    fun minY(data: List<Entry>): Double {
        check2(data)
        return data.minOf { it.y!! }
    }

    fun maxY(data: List<Entry>): Double {
        check2(data)
        return data.maxOf { it.y!! }
    }

    // ---------- 回归（CN-141~160） ----------

    fun regress(type: RegType, data: List<Entry>): RegResult {
        check2(data)
        return when (type) {
            RegType.LINEAR -> linear(data)
            RegType.QUADRATIC -> quadratic(data)
            RegType.LOG -> logReg(data)
            RegType.EXP -> expReg(data)
            RegType.AB_EXP -> abExpReg(data)
            RegType.POWER -> powerReg(data)
            RegType.RECIPROCAL -> recipReg(data)
        }
    }

    /** 线性回归 y = A + BX（CN-141）。 */
    private fun linear(data: List<Entry>): RegResult {
        val n = count(data)
        val sx = sumX(data); val sxx = sumX2(data)
        val sy = sumY(data); val syy = sumY2(data)
        val sxy = sumXY(data)
        val bn = n * sxy - sx * sy
        val bd = n * sxx - sx * sx
        if (bd == 0.0) mathErr("除以 0")
        val b = bn / bd
        val a = (sy - b * sx) / n
        val rd = (n * sxx - sx * sx) * (n * syy - sy * sy)
        if (rd <= 0.0) mathErr("除以 0")
        val r = bn / sqrt(rd)
        return RegResult(a, b, null, r)
    }

    /** 二次回归 y = A + BX + CX²（CN-147~149，无 r）。 */
    private fun quadratic(data: List<Entry>): RegResult {
        val n = count(data)
        val sx = sumX(data); val sx2 = sumX2(data); val sx3 = sumX3(data); val sx4 = sumX4(data)
        val sy = sumY(data); val sxy = sumXY(data); val sx2y = sumX2Y(data)
        val sxx = sx2 - sx * sx / n
        val sxy_ = sxy - sx * sy / n
        val sxx2 = sx3 - sx * sx2 / n
        val sx2x2 = sx4 - sx2 * sx2 / n
        val sx2y_ = sx2y - sx2 * sy / n
        val d = sxx * sx2x2 - sxx2 * sxx2
        if (d == 0.0) mathErr("除以 0")
        val b = (sxy_ * sx2x2 - sx2y_ * sxx2) / d
        val c = (sx2y_ * sxx - sxy_ * sxx2) / d
        val a = sy / n - b * sx / n - c * sx2 / n
        return RegResult(a, b, c, null)
    }

    /** 对数回归 y = A + B·ln X（CN-151）。x ≤ 0 → Math ERROR。 */
    private fun logReg(data: List<Entry>): RegResult {
        val tx = data.map { Entry(lnPos(it.x), it.y, it.freq) }
        val n = count(tx)
        val su = sumX(tx); val suu = sumX2(tx)
        val sy = sumY(tx); val syy = sumY2(tx)
        val suy = sumXY(tx)
        val bn = n * suy - su * sy
        val bd = n * suu - su * su
        if (bd == 0.0) mathErr("除以 0")
        val b = bn / bd
        val a = (sy - b * su) / n
        val rd = (n * suu - su * su) * (n * syy - sy * sy)
        if (rd <= 0.0) mathErr("除以 0")
        return RegResult(a, b, null, bn / sqrt(rd))
    }

    /** e 指数回归 y = A·e^(BX)（CN-151）。y ≤ 0 → Math ERROR。 */
    private fun expReg(data: List<Entry>): RegResult {
        val ty = data.map { Entry(it.x, lnPos(it.y!!), it.freq) }
        val n = count(ty)
        val sx = sumX(ty); val sxx = sumX2(ty)
        val sv = sumY(ty); val svv = sumY2(ty)
        val sxv = sumXY(ty)
        val bn = n * sxv - sx * sv
        val bd = n * sxx - sx * sx
        if (bd == 0.0) mathErr("除以 0")
        val b = bn / bd
        val a = exp((sv - b * sx) / n)
        val rd = (n * sxx - sx * sx) * (n * svv - sv * sv)
        if (rd <= 0.0) mathErr("除以 0")
        return RegResult(a, b, null, bn / sqrt(rd))
    }

    /** ab 指数回归 y = A·B^X（CN-151/152）：对数域同 e 指数，B = e^(对数斜率)。 */
    private fun abExpReg(data: List<Entry>): RegResult {
        val ty = data.map { Entry(it.x, lnPos(it.y!!), it.freq) }
        val n = count(ty)
        val sx = sumX(ty); val sxx = sumX2(ty)
        val sv = sumY(ty); val svv = sumY2(ty)
        val sxv = sumXY(ty)
        val bn = n * sxv - sx * sv
        val bd = n * sxx - sx * sx
        if (bd == 0.0) mathErr("除以 0")
        val bSlope = bn / bd
        val a = exp((sv - bSlope * sx) / n)
        val rd = (n * sxx - sx * sx) * (n * svv - sv * sv)
        if (rd <= 0.0) mathErr("除以 0")
        return RegResult(a, exp(bSlope), null, bn / sqrt(rd))
    }

    /** 幂回归 y = A·X^B（CN-152/153）。x ≤ 0 或 y ≤ 0 → Math ERROR。 */
    private fun powerReg(data: List<Entry>): RegResult {
        val t = data.map { Entry(lnPos(it.x), lnPos(it.y!!), it.freq) }
        val n = count(t)
        val su = sumX(t); val suu = sumX2(t)
        val sv = sumY(t); val svv = sumY2(t)
        val suv = sumXY(t)
        val bn = n * suv - su * sv
        val bd = n * suu - su * su
        if (bd == 0.0) mathErr("除以 0")
        val b = bn / bd
        val a = exp((sv - b * su) / n)
        val rd = (n * suu - su * su) * (n * svv - sv * sv)
        if (rd <= 0.0) mathErr("除以 0")
        return RegResult(a, b, null, bn / sqrt(rd))
    }

    /** 倒数回归 y = A + B/X（CN-153）。x = 0 → Math ERROR。 */
    private fun recipReg(data: List<Entry>): RegResult {
        val tx = data.map { Entry(recipNz(it.x), it.y, it.freq) }
        val n = count(tx)
        val su = sumX(tx); val suu = sumX2(tx)
        val sy = sumY(tx); val syy = sumY2(tx)
        val suy = sumXY(tx)
        val sxx = suu - su * su / n
        if (sxx == 0.0) mathErr("除以 0")
        val syy_ = syy - sy * sy / n
        val sxy_ = suy - su * sy / n
        val b = sxy_ / sxx
        val a = sy / n - b * su / n
        if (syy_ <= 0.0) mathErr("除以 0")
        return RegResult(a, b, null, sxy_ / sqrt(sxx * syy_))
    }

    // ---------- 估计值 x̂ / ŷ（CN-146~160） ----------

    /** ŷ = 回归估计 y。定义域违规 → Math ERROR。 */
    fun estimateY(type: RegType, reg: RegResult, x: Double): Double = when (type) {
        RegType.LINEAR -> reg.a + reg.b * x
        RegType.QUADRATIC -> reg.a + reg.b * x + reg.c!! * x * x
        RegType.LOG -> {
            if (x <= 0) mathErr("ln 定义域")
            reg.a + reg.b * ln(x)
        }
        RegType.EXP -> reg.a * exp(reg.b * x)
        RegType.AB_EXP -> reg.a * reg.b.pow(x)
        RegType.POWER -> {
            if (x <= 0) mathErr("ln 定义域")
            reg.a * x.pow(reg.b)
        }
        RegType.RECIPROCAL -> {
            if (x == 0.0) mathErr("除以 0")
            reg.a + reg.b / x
        }
    }

    /** x̂ = 回归估计 x（二次回归有两个根，用 estimateXQuadratic）。 */
    fun estimateX(type: RegType, reg: RegResult, y: Double): Double {
        if (type == RegType.QUADRATIC) mathErr("二次回归有两个 x̂，请用 estimateXQuadratic")
        if (reg.b == 0.0) mathErr("除以 0")
        return when (type) {
            RegType.LINEAR -> (y - reg.a) / reg.b
            RegType.LOG -> exp((y - reg.a) / reg.b)
            RegType.EXP -> {
                if (y <= 0 || reg.a <= 0) mathErr("ln 定义域")
                (ln(y) - ln(reg.a)) / reg.b
            }
            RegType.AB_EXP -> {
                if (y <= 0 || reg.a <= 0 || reg.b <= 0 || reg.b == 1.0) mathErr("ln 定义域")
                (ln(y) - ln(reg.a)) / ln(reg.b)
            }
            RegType.POWER -> {
                if (y <= 0 || reg.a <= 0) mathErr("ln 定义域")
                exp((ln(y) - ln(reg.a)) / reg.b)
            }
            RegType.RECIPROCAL -> {
                val d = y - reg.a
                if (d == 0.0) mathErr("除以 0")
                reg.b / d
            }
            RegType.QUADRATIC -> throw AssertionError("unreachable")
        }
    }

    /** 二次回归的两个 x̂（CN-149）：x̂1,2 = (−B±√(B²−4C(A−y)))/(2C)。C=0 或判别式<0 → MATH。 */
    fun estimateXQuadratic(reg: RegResult, y: Double): Pair<Double, Double> {
        val c = reg.c!!
        if (c == 0.0) mathErr("除以 0")
        val disc = reg.b * reg.b - 4 * c * (reg.a - y)
        if (disc < 0) mathErr("判别式为负")
        val sq = sqrt(disc)
        return ((-reg.b + sq) / (2 * c)) to ((-reg.b - sq) / (2 * c))
    }

    // ---------- 内部 ----------

    private fun sumW(data: List<Entry>, selector: Entry.() -> Double): Double {
        check1(data)
        return data.sumOf { it.selector() * it.freq }
    }

    private fun sumW2(data: List<Entry>, selector: Entry.() -> Double): Double {
        check2(data)
        return data.sumOf { it.selector() * it.freq }
    }

    private fun lnPos(v: Double): Double {
        if (v <= 0) mathErr("ln 定义域")
        return ln(v)
    }

    private fun recipNz(v: Double): Double {
        if (v == 0.0) mathErr("除以 0")
        return 1 / v
    }

    private fun check1(data: List<Entry>) {
        if (data.isEmpty()) mathErr("无数据")
        if (data.any { it.freq < 0 }) mathErr("频率不得为负")
    }

    private fun check2(data: List<Entry>) {
        check1(data)
        if (data.any { it.y == null }) mathErr("需要成对数据")
    }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)
}
