package com.fincalc.app.state

import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.core.expr.DisplayMode
import com.fincalc.app.core.expr.ExprEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcStateTest {
    @Test
    fun `eval context wires vars and ans`() {
        val s = CalcState()
        ExprEngine.eval("3×4", s.exprContext())          // Ans=12
        assertEquals(12.0, s.getVar("Ans"), 0.0)
        ExprEngine.eval("Ans÷30", s.exprContext())       // 0.4
        assertEquals(0.4, s.getVar("Ans"), 1e-12)
        ExprEngine.eval("2A", s.exprContext())           // A 未赋值 → 0
        s.setVar("A", 5.0)
        assertEquals(10.0, ExprEngine.eval("2A", s.exprContext()), 1e-12)
        // Pol 写回 X/Y 直通 state
        ExprEngine.eval("Pol(√(2),√(2))", s.exprContext())
        assertEquals(2.0, s.getVar("X"), 1e-9)
        assertEquals(45.0, s.getVar("Y"), 1e-9)
    }

    @Test
    fun `settings flow through to context`() {
        val s = CalcState(Settings(angle = AngleUnit.RAD))
        assertEquals(-1.0, ExprEngine.eval("cos(π)", s.exprContext()), 1e-9)
        val s2 = CalcState(Settings(display = DisplayMode.Fix(3)))
        assertEquals(28.571, ExprEngine.eval("Rnd(200÷7)", s2.exprContext()), 1e-12)
    }

    @Test
    fun `on evaluated records history with cap`() {
        val s = CalcState()
        repeat(60) { s.onEvaluated("$it", it.toDouble()) }
        assertEquals(CalcState.HISTORY_CAP, s.history.size)
        assertEquals(59.0, s.history.last().result, 0.0)
    }

    @Test
    fun `history navigation`() {
        val s = CalcState()
        s.onEvaluated("1+1", 2.0)
        s.onEvaluated("2+2", 4.0)
        assertEquals(4.0, s.historyBack()!!.result, 0.0)   // 最新
        assertEquals(2.0, s.historyBack()!!.result, 0.0)   // 次新
        assertEquals(2.0, s.historyBack()!!.result, 0.0)   // 到头停住
        assertEquals(4.0, s.historyForward()!!.result, 0.0)
        assertNull(s.historyForward())                     // 越过最新回到 null
    }

    @Test
    fun `mode switch resets shift`() {
        val s = CalcState()
        s.toggleShift()
        s.setMode(Mode.CMPD)
        assertEquals(false, s.shift)
        assertEquals(Mode.CMPD, s.mode)
    }
}
