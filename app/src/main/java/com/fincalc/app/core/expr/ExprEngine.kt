package com.fincalc.app.core.expr

/** 表达式引擎门面：线性输入串 → 解析 / 求值 / LaTeX。 */
object ExprEngine {
    fun parse(input: String): Program = Parser.parse(input)

    fun eval(input: String, ctx: EvalContext = DefaultContext()): Double =
        Evaluator.eval(parse(input), ctx)

    fun evalAll(input: String, ctx: EvalContext = DefaultContext()): List<Double> =
        Evaluator.evalAll(parse(input), ctx)

    fun latex(input: String): String = Latex.render(parse(input))
}
