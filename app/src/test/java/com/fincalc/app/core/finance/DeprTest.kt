package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DeprTest {
    // 说明书例（L2324-2373）：n=6、PV=150000、FV=0、j=3、YR1=2；FP 用 I%=25、DB 用 I%=200
    private val n = 6
    private val pv = 150000.0
    private val fv = 0.0
    private val j = 3
    private val yr1 = 2

    @Test
    fun `manual example 1 straight line`() {
        val r = Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, j, yr1)
        assertEquals(25000.0, r.depreciation, 1e-9)          // 原文（L2352）
        assertEquals(95833.33333333334, r.rdv, 1e-6)         // 参考值（OCR 误作 95855 已证伪）
    }

    @Test
    fun `sl year 1 and n+1 proration`() {
        // SL₁ = 25000×2/12；SL₇（n+1 年）= 25000×10/12
        assertEquals(4166.666666666667, Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, 1, yr1).depreciation, 1e-9)
        val last = Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, n + 1, yr1)
        assertEquals(20833.333333333332, last.depreciation, 1e-9)
        assertEquals(0.0, last.rdv, 1e-6)                    // 折完归零
    }

    @Test
    fun `manual example 2 fixed percent`() {
        val r = Depr.depreciate(Depr.Method.FP, n, 25.0, pv, fv, j, yr1)
        assertEquals(26953.125, r.depreciation, 1e-9)
        assertEquals(80859.375, r.rdv, 1e-9)
    }

    @Test
    fun `manual example 3 sum of years digits`() {
        val r = Depr.depreciate(Depr.Method.SYD, n, 0.0, pv, fv, j, yr1)
        assertEquals(34523.80952380953, r.depreciation, 1e-6)
        assertEquals(66666.66666666669, r.rdv, 1e-6)
        // SYD₁ = 6/21×2/12×150000
        assertEquals(7142.857142857142, Depr.depreciate(Depr.Method.SYD, n, 0.0, pv, fv, 1, yr1).depreciation, 1e-9)
    }

    @Test
    fun `manual example 4 declining balance ddb`() {
        val r = Depr.depreciate(Depr.Method.DB, n, 200.0, pv, fv, j, yr1)
        assertEquals(31481.48148148148, r.depreciation, 1e-9)
        assertEquals(62962.962962962956, r.rdv, 1e-9)
        assertEquals(8333.333333333334, Depr.depreciate(Depr.Method.DB, n, 200.0, pv, fv, 1, yr1).depreciation, 1e-9)
    }

    @Test
    fun `yr1 12 means no proration and no extra year`() {
        val r = Depr.depreciate(Depr.Method.SL, 5, 0.0, 10000.0, 0.0, 2, 12)
        assertEquals(2000.0, r.depreciation, 1e-12)
        assertThrows(CalcException::class.java) {
            Depr.depreciate(Depr.Method.SL, 5, 0.0, 10000.0, 0.0, 6, 12)
        }
    }

    @Test
    fun `error conditions`() {
        // CN-168：负 PV/FV/I% → MATH；n>255 → MATH；j 超范围 → MATH；YR1>12 → ARGUMENT
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, n, 0.0, -1.0, fv, j, yr1) }
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, n, 0.0, pv, -1.0, j, yr1) }
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.FP, n, -25.0, pv, fv, j, yr1) }
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, 256, 0.0, pv, fv, j, yr1) }
        assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, n + 2, yr1) }
        val e = assertThrows(CalcException::class.java) { Depr.depreciate(Depr.Method.SL, n, 0.0, pv, fv, j, 13) }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
    }
}
