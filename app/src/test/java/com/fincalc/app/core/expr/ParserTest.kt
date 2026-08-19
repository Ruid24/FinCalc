package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ParserTest {
    private fun n(v: Double) =
        Node.Num(if (v == Math.floor(v) && v.isFinite()) v.toLong().toString() else v.toString(), v)

    @Test
    fun `simple precedence mul over add`() {
        val p = Parser.parse("2+3×4")
        assertEquals(Program(listOf(Node.Add(n(2.0), Node.Mul(n(3.0), n(4.0))))), p)
    }

    @Test
    fun `implicit mul same level as div and left associative`() {
        // 权威语义（说明书 CN-162）：1÷2π = (1÷2)×π
        val p = Parser.parse("1÷2π")
        assertEquals(Program(listOf(Node.ImplicitMul(Node.Div(n(1.0), n(2.0)), Node.Pi))), p)
    }

    @Test
    fun `implicit mul parenthesized binds inside parens`() {
        val p = Parser.parse("1÷(2π)")
        assertEquals(Program(listOf(Node.Div(n(1.0), Node.ImplicitMul(n(2.0), Node.Pi)))), p)
    }

    @Test
    fun `square binds tighter than unary minus`() {
        // 权威语义（说明书 L5200）：-2² = -(2²)
        val p = Parser.parse("-2²")
        assertEquals(Program(listOf(Node.Neg(Node.Pow(n(2.0), n(2.0))))), p)
    }

    @Test
    fun `paren neg square`() {
        val p = Parser.parse("(-2)²")
        assertEquals(Program(listOf(Node.Pow(Node.Neg(n(2.0)), n(2.0)))), p)
    }

    @Test
    fun `implicit mul before paren func var`() {
        assertEquals(
            Program(listOf(Node.ImplicitMul(n(2.0), Node.Add(n(5.0), n(4.0))))),
            Parser.parse("2(5+4)")
        )
        assertEquals(
            Program(listOf(Node.ImplicitMul(n(2.0), Node.Func(FuncName.SIN, listOf(n(30.0)))))),
            Parser.parse("2sin(30)")
        )
        assertEquals(
            Program(listOf(Node.ImplicitMul(n(5.0), Node.Var("A")))),
            Parser.parse("5A")
        )
        assertEquals(
            Program(listOf(Node.ImplicitMul(Node.Var("A"), Node.Func(FuncName.SIN, listOf(n(30.0)))))),
            Parser.parse("Asin(30)")
        )
    }

    @Test
    fun `power right associative and negative exponent`() {
        assertEquals(
            Program(listOf(Node.Pow(n(2.0), Node.Pow(n(3.0), n(2.0))))),
            Parser.parse("2^3^2")
        )
        assertEquals(
            Program(listOf(Node.Pow(n(2.0), Node.Neg(n(3.0))))),
            Parser.parse("2^-3")
        )
    }

    @Test
    fun `postfix normalization`() {
        assertEquals(
            Program(listOf(Node.Pow(n(2.0), n(3.0)))),
            Parser.parse("2³")
        )
        assertEquals(
            Program(listOf(Node.Pow(n(4.0), Node.Num("-1", -1.0)))),
            Parser.parse("4⁻¹")
        )
        assertEquals(
            Program(listOf(Node.Fact(Node.Add(n(5.0), n(3.0))))),
            Parser.parse("(5+3)!")
        )
        assertEquals(
            Program(listOf(Node.Percent(n(15.0)))),
            Parser.parse("15%")
        )
    }

    @Test
    fun `perm comb are infix above muldiv`() {
        assertEquals(
            Program(listOf(Node.Mul(Node.Perm(n(10.0), n(4.0)), n(2.0)))),
            Parser.parse("10 nPr 4×2")
        )
        assertEquals(
            Program(listOf(Node.Comb(n(10.0), n(4.0)))),
            Parser.parse("10 nCr 4")
        )
    }

    @Test
    fun `xroot infix with parenthesized radicand`() {
        assertEquals(
            Program(listOf(Node.XRoot(n(5.0), n(32.0)))),
            Parser.parse("5ˣ√(32)")
        )
    }

    @Test
    fun `sqrt cbrt function form`() {
        assertEquals(
            Program(listOf(
                Node.Add(
                    Node.ImplicitMul(Node.Add(Node.Sqrt(n(2.0)), n(1.0)), Node.Sub(Node.Sqrt(n(2.0)), n(1.0))),
                    Node.Cbrt(n(5.0))
                )
            )),
            Parser.parse("(√(2)+1)(√(2)-1)+∛(5)")
        )
    }

    @Test
    fun `two arg functions`() {
        assertEquals(
            Program(listOf(Node.Func(FuncName.LOG, listOf(n(2.0), n(16.0))))),
            Parser.parse("log(2,16)")
        )
        assertEquals(
            Program(listOf(Node.Func(FuncName.POL, listOf(Node.Sqrt(n(2.0)), Node.Sqrt(n(2.0)))))),
            Parser.parse("Pol(√(2),√(2))")
        )
    }

    @Test
    fun `multi statement program`() {
        val p = Parser.parse("3+3:3×3")
        assertEquals(
            Program(listOf(Node.Add(n(3.0), n(3.0)), Node.Mul(n(3.0), n(3.0)))),
            p
        )
    }

    @Test
    fun `trailing rparen may be omitted`() {
        assertEquals(
            Program(listOf(Node.Mul(Node.Add(n(2.0), n(3.0)), Node.Sub(n(4.0), n(1.0))))),
            Parser.parse("(2+3)×(4-1")
        )
    }

    @Test
    fun `empty input syntax error`() {
        assertThrows(CalcException::class.java) { Parser.parse("") }
    }

    @Test
    fun `dangling operator syntax error`() {
        assertThrows(CalcException::class.java) { Parser.parse("1+") }
    }

    @Test
    fun `mismatched paren syntax error`() {
        assertThrows(CalcException::class.java) { Parser.parse("(1+2))") }
    }

    @Test
    fun `deep nesting stack error`() {
        val deep = "(".repeat(30) + "1" + ")".repeat(30)
        val e = assertThrows(CalcException::class.java) { Parser.parse(deep) }
        assertEquals(CalcException.Kind.STACK, e.kind)
    }

    @Test
    fun `moderate nesting ok`() {
        val ok = "(".repeat(20) + "1" + ")".repeat(20)
        assertEquals(Program(listOf(n(1.0))), Parser.parse(ok))
    }
}
