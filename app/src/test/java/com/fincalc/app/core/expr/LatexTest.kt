package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Test

class LatexTest {
    private fun lx(input: String) = ExprEngine.latex(input)

    @Test
    fun `arithmetic`() {
        assertEquals("2 + 3 \\times 4", lx("2+3×4"))
        assertEquals("2 \\times (3 + 4)", lx("2×(3+4)"))
        assertEquals("5 - (2 + 3)", lx("5-(2+3)"))
        assertEquals("5 - 2 - 3", lx("5-2-3"))
        assertEquals("5 - (2 - 3)", lx("5-(2-3)"))
    }

    @Test
    fun `division renders as fraction`() {
        // 权威语义对照：1÷2π 按 (1÷2)×π 排版
        assertEquals("\\frac{1}{2} \\pi", lx("1÷2π"))
        assertEquals("\\frac{1}{2 \\pi}", lx("1÷(2π)"))
        assertEquals("\\frac{2 + 3}{4 - 1}", lx("(2+3)÷(4-1)"))
    }

    @Test
    fun `implicit multiplication juxtaposed`() {
        assertEquals("2 \\pi", lx("2π"))
        assertEquals("5 A", lx("5A"))
        assertEquals("2 (5 + 4)", lx("2(5+4)"))
        assertEquals("2 \\sqrt{3}", lx("2√(3)"))
    }

    @Test
    fun `powers`() {
        assertEquals("{2}^{3}", lx("2^3"))
        assertEquals("-{2}^{2}", lx("-2²"))
        assertEquals("{(-2)}^{2}", lx("(-2)²"))
        assertEquals("{2}^{{3}^{2}}", lx("2^3^2"))
        assertEquals("{2}^{-3}", lx("2^-3"))
        assertEquals("{e}^{10}", lx("e^10"))
        assertEquals("{X}^{-1}", lx("X⁻¹"))
    }

    @Test
    fun `roots`() {
        assertEquals("\\sqrt{2}", lx("√(2)"))
        assertEquals("\\sqrt[3]{5}", lx("∛(5)"))
        assertEquals("\\sqrt[5]{32}", lx("5ˣ√(32)"))
    }

    @Test
    fun `postfix`() {
        assertEquals("(5 + 3)!", lx("(5+3)!"))
        assertEquals("5!", lx("5!"))
        assertEquals("2500 + 15\\%", lx("2500+15%"))
    }

    @Test
    fun `perm comb`() {
        assertEquals("10\\mathrm{P}4", lx("10 nPr 4"))
        assertEquals("10\\mathrm{C}4", lx("10 nCr 4"))
    }

    @Test
    fun `functions`() {
        assertEquals("\\sin(30)", lx("sin(30)"))
        assertEquals("\\sin^{-1}(0.5)", lx("asin(0.5)"))
        assertEquals("\\sinh(1)", lx("sinh(1)"))
        assertEquals("\\cosh^{-1}(1)", lx("acosh(1)"))
        assertEquals("\\ln(90)", lx("ln(90)"))
        assertEquals("\\log(16)", lx("log(16)"))
        assertEquals("\\log_{2}(16)", lx("log(2,16)"))
        assertEquals("|2 - 7|", lx("Abs(2-7)"))
        assertEquals("\\mathrm{Rnd}(Ans)", lx("Rnd(Ans)"))
        assertEquals("\\mathrm{Pol}(\\sqrt{2}, \\sqrt{2})", lx("Pol(√(2),√(2))"))
        assertEquals("\\mathrm{Rec}(2, 30)", lx("Rec(2,30)"))
    }

    @Test
    fun `ran hash`() {
        assertEquals("\\mathrm{Ran\\#}", lx("Ran#"))
        assertEquals("1000 \\mathrm{Ran\\#}", lx("1000Ran#"))
    }

    @Test
    fun `scientific literal natural display`() {
        assertEquals("1.2 \\times 10^{3}", lx("1.2E3"))
        assertEquals("1.2 \\times 10^{-3}", lx("1.2E-3"))
    }

    @Test
    fun `multi statement`() {
        assertEquals("3 + 3 : 3 \\times 3", lx("3+3:3×3"))
    }

    @Test
    fun `neg operand parenthesized to avoid ambiguity`() {
        // 审查发现：隐式乘/后缀运算符的负操作数不加括号会与减法等产生显示歧义
        assertEquals("2 (-3)", lx("2(-3)"))
        assertEquals("2 \\times (-3)", lx("2×-3"))
        assertEquals("5 - (-3)", lx("5--3"))
        assertEquals("2500 + (-3)\\%", lx("2500+(-3)%"))
        assertEquals("(-3)!", lx("(-3)!"))
    }
}
