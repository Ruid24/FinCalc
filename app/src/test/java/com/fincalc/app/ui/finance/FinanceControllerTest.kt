package com.fincalc.app.ui.finance

import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceControllerTest {

    private fun costController(state: CalcState): FinanceController {
        val (spec, solver) = com.fincalc.app.ui.finance.modes.costSpec(state)
        return FinanceController(state, spec, solver)
    }

    @Test
    fun `navigate and edit and store`() {
        val s = CalcState()
        val c = costController(s)
        // 选中 CST（第 0 行），输入 40，EXE 存入
        c.insert("4"); c.insert("0"); c.exe()
        assertEquals(40.0, s.getVar("CST"), 0.0)
        // ▼ 到 SEL，输入 100，EXE
        c.moveDown()
        c.insert("1"); c.insert("0"); c.insert("0"); c.exe()
        assertEquals(100.0, s.getVar("SEL"), 0.0)
        // ▼ 到 MRG，SOLVE → 60
        c.moveDown()
        c.solve()
        assertEquals(60.0, s.getVar("MRG"), 1e-12)
        assertEquals("MRG = 60", c.resultText)
    }

    @Test
    fun `expression input allowed`() {
        // CN-56：输入值允许表达式（16 个月 20 天 → 20÷30+16）
        val s = CalcState()
        val (spec, _) = com.fincalc.app.ui.finance.modes.smplSpec(s)
        val c = FinanceController(s, spec) { 0.0 }
        c.insert("2"); c.insert("0"); c.insert("÷"); c.insert("3"); c.insert("0"); c.insert("+"); c.insert("1"); c.insert("6")
        c.exe()
        // Dys 为整数输入（integer=true）：表达式照常求值，EXE 存入时取整 → 17
        assertEquals(17.0, s.getVar("Dys"), 1e-9)
    }

    @Test
    fun `solve non solvable shows error`() {
        val s = CalcState()
        val (spec, _) = com.fincalc.app.ui.finance.modes.smplSpec(s)
        val c = FinanceController(s, spec) { 0.0 }
        c.select(0)  // Dys 不可解
        c.solve()
        assertEquals("Math ERROR", c.errorText)
    }

    @Test
    fun `bad expression shows error and keeps editing`() {
        val s = CalcState()
        val c = costController(s)
        c.insert("1"); c.insert("÷"); c.insert("0"); c.exe()
        assertEquals("Math ERROR", c.errorText)
        assertEquals("1÷0", c.editText)
    }
}
