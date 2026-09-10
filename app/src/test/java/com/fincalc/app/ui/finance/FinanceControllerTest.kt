package com.fincalc.app.ui.finance

import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // ── 即输即改（计划 7 Task 4.3）──

    @Test
    fun `insert commits value immediately without exe`() {
        val s = CalcState()
        val c = costController(s)
        c.insert("4")
        assertEquals(4.0, s.getVar("CST"), 0.0)      // 未按 EXE 已写回
        c.insert("0")
        assertEquals(40.0, s.getVar("CST"), 0.0)
        assertEquals("40", c.editText)               // 保持编辑态，可继续输入
    }

    @Test
    fun `invalid intermediate input keeps old value silently`() {
        val s = CalcState()
        val c = costController(s)
        c.insert("1"); c.insert("5")
        assertEquals(15.0, s.getVar("CST"), 0.0)
        c.insert("-")                                 // "15-" 非法中间态
        assertEquals(15.0, s.getVar("CST"), 0.0)      // 旧值保持
        assertNull(c.errorText)                       // 静默不报错
        assertEquals("15-", c.editText)               // 输入不清
        c.insert("5")                                 // "15-5" 恢复合法
        assertEquals(10.0, s.getVar("CST"), 0.0)
    }

    @Test
    fun `moveDown commits pending edit`() {
        val s = CalcState()
        val c = costController(s)
        c.insert("4"); c.insert("2")                  // 未按 EXE
        c.moveDown()
        assertEquals(42.0, s.getVar("CST"), 0.0)      // 移动时自动提交
        assertNull(c.editText)
        assertEquals(1, c.selected)
    }

    @Test
    fun `integer row rounds on insert`() {
        val s = CalcState()
        val (spec, _) = com.fincalc.app.ui.finance.modes.smplSpec(s)
        val c = FinanceController(s, spec) { 0.0 }
        listOf("1", "2", ".", "6").forEach(c::insert)
        assertEquals(13.0, s.getVar("Dys"), 0.0)      // 12.6 即时取整 → 13（未按 EXE）
    }

    // ── 自引用快照（Task 4 质量审查 B1 修复）──
    // 金融变量键（n、I%、PV…）不在表达式变量体系内（tokenizer 仅认 ABCDXYM/Ans/e），
    // 真实自引用不可达；此处用手工 spec（key=A）直接测试快照机制本身。

    private fun varController(s: CalcState, key: String): FinanceController {
        val spec = ModeScreenSpec("TEST", listOf(
            FinanceVar(key, key, solvable = false, formula = ""),
            FinanceVar("B", "B", solvable = false, formula = "")
        ))
        return FinanceController(s, spec) { 0.0 }
    }

    @Test
    fun `self referencing expression is idempotent on exe`() {
        // 审查 B1：A=10 输入 A+1，即输即改写回 11；EXE 不得以新值重复求值成 12（基准=编辑前快照）
        val s = CalcState()
        s.setVar("A", 10.0)
        val c = varController(s, "A")
        c.insert("A"); c.insert("+"); c.insert("1")
        assertEquals(11.0, s.getVar("A"), 1e-9)       // 即输即改：10+1
        c.exe()
        assertEquals(11.0, s.getVar("A"), 1e-9)       // EXE 幂等
    }

    @Test
    fun `self referencing expression is idempotent on row switch`() {
        val s = CalcState()
        s.setVar("A", 10.0)
        val c = varController(s, "A")
        c.insert("A"); c.insert("+"); c.insert("1")
        c.moveDown()                                  // 切行提交同样幂等
        assertEquals(11.0, s.getVar("A"), 1e-9)
    }
}
