package com.fincalc.app.core.expr

/**
 * 排版器：AST → LaTeX 字符串。
 * 排版约定见计划 2 头部，测试逐字锁定。UI 的自然显示由 core/render 的自研排版器承担
 * （AndroidMath 路线已弃用）；本类保留为 LaTeX 文本通道（调试/日志/将来换库的备用）。
 */
object Latex {

    fun render(program: Program): String = program.statements.joinToString(" : ") { node(it) }

    private fun node(n: Node): String = when (n) {
        is Node.Num -> num(n)
        Node.Pi -> "\\pi"
        Node.EConst -> "e"
        is Node.Var -> n.name
        Node.Ran -> "\\mathrm{Ran\\#}"
        is Node.Add -> "${node(n.l)} + ${node(n.r)}"
        is Node.Sub -> "${node(n.l)} - ${factor(n.r)}"
        is Node.Mul -> "${factor(n.l)} \\times ${factor(n.r)}"
        is Node.Div -> "\\frac{${node(n.l)}}{${node(n.r)}}"
        is Node.ImplicitMul -> "${factor(n.l)} ${factor(n.r)}"
        is Node.Neg -> "-${factor(n.e)}"
        is Node.Pow -> "{${base(n.base)}}^{${node(n.exp)}}"
        is Node.XRoot -> "\\sqrt[${node(n.degree)}]{${node(n.radicand)}}"
        is Node.Sqrt -> "\\sqrt{${node(n.e)}}"
        is Node.Cbrt -> "\\sqrt[3]{${node(n.e)}}"
        is Node.Fact -> "${factor(n.e)}!"
        is Node.Percent -> "${factor(n.e)}\\%"
        is Node.Perm -> "${factor(n.n)}\\mathrm{P}${factor(n.r)}"
        is Node.Comb -> "${factor(n.n)}\\mathrm{C}${factor(n.r)}"
        is Node.Func -> func(n)
    }

    /** 指数计数法自然显示：1.2E3 → 1.2 \times 10^{3} */
    private fun num(n: Node.Num): String {
        val e = n.raw.indexOf('E')
        return if (e > 0) {
            "${n.raw.substring(0, e)} \\times 10^{${n.raw.substring(e + 1)}}"
        } else {
            n.raw
        }
    }

    /** 因子位置（乘/隐式乘/阶乘/%/负号/减法右侧）：加减式与负式加 (...) 保持语义、避免显示歧义（如 2(-3) 不排成 2 -3） */
    private fun factor(n: Node): String = when (n) {
        is Node.Add, is Node.Sub, is Node.Neg -> "(${node(n)})"
        else -> node(n)
    }

    /** 幂底数：加减/负/乘/隐式乘需加 (...) */
    private fun base(n: Node): String = when (n) {
        is Node.Add, is Node.Sub, is Node.Neg, is Node.Mul, is Node.ImplicitMul -> "(${node(n)})"
        else -> node(n)
    }

    private fun func(n: Node.Func): String {
        val a = n.args
        return when (n.fn) {
            FuncName.SIN -> "\\sin(${node(a[0])})"
            FuncName.COS -> "\\cos(${node(a[0])})"
            FuncName.TAN -> "\\tan(${node(a[0])})"
            FuncName.ASIN -> "\\sin^{-1}(${node(a[0])})"
            FuncName.ACOS -> "\\cos^{-1}(${node(a[0])})"
            FuncName.ATAN -> "\\tan^{-1}(${node(a[0])})"
            FuncName.SINH -> "\\sinh(${node(a[0])})"
            FuncName.COSH -> "\\cosh(${node(a[0])})"
            FuncName.TANH -> "\\tanh(${node(a[0])})"
            FuncName.ASINH -> "\\sinh^{-1}(${node(a[0])})"
            FuncName.ACOSH -> "\\cosh^{-1}(${node(a[0])})"
            FuncName.ATANH -> "\\tanh^{-1}(${node(a[0])})"
            FuncName.LN -> "\\ln(${node(a[0])})"
            FuncName.LOG -> if (a.size == 2) "\\log_{${node(a[0])}}(${node(a[1])})" else "\\log(${node(a[0])})"
            FuncName.ABS -> "|${node(a[0])}|"
            FuncName.RND -> "\\mathrm{Rnd}(${node(a[0])})"
            FuncName.POL -> "\\mathrm{Pol}(${node(a[0])}, ${node(a[1])})"
            FuncName.REC -> "\\mathrm{Rec}(${node(a[0])}, ${node(a[1])})"
        }
    }
}
