package com.fincalc.app.core.format

import com.fincalc.app.core.expr.DisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {
    @Test
    fun `norm1 examples from manual`() {
        // 说明书 CN-25/26：1÷6 = 0.1666666667（Norm1 10 位）
        assertEquals("0.1666666667", NumberFormatter.format(1.0 / 6, DisplayMode.Norm1))
        // 1÷200 = 5E-3（Norm1 转指数）；0.005（Norm2 不转）
        assertEquals("5E-3", NumberFormatter.format(1.0 / 200, DisplayMode.Norm1))
        assertEquals("0.005", NumberFormatter.format(1.0 / 200, DisplayMode.Norm2))
    }

    @Test
    fun `norm large magnitude switches at 1e10`() {
        assertEquals("1234567890", NumberFormatter.format(1234567890.0, DisplayMode.Norm1))
        // 12345678905 → 10 位有效数字进位（第 11 位为 5）→ 1.234567891E10
        assertEquals("1.234567891E10", NumberFormatter.format(12345678905.0, DisplayMode.Norm1))
    }

    @Test
    fun `norm strips trailing zeros`() {
        assertEquals("0.4", NumberFormatter.format(0.4, DisplayMode.Norm1))
        assertEquals("-3.75", NumberFormatter.format(-3.75, DisplayMode.Norm1))
        assertEquals("100", NumberFormatter.format(100.0, DisplayMode.Norm2))
    }

    @Test
    fun `fix mode`() {
        // 说明书 CN-25：100÷7 = 14.286（Fix3）、14.29（Fix2）
        assertEquals("14.286", NumberFormatter.format(100.0 / 7, DisplayMode.Fix(3)))
        assertEquals("14.29", NumberFormatter.format(100.0 / 7, DisplayMode.Fix(2)))
        assertEquals("-2.68", NumberFormatter.format(-2.675, DisplayMode.Fix(2)))
        assertEquals("5", NumberFormatter.format(4.5, DisplayMode.Fix(0)))
    }

    @Test
    fun `sci mode`() {
        // 说明书 CN-26：1÷7 = 1.4286E-1（Sci5）
        assertEquals("1.4286E-1", NumberFormatter.format(1.0 / 7, DisplayMode.Sci(5)))
        assertEquals("1.428571429E-1", NumberFormatter.format(1.0 / 7, DisplayMode.Sci(10)))
        assertEquals("-1.2E3", NumberFormatter.format(-1200.0, DisplayMode.Sci(2)))
    }

    @Test
    fun `edge cases`() {
        assertEquals("0", NumberFormatter.format(0.0, DisplayMode.Norm1))
        assertEquals("Math ERROR", NumberFormatter.format(Double.NaN, DisplayMode.Norm1))
        assertEquals("Math ERROR", NumberFormatter.format(Double.POSITIVE_INFINITY, DisplayMode.Norm1))
    }
}
