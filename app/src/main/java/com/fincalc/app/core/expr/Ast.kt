package com.fincalc.app.core.expr

/** 语法树节点。² ³ ⁻¹ 在解析期归一为 Pow；两个消费者：求值器、LaTeX 排版器。 */
sealed class Node {
    data class Num(val raw: String, val value: Double) : Node()
    object Pi : Node()
    object EConst : Node()
    data class Var(val name: String) : Node()
    object Ran : Node()
    data class Add(val l: Node, val r: Node) : Node()
    data class Sub(val l: Node, val r: Node) : Node()
    data class Mul(val l: Node, val r: Node) : Node()
    data class Div(val l: Node, val r: Node) : Node()
    data class ImplicitMul(val l: Node, val r: Node) : Node()
    data class Neg(val e: Node) : Node()
    data class Pow(val base: Node, val exp: Node) : Node()
    data class XRoot(val degree: Node, val radicand: Node) : Node()
    data class Sqrt(val e: Node) : Node()
    data class Cbrt(val e: Node) : Node()
    data class Fact(val e: Node) : Node()
    data class Percent(val e: Node) : Node()
    data class Perm(val n: Node, val r: Node) : Node()
    data class Comb(val n: Node, val r: Node) : Node()
    data class Func(val fn: FuncName, val args: List<Node>) : Node()
}

/** 一段完整输入：多语句用 : 连接，从左至右执行（说明书 CN-39）。 */
data class Program(val statements: List<Node>)
