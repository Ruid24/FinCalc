package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SmplTest {
    @Test
    fun `manual example 365 basis`() {
        // 说明书例1（L1424-1474）：Set=365，Dys=120，I%=5，PV=10000
        // 说明书 10 位显示 SI=-164.3835616、SFV=-10164.38356；此处按全精度期望值断言
        assertEquals(-164.3835616438356, Smpl.si(120.0, 5.0, 10000.0, 365), 1e-9)
        assertEquals(-10164.383561643835, Smpl.sfv(120.0, 5.0, 10000.0, 365), 1e-9)
    }

    @Test
    fun `basis 360`() {
        // SI′ = 120/360 × 10000 × 0.05 = 166.66…
        assertEquals(-166.66666666666666, Smpl.si(120.0, 5.0, 10000.0, 360), 1e-9)
        assertEquals(-10166.666666666666, Smpl.sfv(120.0, 5.0, 10000.0, 360), 1e-9)
    }

    @Test
    fun `sign convention for negative pv`() {
        // 借入（PV 为负）时符号翻转
        assertEquals(164.3835616438356, Smpl.si(120.0, 5.0, -10000.0, 365), 1e-9)
    }

    @Test
    fun `invalid days basis throws argument error`() {
        val e = assertThrows(CalcException::class.java) { Smpl.si(120.0, 5.0, 10000.0, 363) }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
    }
}
