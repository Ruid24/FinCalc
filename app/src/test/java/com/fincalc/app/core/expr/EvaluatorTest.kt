package com.fincalc.app.core.expr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EvaluatorTest {
    private fun ev(input: String, ctx: EvalContext = DefaultContext()) = ExprEngine.eval(input, ctx)

    // ---- 算术与优先级（说明书 CN-35 起） ----

    @Test
    fun `manual arithmetic examples`() {
        assertEquals(36.0, ev("7×8-4×5"), 1e-12)              // L888
        assertEquals(15.0, ev("(2+3)×(4-1)"), 1e-12)         // L924
        assertEquals(24.0, ev("2(5+4)-2×(-3)"), 1e-12)       // L671
    }

    @Test
    fun `implicit mul same level as div`() {
        assertEquals(Math.PI / 2, ev("1÷2π"), 1e-9)          // L5196：1.570796327
        assertEquals(0.1591549431, ev("1÷(2π)"), 1e-9)
    }

    @Test
    fun `neg and square precedence`() {
        assertEquals(-4.0, ev("-2²"), 0.0)                   // L5200
        assertEquals(4.0, ev("(-2)²"), 0.0)
    }

    // ---- 百分比（说明书 CN-37/38） ----

    @Test
    fun `percent semantics`() {
        assertEquals(0.02, ev("2%"), 1e-15)                  // L945
        assertEquals(30.0, ev("150×20%"), 1e-12)             // L957
        assertEquals(75.0, ev("660÷880%"), 1e-9)             // L972
        assertEquals(2875.0, ev("2500+15%"), 1e-9)           // L984
        assertEquals(2625.0, ev("3500-25%"), 1e-9)           // L1000
    }

    @Test
    fun `percent with ans`() {
        val ctx = DefaultContext()
        assertEquals(1000.0, ev("168+98+734", ctx), 1e-12)   // L1018
        assertEquals(800.0, ev("Ans-20%", ctx), 1e-9)        // L1018-1042
    }

    // ---- 多语句与 Ans（说明书 CN-39/42） ----

    @Test
    fun `multi statement evaluates left to right`() {
        val ctx = DefaultContext()
        val results = ExprEngine.evalAll("3+3:3×3", ctx)
        assertEquals(listOf(6.0, 9.0), results)              // L1113
        assertEquals(9.0, ctx.getVar("Ans"), 0.0)
    }

    @Test
    fun `ans chain`() {
        val ctx = DefaultContext()
        assertEquals(12.0, ev("3×4", ctx), 0.0)
        assertEquals(0.4, ev("Ans÷30", ctx), 1e-12)          // L1196
        assertEquals(579.0, ev("123+456", ctx), 0.0)
        assertEquals(210.0, ev("789-Ans", ctx), 1e-12)       // L1214
    }

    @Test
    fun `variables`() {
        val ctx = DefaultContext()
        ctx.setVar("B", 57.0)                                // 9×6+3，L1323
        ctx.setVar("C", 40.0)                                // 5×8
        assertEquals(1.425, ev("B÷C", ctx), 1e-12)
    }

    // ---- 三角与角度（说明书函数章 CN-118 起） ----

    @Test
    fun `trig in degrees`() {
        assertEquals(0.5, ev("sin(30)"), 1e-9)               // L3549
        assertEquals(30.0, ev("asin(0.5)"), 1e-9)            // L3567
        assertEquals(180.0, ev("acos(-1)"), 1e-9)            // L3624
    }

    @Test
    fun `trig in radians and grads`() {
        val rad = DefaultContext(angle = AngleUnit.RAD)
        assertEquals(-1.0, ev("cos(π)", rad), 1e-9)          // L3614
        assertEquals(Math.PI, ev("acos(-1)", rad), 1e-9)     // L3648
        val gra = DefaultContext(angle = AngleUnit.GRA)
        assertEquals(0.0, ev("cos(100)", gra), 1e-9)         // L3622
    }

    @Test
    fun `hyperbolic`() {
        assertEquals(1.175201194, ev("sinh(1)"), 1e-9)       // L3571
        assertEquals(0.0, ev("acosh(1)"), 0.0)               // L3590
    }

    // ---- 对数指数幂根 ----

    @Test
    fun `log ln exp`() {
        assertEquals(4.0, ev("log(2,16)"), 1e-9)             // L3656
        assertEquals(1.204119983, ev("log(16)"), 1e-9)       // L3667
        assertEquals(4.49980967, ev("ln(90)"), 1e-9)         // L3678
        assertEquals(1.0, ev("ln(e)"), 1e-12)                // L3689
        assertEquals(kotlin.math.exp(10.0), ev("e^10"), 1e-6) // L3697：22026.46579
        assertEquals(1200.0, ev("1.2×10^3"), 1e-12)          // L3712
    }

    @Test
    fun `powers and roots`() {
        assertEquals(16.0, ev("(1+1)^(2+2)"), 1e-12)         // L3720
        assertEquals(8.0, ev("2³"), 0.0)                     // L3733
        assertEquals(1.0, ev("(√(2)+1)(√(2)-1)"), 1e-9)      // L3739
        assertEquals(2.0, ev("5ˣ√(32)"), 1e-9)               // L3749
        assertEquals(-1.290024053, ev("∛(5)+∛(-27)"), 1e-9)  // L3760
        assertEquals(-3.0, ev("3ˣ√(-27)"), 1e-9)
        assertEquals(0.125, ev("2^-3"), 1e-12)
    }

    // ---- Pol/Rec（说明书 CN-125/126） ----

    @Test
    fun `pol assigns X Y and returns r`() {
        val ctx = DefaultContext()
        assertEquals(2.0, ev("Pol(√(2),√(2))", ctx), 1e-9)
        assertEquals(2.0, ctx.getVar("X"), 1e-9)
        assertEquals(45.0, ctx.getVar("Y"), 1e-9)
    }

    @Test
    fun `rec assigns X Y and returns x`() {
        val ctx = DefaultContext()
        assertEquals(1.732050808, ev("Rec(2,30)", ctx), 1e-9)
        assertEquals(1.732050808, ctx.getVar("X"), 1e-9)
        assertEquals(1.0, ctx.getVar("Y"), 1e-9)
    }

    @Test
    fun `pol inside expression yields first value`() {
        assertEquals(7.0, ev("Pol(√(2),√(2))+5"), 1e-9)      // L3800
    }

    // ---- 阶乘、Abs、nPr/nCr、Ran#、Rnd ----

    @Test
    fun `factorial abs perm comb`() {
        assertEquals(40320.0, ev("(5+3)!"), 0.0)             // L3847
        assertEquals(5.0, ev("Abs(2-7)"), 0.0)               // L3863
        assertEquals(5040.0, ev("10 nPr 4"), 0.0)            // L3905
        assertEquals(210.0, ev("10 nCr 4"), 0.0)             // L3921
    }

    @Test
    fun `ran hash uses injected random`() {
        val ctx = DefaultContext(random = { 0.5839 })
        assertEquals(0.583, ev("Ran#", ctx), 0.0)            // L3875：3 位假随机数
        assertEquals(583.0, ev("1000Ran#", ctx), 1e-12)      // L3877
    }

    @Test
    fun `rnd respects display mode`() {
        val fix3 = DefaultContext(display = DisplayMode.Fix(3))
        // 说明书 L3935-3985：Fix3 下 Rnd(200÷7)=28.571，再 ×14 = 399.994
        assertEquals(399.994, ev("Rnd(200÷7)×14", fix3), 1e-9)
        assertEquals(400.0, ev("200÷7×14", fix3), 1e-9)      // 不 Rnd 时内部 15 位计算
        val norm = DefaultContext()
        assertEquals(0.3333333333, ev("Rnd(1÷3)", norm), 1e-12)
    }

    @Test
    fun `constants`() {
        assertEquals(Math.PI, ev("π"), 0.0)
        assertEquals(Math.E, ev("e"), 0.0)
    }

    // ---- 计算范围（说明书 CN-165：±9.999999999×10^99；CN-169：中间结果超范围即 Math ERROR） ----

    @Test
    fun `range enforcement`() {
        assertEquals(9.9E99, ev("9.9E99"), 1e85)             // 范围内最大量级合法
        assertMathErr("1E100")                               // 终值超范围
        assertMathErr("9E99+9E99")                           // 中间加法超范围（1.8E100，Double 内有限但真机报错）
        assertMathErr("9E99×9E99")                           // 中间乘法超范围
        assertMathErr("1÷(9E99×9E99)")                       // 中间超范围不得被后续运算"消化"成 0
    }

    @Test
    fun `rnd decimal boundary`() {
        // BigDecimal.valueOf 按十进制舍入：2.675 在真机 BCD 中精确存在 → 2.68
        val fix2 = DefaultContext(display = DisplayMode.Fix(2))
        assertEquals(2.68, ev("Rnd(2.675)", fix2), 0.0)
    }

    @Test
    fun `perm comb overflow fails fast`() {
        assertMathErr("999999999 nPr 999999999")             // 结果单调增长，超 1e100 即报错，不空转
        assertMathErr("999999999 nCr 499999999")
    }

    @Test
    fun `chained percent`() {
        // 卡西欧逐步加成：100+5%+10% = (105)+105×10/100 = 115.5
        assertEquals(115.5, ev("100+5%+10%"), 1e-9)
    }

    // ---- 错误（说明书 CN-169） ----

    @Test
    fun `math errors`() {
        assertMathErr("1÷0")
        assertMathErr("√(-1)")
        assertMathErr("log(0)")
        assertMathErr("ln(-5)")
        assertMathErr("asin(2)")
        assertMathErr("acos(1.5)")
        assertMathErr("tan(90)")
        assertMathErr("70!")
        assertMathErr("2.5!")
        assertMathErr("(-2)^0.5")
        assertMathErr("0^0")
        assertMathErr("2ˣ√(-16)")
        assertMathErr("10 nPr 4.5")
        assertMathErr("atanh(1)")
    }

    @Test
    fun `syntax error on wrong arity`() {
        val e = assertThrows(CalcException::class.java) { ev("log(1,2,3)") }
        assertEquals(CalcException.Kind.SYNTAX, e.kind)
    }

    private fun assertMathErr(input: String) {
        val e = assertThrows(CalcException::class.java) { ev(input) }
        assertEquals("输入 $input 应报 Math ERROR", CalcException.Kind.MATH, e.kind)
    }
}
