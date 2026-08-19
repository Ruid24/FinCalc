package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CnvrTest {
    @Test
    fun `manual example apr to eff`() {
        // 说明书例1（L2066-2088）：n=6，APR=3 → EFF
        assertEquals(3.0377509393765045, Cnvr.eff(3.0, 6.0), 1e-9)
    }

    @Test
    fun `eff to apr round trip`() {
        // 说明书例2（L2090-2094）：EFF→APR 演示可逆性
        assertEquals(3.0, Cnvr.apr(3.0377509393765045, 6.0), 1e-9)
    }

    @Test
    fun `n equals one`() {
        assertEquals(5.0, Cnvr.eff(5.0, 1.0), 1e-12)
    }

    @Test
    fun `non positive n throws math error`() {
        assertThrows(CalcException::class.java) { Cnvr.eff(3.0, 0.0) }
        assertThrows(CalcException::class.java) { Cnvr.apr(3.0, -1.0) }
    }
}
