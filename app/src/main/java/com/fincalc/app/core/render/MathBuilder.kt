package com.fincalc.app.core.render

import com.fincalc.app.core.expr.FuncName
import com.fincalc.app.core.expr.Node
import com.fincalc.app.core.expr.Program

/**
 * AST → MathBox 排版树。排版约定与 Latex.kt 一致（分数、幂、根号、隐式乘并置、负式加括号）。
 * 上标/下标用 SCRIPT 缩放。
 */
object MathBuilder {

    const val SCRIPT = 0.7f
    private const val BASELINE_FRAC = 0.8f

    fun build(program: Program, m: TextMeasure, em: Float): MathBox =
        RowBox(program.statements.map { build(it, m, em) }.intersperse(text(" : ", m, em)))

    fun build(node: Node, m: TextMeasure, em: Float, scale: Float = 1f): MathBox = when (node) {
        is Node.Num -> num(node, m, em, scale)
        Node.Pi -> text("π", m, em, scale)
        Node.EConst -> text("e", m, em, scale)
        is Node.Var -> text(node.name, m, em, scale)
        Node.Ran -> text("Ran#", m, em, scale)
        is Node.Add -> row(m, em, scale, build(node.l, m, em, scale), text(" + ", m, em, scale), build(node.r, m, em, scale))
        is Node.Sub -> row(m, em, scale, build(node.l, m, em, scale), text(" − ", m, em, scale), factor(node.r, m, em, scale))
        is Node.Mul -> row(m, em, scale, factor(node.l, m, em, scale), text(" × ", m, em, scale), factor(node.r, m, em, scale))
        is Node.Div -> FracBox(build(node.l, m, em, scale), build(node.r, m, em, scale), em)
        is Node.ImplicitMul -> row(m, em, scale, factor(node.l, m, em, scale), text(" ", m, em, scale), factor(node.r, m, em, scale))
        is Node.Neg -> row(m, em, scale, text("−", m, em, scale), factor(node.e, m, em, scale))
        is Node.Pow -> SupBox(base(node.base, m, em, scale), build(node.exp, m, em, SCRIPT), em)
        is Node.XRoot -> SqrtBox(build(node.radicand, m, em, scale), build(node.degree, m, em, SCRIPT), em)
        is Node.Sqrt -> SqrtBox(build(node.e, m, em, scale), null, em)
        is Node.Cbrt -> SqrtBox(build(node.e, m, em, scale), text("3", m, em, SCRIPT), em)
        is Node.Fact -> row(m, em, scale, factor(node.e, m, em, scale), text("!", m, em, scale))
        is Node.Percent -> row(m, em, scale, factor(node.e, m, em, scale), text("%", m, em, scale))
        is Node.Perm -> row(m, em, scale, factor(node.n, m, em, scale), text("P", m, em, scale), factor(node.r, m, em, scale))
        is Node.Comb -> row(m, em, scale, factor(node.n, m, em, scale), text("C", m, em, scale), factor(node.r, m, em, scale))
        is Node.Func -> func(node, m, em, scale)
    }

    /** 指数计数法自然显示：1.2E3 → 1.2 × 10^3。 */
    private fun num(n: Node.Num, m: TextMeasure, em: Float, scale: Float): MathBox {
        val e = n.raw.indexOf('E')
        return if (e > 0) {
            row(
                m, em, scale,
                text(n.raw.substring(0, e), m, em, scale),
                text(" × ", m, em, scale),
                SupBox(text("10", m, em, scale), text(n.raw.substring(e + 1), m, em, SCRIPT), em)
            )
        } else {
            text(n.raw, m, em, scale)
        }
    }

    /** 因子位置（乘/隐式乘/阶乘/%/负号/减法右侧）：加减式与负式加括号。 */
    private fun factor(n: Node, m: TextMeasure, em: Float, scale: Float): MathBox = when (n) {
        is Node.Add, is Node.Sub, is Node.Neg -> paren(n, m, em, scale)
        else -> build(n, m, em, scale)
    }

    /** 幂底数：加减/负/乘/隐式乘需加括号。 */
    private fun base(n: Node, m: TextMeasure, em: Float, scale: Float): MathBox = when (n) {
        is Node.Add, is Node.Sub, is Node.Neg, is Node.Mul, is Node.ImplicitMul -> paren(n, m, em, scale)
        else -> build(n, m, em, scale)
    }

    private fun paren(n: Node, m: TextMeasure, em: Float, scale: Float): MathBox =
        row(m, em, scale, text("(", m, em, scale), build(n, m, em, scale), text(")", m, em, scale))

    private val FUNC_LABELS = mapOf(
        FuncName.SIN to "sin", FuncName.COS to "cos", FuncName.TAN to "tan",
        FuncName.ASIN to "sin⁻¹", FuncName.ACOS to "cos⁻¹", FuncName.ATAN to "tan⁻¹",
        FuncName.SINH to "sinh", FuncName.COSH to "cosh", FuncName.TANH to "tanh",
        FuncName.ASINH to "sinh⁻¹", FuncName.ACOSH to "cosh⁻¹", FuncName.ATANH to "tanh⁻¹",
        FuncName.LN to "ln", FuncName.RND to "Rnd", FuncName.POL to "Pol", FuncName.REC to "Rec"
    )

    private fun func(node: Node.Func, m: TextMeasure, em: Float, scale: Float): MathBox = when (node.fn) {
        FuncName.LOG ->
            if (node.args.size == 2) {
                row(
                    m, em, scale,
                    SubBox(text("log", m, em, scale), build(node.args[0], m, em, SCRIPT), em),
                    text("(", m, em, scale),
                    build(node.args[1], m, em, scale),
                    text(")", m, em, scale)
                )
            } else {
                plainFunc("log", node.args, m, em, scale)
            }
        FuncName.ABS -> row(m, em, scale, text("|", m, em, scale), build(node.args[0], m, em, scale), text("|", m, em, scale))
        else -> plainFunc(FUNC_LABELS.getValue(node.fn), node.args, m, em, scale)
    }

    private fun plainFunc(label: String, args: List<Node>, m: TextMeasure, em: Float, scale: Float): MathBox =
        row(
            m, em, scale,
            listOf(text(label, m, em, scale), text("(", m, em, scale)) +
                args.map { build(it, m, em, scale) }.intersperse(text(", ", m, em, scale)) +
                listOf(text(")", m, em, scale))
        )

    internal fun text(s: String, m: TextMeasure, em: Float, scale: Float = 1f): TextBox {
        val (w, h) = m.measure(s, scale)
        return TextBox(s, scale, w, h, h * BASELINE_FRAC)
    }

    private fun row(m: TextMeasure, em: Float, scale: Float, vararg boxes: MathBox): RowBox =
        RowBox(boxes.toList())

    private fun row(m: TextMeasure, em: Float, scale: Float, boxes: List<MathBox>): RowBox =
        RowBox(boxes)

    /** 在元素间插入分隔符（Kotlin 标准库无此函数，自行实现）。 */
    private fun <T> List<T>.intersperse(sep: T): List<T> =
        flatMapIndexed { i, item -> if (i == 0) listOf(item) else listOf(sep, item) }
}
