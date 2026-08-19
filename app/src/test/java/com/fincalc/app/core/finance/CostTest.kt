package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CostTest {
    @Test
    fun `manual example mrg`() {
        // 说明书设定值表（L2137）：CST=40，SEL=100 → MRG=60（占售价，非加价率）
        assertEquals(60.0, Cost.mrg(40.0, 100.0), 1e-12)
    }

    @Test
    fun `manual example reverse directions`() {
        assertEquals(40.0, Cost.cst(100.0, 60.0), 1e-12)
        assertEquals(100.0, Cost.sel(40.0, 60.0), 1e-12)
    }

    @Test
    fun `zero margin`() {
        assertEquals(0.0, Cost.mrg(50.0, 50.0), 0.0)
        assertEquals(50.0, Cost.sel(50.0, 0.0), 0.0)
    }

    @Test
    fun `math errors on division by zero`() {
        assertThrows(CalcException::class.java) { Cost.sel(40.0, 100.0) }
        assertThrows(CalcException::class.java) { Cost.mrg(40.0, 0.0) }
    }
}
