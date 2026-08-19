package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AmrtTest {

    // ---- 说明书例（L1929-1989）：End，PM1=15，PM2=28，I%=2，PV=100000，PMT=−920，P/Y=C/Y=12 ----
    // 五个答案在说明书中为截图；期望值为按 CN-69/70 公式体系的高精度计算参考值
    // （BAL₀=PV 为工程推断，已经 ΣPRN = BAL₁₄−BAL₂₈ 自洽验证）

    private val r = Amrt.amortize(15, 28, 2.0, 100000.0, -920.0, 12, 12, Cmpd.Payment.END)

    @Test
    fun `manual example bal`() {
        assertEquals(78425.13934866441, r.bal, 1e-6)
    }

    @Test
    fun `manual example int and prn`() {
        assertEquals(-148.89718761877987, r.int, 1e-9)
        assertEquals(-771.1028123812201, r.prn, 1e-9)
    }

    @Test
    fun `manual example sums`() {
        assertEquals(-1966.8267773985376, r.sumInt, 1e-6)
        assertEquals(-10913.173222601463, r.sumPrn, 1e-6)
    }

    @Test
    fun `sums are consistent with payment count`() {
        // 每期付款 = 利息 + 本金：ΣINT + ΣPRN = PMT × 期数
        assertEquals(-920.0 * 14, r.sumInt + r.sumPrn, 1e-6)
    }

    @Test
    fun `begin payment first period special case`() {
        // CN-70：Begin 时 INT₁ = 0、PRN₁ = PMT（自构造用例）
        val b = Amrt.amortize(1, 2, 12.0, 1000.0, -100.0, 1, 1, Cmpd.Payment.BEGIN)
        assertEquals(0.0, b.int, 0.0)
        assertEquals(-100.0, b.prn, 0.0)
        assertEquals(908.0, b.bal, 1e-12)
        assertEquals(-108.0, b.sumInt, 1e-12)
        assertEquals(-92.0, b.sumPrn, 1e-12)
    }

    @Test
    fun `py differs from cy rate conversion`() {
        // CN-70：P/Y≠C/Y 时先做名义→实际转换（与 CMPD 同一公式），此处只验证通路不报错
        Amrt.amortize(1, 2, 6.0, 1000.0, -100.0, 12, 2, Cmpd.Payment.END)
    }

    @Test
    fun `invalid pm range throws argument error`() {
        assertThrows(CalcException::class.java) {
            Amrt.amortize(28, 15, 2.0, 100000.0, -920.0, 12, 12, Cmpd.Payment.END)
        }
        assertThrows(CalcException::class.java) {
            Amrt.amortize(15, 15, 2.0, 100000.0, -920.0, 12, 12, Cmpd.Payment.END)
        }
        assertThrows(CalcException::class.java) {
            Amrt.amortize(0, 15, 2.0, 100000.0, -920.0, 12, 12, Cmpd.Payment.END)
        }
    }
}
