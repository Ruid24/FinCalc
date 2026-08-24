package com.fincalc.app.ui.comp

import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** COMP 控制器行为测试（Task 5 质量审查发现的缺陷回归锁定）。 */
class CompControllerTest {

    @Test
    fun `operator after exe continues with Ans`() {
        val c = CompController(CalcState())
        c.insert("2"); c.insert("+"); c.insert("3"); c.execute()   // 5
        c.insert("×"); c.insert("2")                               // Ans×2
        c.execute()
        assertEquals(10.0, c.result!!, 1e-12)
    }

    @Test
    fun `digit after exe starts fresh`() {
        val c = CompController(CalcState())
        c.insert("2"); c.execute()
        c.insert("5")
        assertEquals("5", c.input)
    }

    @Test
    fun `postfix after exe continues with Ans`() {
        // 审查发现：EXE 后按 x² 应得 Ans² 而非孤立 ²
        val c = CompController(CalcState())
        c.insert("3"); c.execute()   // 3
        c.insert("²")                // Ans²
        c.execute()
        assertEquals(9.0, c.result!!, 1e-12)
    }

    @Test
    fun `infix function after exe continues with Ans`() {
        val c = CompController(CalcState())
        c.insert("10"); c.execute()      // 10
        c.insert(" nCr "); c.insert("2") // Ans nCr 2
        c.execute()
        assertEquals(45.0, c.result!!, 1e-12)
    }

    @Test
    fun `shift is consumed after insert`() {
        val s = CalcState()
        val c = CompController(s)
        s.toggleShift()
        c.insert("³")
        assertFalse(s.shift)
    }

    @Test
    fun `error keeps input editable`() {
        val c = CompController(CalcState())
        c.insert("1"); c.insert("÷"); c.insert("0"); c.execute()
        assertEquals("Math ERROR", c.errorText)
        c.delete(); c.insert("2"); c.execute()
        assertEquals(0.5, c.result!!, 1e-12)
    }
}
