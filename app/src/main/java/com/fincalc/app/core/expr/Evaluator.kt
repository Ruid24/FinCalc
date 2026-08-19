package com.fincalc.app.core.expr

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * 求值器：AST → Double。Double 精度（15-16 位）近似真机 15 位内部计算（说明书 CN-165）。
 * 终值 Inf/NaN 一律报 Math ERROR。
 */
object Evaluator {

    fun eval(program: Program, ctx: EvalContext): Double = evalAll(program, ctx).last()

    /** 逐段求值；每段结束写回 Ans（说明书 CN-42：E 执行计算时更新答案存储器）。 */
    fun evalAll(program: Program, ctx: EvalContext): List<Double> =
        program.statements.map { stmt ->
            val v = checkFinite(evalNode(stmt, ctx))
            ctx.setVar("Ans", v)
            v
        }

    private fun checkFinite(v: Double): Double =
        if (v.isNaN() || v.isInfinite()) {
            throw CalcException(CalcException.Kind.MATH, "结果超出计算范围")
        } else {
            v
        }

    private fun mathErr(msg: String): Nothing = throw CalcException(CalcException.Kind.MATH, msg)

    fun evalNode(node: Node, ctx: EvalContext): Double = when (node) {
        is Node.Num -> node.value
        Node.Pi -> Math.PI
        Node.EConst -> Math.E
        is Node.Var -> ctx.getVar(node.name)
        // 说明书：Ran# 产生小于 1 的 3 位假随机数
        Node.Ran -> floor(ctx.nextRandom() * 1000) / 1000

        // 说明书 CN-37/38：加减语境中 b% 意为"基数 a 的百分之 b"
        is Node.Add -> {
            val l = evalNode(node.l, ctx)
            val r = node.r
            if (r is Node.Percent) l + l * evalNode(r.e, ctx) / 100 else l + evalNode(r, ctx)
        }
        is Node.Sub -> {
            val l = evalNode(node.l, ctx)
            val r = node.r
            if (r is Node.Percent) l - l * evalNode(r.e, ctx) / 100 else l - evalNode(r, ctx)
        }
        is Node.Mul -> evalNode(node.l, ctx) * evalNode(node.r, ctx)
        is Node.Div -> {
            val r = evalNode(node.r, ctx)
            if (r == 0.0) mathErr("除以 0")
            evalNode(node.l, ctx) / r
        }
        is Node.ImplicitMul -> evalNode(node.l, ctx) * evalNode(node.r, ctx)
        is Node.Neg -> -evalNode(node.e, ctx)
        is Node.Pow -> powChecked(evalNode(node.base, ctx), evalNode(node.exp, ctx))
        is Node.XRoot -> {
            val d = evalNode(node.degree, ctx)
            if (d == 0.0) mathErr("0 次根")
            val x = evalNode(node.radicand, ctx)
            if (x < 0) {
                // 说明书范围表的整数化简：负被开方数仅允许奇整数次根
                if (d != floor(d) || abs(d) >= 1e15 || abs(d) % 2.0 != 1.0) mathErr("负数的偶次根")
                -powChecked(-x, 1.0 / d)
            } else {
                powChecked(x, 1.0 / d)
            }
        }
        is Node.Sqrt -> {
            val v = evalNode(node.e, ctx)
            if (v < 0) mathErr("负数开平方")
            sqrt(v)
        }
        is Node.Cbrt -> cbrt(evalNode(node.e, ctx))
        is Node.Fact -> factorial(evalNode(node.e, ctx))
        is Node.Percent -> evalNode(node.e, ctx) / 100
        is Node.Perm -> permComb(evalNode(node.n, ctx), evalNode(node.r, ctx), true)
        is Node.Comb -> permComb(evalNode(node.n, ctx), evalNode(node.r, ctx), false)
        is Node.Func -> evalFunc(node, ctx)
    }

    /** x^y 定义域（说明书 CN-165 的整数化简）：x>0 任意 y；x=0 要求 y>0；x<0 要求 y 为整数。 */
    private fun powChecked(base: Double, exp: Double): Double {
        val v = when {
            base > 0 -> base.pow(exp)
            base == 0.0 -> {
                if (exp <= 0) mathErr("0 的非正次幂")
                0.0
            }
            else -> {
                if (exp != floor(exp) || abs(exp) >= 1e15) mathErr("负数的非整数次幂")
                base.pow(exp)
            }
        }
        return checkFinite(v)
    }

    /** 说明书：x! 自变量为 0 ≤ x ≤ 69 的整数。 */
    private fun factorial(x: Double): Double {
        if (x < 0 || x > 69 || x != floor(x)) mathErr("阶乘自变量须为 0..69 的整数")
        var r = 1.0
        var i = 2L
        while (i <= x.toLong()) {
            r *= i
            i++
        }
        return r
    }

    /** 说明书：n、r 为整数且 0 ≤ r ≤ n < 1e10。 */
    private fun permComb(n0: Double, r0: Double, perm: Boolean): Double {
        if (n0 != floor(n0) || r0 != floor(r0) || n0 < 0 || r0 < 0 || r0 > n0 || n0 >= 1e10) {
            mathErr("nPr/nCr 自变量超出范围")
        }
        var result = 1.0
        var k = 0.0
        if (perm) {
            while (k < r0) {
                result *= (n0 - k)
                k++
            }
        } else {
            val rr = minOf(r0, n0 - r0)
            while (k < rr) {
                result = result * (n0 - k) / (k + 1)
                k++
            }
            result = round(result)
        }
        return checkFinite(result)
    }

    /** Rnd（说明书 CN-128/129）：Norm → 尾数舍入至 10 位；Fix → 指定小数位；Sci → 指定有效位。 */
    private fun rnd(v: Double, mode: DisplayMode): Double = when (mode) {
        is DisplayMode.Fix -> BigDecimal(v).setScale(mode.digits, RoundingMode.HALF_UP).toDouble()
        is DisplayMode.Sci ->
            if (v == 0.0) 0.0 else BigDecimal(v, MathContext(mode.digits, RoundingMode.HALF_UP)).toDouble()
        else ->
            if (v == 0.0) 0.0 else BigDecimal(v, MathContext(10, RoundingMode.HALF_UP)).toDouble()
    }

    private fun evalFunc(node: Node.Func, ctx: EvalContext): Double {
        val f = ctx.angle.toRadians
        fun arity(n: Int) {
            if (node.args.size != n) {
                throw CalcException(CalcException.Kind.SYNTAX, "${node.fn} 需要 $n 个参数")
            }
        }
        fun a(i: Int) = evalNode(node.args[i], ctx)
        return when (node.fn) {
            FuncName.SIN -> { arity(1); sin(a(0) * f) }
            FuncName.COS -> { arity(1); cos(a(0) * f) }
            FuncName.TAN -> {
                arity(1)
                val t = a(0) * f
                val r = t % Math.PI
                if (abs(abs(r) - Math.PI / 2) < 1e-12) mathErr("tan 奇点")
                tan(t)
            }
            FuncName.ASIN -> { arity(1); val v = a(0); if (abs(v) > 1) mathErr("asin 定义域"); asin(v) / f }
            FuncName.ACOS -> { arity(1); val v = a(0); if (abs(v) > 1) mathErr("acos 定义域"); acos(v) / f }
            FuncName.ATAN -> { arity(1); atan(a(0)) / f }
            FuncName.SINH -> { arity(1); sinh(a(0)) }
            FuncName.COSH -> { arity(1); cosh(a(0)) }
            FuncName.TANH -> { arity(1); tanh(a(0)) }
            FuncName.ASINH -> { arity(1); asinh(a(0)) }
            FuncName.ACOSH -> { arity(1); val v = a(0); if (v < 1) mathErr("acosh 定义域"); acosh(v) }
            FuncName.ATANH -> { arity(1); val v = a(0); if (abs(v) >= 1) mathErr("atanh 定义域"); atanh(v) }
            FuncName.LN -> { arity(1); val v = a(0); if (v <= 0) mathErr("ln 定义域"); ln(v) }
            FuncName.LOG -> when (node.args.size) {
                1 -> { val v = a(0); if (v <= 0) mathErr("log 定义域"); log10(v) }
                2 -> {
                    val m = a(0)
                    val v = a(1)
                    if (m <= 0 || m == 1.0 || v <= 0) mathErr("log 定义域")
                    ln(v) / ln(m)
                }
                else -> throw CalcException(CalcException.Kind.SYNTAX, "log 需要 1 或 2 个参数")
            }
            FuncName.ABS -> { arity(1); abs(a(0)) }
            FuncName.RND -> { arity(1); rnd(a(0), ctx.display) }
            // 说明书 CN-125/126：Pol 结果 r→X、θ→Y（θ 按当前角度单位，∈(-180°,180°]）；表达式内取第一值
            FuncName.POL -> {
                arity(2)
                val x = a(0)
                val y = a(1)
                val r = hypot(x, y)
                val theta = atan2(y, x) / f
                ctx.setVar("X", r)
                ctx.setVar("Y", theta)
                r
            }
            FuncName.REC -> {
                arity(2)
                val r = a(0)
                val theta = a(1) * f
                val x = r * cos(theta)
                val y = r * sin(theta)
                ctx.setVar("X", x)
                ctx.setVar("Y", y)
                x
            }
        }
    }
}
