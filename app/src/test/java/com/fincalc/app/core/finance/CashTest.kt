package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CashTest {
    private val cfs = listOf(-10000.0, -1000.0, 4500.0, 5000.0, 4000.0)

    @Test
    fun `manual example npv`() {
        // 说明书例1（L1748-1796）：I%=3 → NPV=1400.464293（原文精确值）
        assertEquals(1400.464293, Cash.npv(3.0, cfs), 1e-6)
    }

    @Test
    fun `manual example nfv`() {
        // NFV = NPV × 1.03^4（L1844-1848）
        assertEquals(1576.2349, Cash.nfv(3.0, cfs), 1e-6)
    }

    @Test
    fun `manual example irr`() {
        // 说明书例2（L1800-1804，答案为截图）：按公式体系参考值 7.443619297
        assertEquals(7.443619297, Cash.irr(cfs), 1e-6)
    }

    @Test
    fun `manual example pbp`() {
        // 说明书例3（L1806-1808，答案为截图）：NPV₃=−2153.48、NPV₄=+1400.46 → 3.605941275
        assertEquals(3.605941275, Cash.pbp(3.0, cfs), 1e-9)
    }

    @Test
    fun `irr writes percent scale`() {
        // IRR 返回百分比（7.44 而非 0.0744）
        val r = Cash.irr(cfs)
        assertEquals(7.443619297, r, 1e-6)
    }

    @Test
    fun `irr same sign throws math error`() {
        // CN-168：所有收入/付款值符号相同 → Math ERROR
        assertThrows(CalcException::class.java) { Cash.irr(listOf(1.0, 2.0, 3.0)) }
        assertThrows(CalcException::class.java) { Cash.irr(listOf(-1.0, -2.0)) }
    }

    @Test
    fun `irr result below minus 50 throws math error`() {
        // CF=[-1, 0.4]：IRR = -60% ≤ -50 → Math ERROR（CN-168）
        assertThrows(CalcException::class.java) { Cash.irr(listOf(-1.0, 0.4)) }
    }

    @Test
    fun `pbp zero when cf0 non negative`() {
        assertEquals(0.0, Cash.pbp(3.0, listOf(100.0, -50.0, -50.0)), 0.0)
    }

    @Test
    fun `pbp simple payback at zero rate`() {
        // I=0 时退化为单回收期（CN-60）：[-100, 30, 40, 50] → 2.6 年
        assertEquals(2.6, Cash.pbp(0.0, listOf(-100.0, 30.0, 40.0, 50.0)), 1e-12)
    }

    @Test
    fun `pbp never recovers throws math error`() {
        // I%=50 时例题现金流 NPV 恒为负
        assertThrows(CalcException::class.java) { Cash.pbp(50.0, cfs) }
    }

    @Test
    fun `item count validation`() {
        assertThrows(CalcException::class.java) { Cash.npv(3.0, emptyList()) }
        assertThrows(CalcException::class.java) { Cash.npv(3.0, List(81) { 1.0 }) }
    }

    @Test
    fun `max 80 items accepted`() {
        // 80 项合法（CF₀~CF₇₉）
        val flows = List(80) { if (it == 0) -1.0 else 1.0 }
        Cash.npv(3.0, flows)
    }

    @Test
    fun `rate at or below minus 100 throws math error`() {
        // CN-168：I% ≤ −100 → Math ERROR（终审发现 npv/nfv/pbp 曾漏校验，静默返回 Infinity）
        assertThrows(CalcException::class.java) { Cash.npv(-100.0, cfs) }
        assertThrows(CalcException::class.java) { Cash.nfv(-101.0, cfs) }
        assertThrows(CalcException::class.java) { Cash.pbp(-100.0, cfs) }
    }
}
