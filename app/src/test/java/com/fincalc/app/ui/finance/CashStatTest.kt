package com.fincalc.app.ui.finance

import com.fincalc.app.ui.finance.modes.CashController
import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashStatTest {

    @Test
    fun `cash manual example through controller`() {
        // 说明书例（L1742）：CF=[−10000,−1000,4500,5000,4000]，I%=3
        val s = CalcState()
        s.setVar("I%", 3.0)
        val c = CashController(s)
        listOf("-10000", "-1000", "4500", "5000", "4000").forEach { c.addRow(); c.editRow(c.rows.size - 1, it) }
        c.solve("NPV")
        assertTrue(c.resultText!!.startsWith("NPV = 1400.464293"))
        c.solve("IRR")
        assertTrue(c.resultText!!.startsWith("IRR = 7.443619297"))
        c.solve("PBP")
        assertTrue(c.resultText!!.startsWith("PBP = 3.605941275"))
    }

    @Test
    fun `cash bad number shows error`() {
        val s = CalcState()
        val c = CashController(s)
        c.addRow(); c.editRow(0, "abc")
        c.solve("NPV")
        assertEquals("Syntax ERROR", c.errorText)
    }
}
