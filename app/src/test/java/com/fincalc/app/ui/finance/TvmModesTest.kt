package com.fincalc.app.ui.finance

import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.modes.bondSpec
import com.fincalc.app.ui.finance.modes.cmpdSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class TvmModesTest {

    @Test
    fun `cmpd manual example through controller`() {
        // 说明书例1：n=48、I%=4、PV=−1000、PMT=−300 → FV（P/Y=C/Y=12，与核心 CmpdTest 同源）
        val s = CalcState()
        s.settings = s.settings.copy(periodsPerYear = 12)
        val (spec, solver) = cmpdSpec(s)
        val c = FinanceController(s, spec, solver)
        c.insert("4"); c.insert("8"); c.exe()                    // n=48
        c.moveDown(); c.insert("4"); c.exe()                      // I%=4
        c.moveDown(); c.insert("-"); c.insert("1"); c.insert("0"); c.insert("0"); c.insert("0"); c.exe()  // PV=−1000
        c.moveDown(); c.insert("-"); c.insert("3"); c.insert("0"); c.insert("0"); c.exe()                 // PMT=−300
        c.moveDown(); c.solve()                                   // FV
        assertEquals(16761.07896780279, s.getVar("FV"), 1e-4)
    }

    @Test
    fun `bond term mode through controller`() {
        // 例3：Term、n=3、RDV=100、CPN=3、YLD=4 → PRC
        // 变量顺序（说明书 CN-86 表：n、RDV、CPN、PRC、YLD）——YLD 在 PRC 之后，输入 YLD 后回选 PRC 求解
        val s = CalcState()
        s.settings = s.settings.copy(bondTerm = true)
        val (spec, solver) = bondSpec(s)
        val c = FinanceController(s, spec, solver)
        c.insert("3"); c.exe()                                    // n=3
        c.moveDown(); c.insert("1"); c.insert("0"); c.insert("0"); c.exe()  // RDV=100
        c.moveDown(); c.insert("3"); c.exe()                      // CPN=3
        c.moveDown(); c.moveDown(); c.insert("4"); c.exe()        // YLD=4（跳过 PRC 行）
        c.moveUp(); c.solve()                                     // PRC
        assertEquals(-97.22490896677286, s.getVar("PRC"), 1e-9)
    }
}
