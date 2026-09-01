package com.fincalc.app.ui.comp

import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryTest {
    @Test
    fun `store recall and mem plus minus`() {
        val s = CalcState()
        ExprEngine.eval("3×4", s.exprContext())          // Ans=12
        Memory.store(s, "A")
        assertEquals(12.0, s.getVar("A"), 0.0)
        Memory.memPlus(s)                                // M += 12
        Memory.memPlus(s)                                // M += 12
        assertEquals(24.0, s.getVar("M"), 0.0)
        Memory.memMinus(s)                               // M -= 12
        assertEquals(12.0, s.getVar("M"), 0.0)
        // Ans 未被存储器操作改变
        assertEquals(12.0, s.getVar("Ans"), 0.0)
    }
}
