package com.fincalc.app.core.render

import com.fincalc.app.core.expr.ExprEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathBuilderTest {
    private val fake = TextMeasure { t, s -> (t.length * 10f * s) to (20f * s) }
    private val em = 20f

    private fun build(input: String): MathBox {
        val program = ExprEngine.parse(input)
        // 单语句直接取该表达式的盒子；多语句走 Program 拼接（" : " 分隔）
        return if (program.statements.size == 1) {
            MathBuilder.build(program.statements[0], fake, em)
        } else {
            MathBuilder.build(program, fake, em)
        }
    }

    @Test
    fun `plain number is text box`() {
        val b = build("123")
        assertTrue(b is TextBox)
        assertEquals(30f, b.width, 1e-4f)
        assertEquals(20f, b.height, 1e-4f)
        assertEquals(16f, b.baseline, 1e-4f)
    }

    @Test
    fun `division becomes frac box with centered geometry`() {
        val b = build("1÷2")
        assertTrue(b is FracBox)
        val f = b as FracBox
        // num/den 各 1 字符宽 10；宽 = max + 2×pad(0.2em=4)
        assertEquals(10f + 8f, f.width, 1e-4f)
        // lineY = num.height(20) + gap(3) + line/2(0.6)
        assertEquals(23.6f, f.lineY, 1e-4f)
        assertEquals(23.6f, f.baseline, 1e-4f)
        // 高 = 20 + 2×3 + 1.2 + 20
        assertEquals(47.2f, f.height, 1e-4f)
    }

    @Test
    fun `implicit mul renders juxtaposed after fraction`() {
        // 权威对照（同 Latex 约定）：1÷2π → \frac{1}{2} π
        val b = build("1÷2π")
        assertTrue(b is RowBox)
        val children = (b as RowBox).children
        assertEquals(3, children.size)
        assertTrue(children[0] is FracBox)
        assertTrue(children[2] is TextBox && (children[2] as TextBox).text == "π")
    }

    @Test
    fun `sup lifts content to keep ink inside box`() {
        val b = build("2^3")
        assertTrue(b is SupBox)
        val s = b as SupBox
        // 底 2：宽 10 基线 16；上标 3：scale 0.7 宽 7 高 14 基线 11.2
        assertEquals(17f, s.width, 1e-4f)
        // lift = max(0, 11.2 + 9 − 16) = 4.2 → 基线 20.2、高 24.2、sup 顶恰为 0
        assertEquals(20.2f, s.baseline, 1e-3f)
        assertEquals(24.2f, s.height, 1e-3f)
        assertEquals(0f, s.supTop, 1e-3f)
        assertEquals(4.2f, s.baseTop, 1e-3f)
    }

    @Test
    fun `nested power uses single level script scale`() {
        // 嵌套上标不二次缩小（有意决策：卡西欧真机只有一级脚本，与 TeX 惯例不同）
        val b = build("2^3^2")
        val outer = b as SupBox
        val inner = outer.sup as SupBox
        assertEquals(0.7f, (inner.sup as TextBox).scale, 1e-6f)
    }

    @Test
    fun `sqrt without and with index`() {
        val plain = build("√(2)")
        assertTrue(plain is SqrtBox)
        assertEquals(0.55f * em + 10f + 0.1f * em, plain.width, 1e-4f)

        val indexed = build("∛(5)")
        assertTrue(indexed is SqrtBox)
        val s = indexed as SqrtBox
        // index "3" 宽 7（scale 0.7）
        assertEquals(7f + 0.55f * em + 10f + 0.1f * em, s.width, 1e-4f)
    }

    @Test
    fun `factor parenthesizes add and neg`() {
        // 2×(3+4)：右操作数为加式 → 带括号
        val b = build("2×(3+4)")
        assertTrue(b is RowBox)
        val texts = collectTexts(b)
        assertEquals(listOf("2", " × ", "(", "3", " + ", "4", ")"), texts)
        // −3!：Neg 的因子位置
        val n = build("-3!")
        assertEquals(listOf("−", "3", "!"), collectTexts(n))
    }

    @Test
    fun `scientific literal natural display`() {
        val b = build("1.2E3")
        val texts = collectTexts(b)
        assertEquals(listOf("1.2", " × ", "10", "3"), texts)
        assertTrue(b is RowBox && (b as RowBox).children.last() is SupBox)
    }

    @Test
    fun `log with base has sub box`() {
        val b = build("log(2,16)")
        val boxes = flatten(b)
        assertTrue(boxes.any { it is SubBox })
        assertEquals(listOf("log", "2", "(", "16", ")"), collectTexts(b))
    }

    @Test
    fun `function and constants`() {
        assertEquals(listOf("sin", "(", "30", ")"), collectTexts(build("sin(30)")))
        assertEquals(listOf("sin⁻¹", "(", "0.5", ")"), collectTexts(build("asin(0.5)")))
        assertEquals(listOf("|", "2", " − ", "7", "|"), collectTexts(build("Abs(2-7)")))
        assertEquals(listOf("10", "P", "4"), collectTexts(build("10 nPr 4")))
        assertEquals(listOf("Ran#"), collectTexts(build("Ran#")))
    }

    @Test
    fun `multi statement joined with colon`() {
        val b = build("3+3:3×3")
        assertEquals(listOf("3", " + ", "3", " : ", "3", " × ", "3"), collectTexts(b))
    }

    @Test
    fun `row geometry aggregates`() {
        val b = build("2+3") as RowBox
        // 宽 = 10("2") + 30(" + " 三字符) + 10("3") = 50；高 = 16 + 4 = 20；基线 = 16
        assertEquals(50f, b.width, 1e-4f)
        assertEquals(20f, b.height, 1e-4f)
        assertEquals(16f, b.baseline, 1e-4f)
    }

    private fun collectTexts(b: MathBox): List<String> = flatten(b).filterIsInstance<TextBox>().map { it.text }

    private fun flatten(b: MathBox): List<MathBox> = when (b) {
        is RowBox -> listOf(b) + b.children.flatMap { flatten(it) }
        is FracBox -> listOf(b) + flatten(b.num) + flatten(b.den)
        is SupBox -> listOf(b) + flatten(b.base) + flatten(b.sup)
        is SubBox -> listOf(b) + flatten(b.base) + flatten(b.sub)
        is SqrtBox -> listOf(b) + (b.index?.let { flatten(it) } ?: emptyList()) + flatten(b.content)
        else -> listOf(b)
    }
}
