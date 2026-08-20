package com.fincalc.app.core.finance

import com.fincalc.app.core.expr.CalcException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DaysTest {
    private val d1 = Days.Date(2022, 11, 5)
    private val d2 = Days.Date(2023, 4, 27)

    @Test
    fun `manual example 1 days between 365 basis`() {
        // 说明书例1（L2237-2262）：365 基准
        assertEquals(173, Days.daysBetween(d1, d2, days360 = false))
    }

    @Test
    fun `same example 360 basis`() {
        // 30/360：360×(2023−2022) + 30×(4−11) + (27−5) = 172
        assertEquals(172, Days.daysBetween(d1, d2, days360 = true))
    }

    @Test
    fun `31st day rule on 360 basis`() {
        // CN-77 L2225：d1 为 31 日→当月 30 日；d2 为 31 日→次月 1 日
        // 2022-01-31 → 2022-03-31：d1→30，d2→4/1：360×0 + 30×(4−1) + (1−30) = 61
        assertEquals(61, Days.daysBetween(Days.Date(2022, 1, 31), Days.Date(2022, 3, 31), true))
        // 跨年变体：2022-01-31 → 2023-03-31：d2→2023-04-01：360×1 + 30×(4−1) + (1−30) = 421
        assertEquals(421, Days.daysBetween(Days.Date(2022, 1, 31), Days.Date(2023, 3, 31), true))
    }

    @Test
    fun `leap year handling`() {
        // 2024 为闰年：2/1 → 3/1 实际 29 天
        assertEquals(29, Days.daysBetween(Days.Date(2024, 2, 1), Days.Date(2024, 3, 1), false))
        // 2023 非闰年：28 天
        assertEquals(28, Days.daysBetween(Days.Date(2023, 2, 1), Days.Date(2023, 3, 1), false))
    }

    @Test
    fun `manual example 2 and 3 date plus minus days`() {
        // 说明书例2/例3（L2275-2288，365 模式）：d1+173=d2；d2−173=d1
        assertEquals(d2, Days.plusDays(d1, 173))
        assertEquals(d1, Days.minusDays(d2, 173))
    }

    @Test
    fun `plus days zero and negative`() {
        assertEquals(d1, Days.plusDays(d1, 0))
        assertEquals(Days.Date(2022, 11, 4), Days.plusDays(d1, -1))
    }

    @Test
    fun `parse and format mdy`() {
        // CN-22：MDY 月日年
        assertEquals(Days.Date(2022, 11, 5), Days.parse(11052022.0, Days.DateFormat.MDY))
        assertEquals(11052022.0, Days.format(d1, Days.DateFormat.MDY), 0.0)
        // 前导零月份
        assertEquals(Days.Date(2022, 6, 1), Days.parse(6012022.0, Days.DateFormat.MDY))
    }

    @Test
    fun `parse and format dmy`() {
        // CN-22：DMY 日月年
        assertEquals(Days.Date(2022, 11, 5), Days.parse(5112022.0, Days.DateFormat.DMY))
        assertEquals(5112022.0, Days.format(d1, Days.DateFormat.DMY), 0.0)
    }

    @Test
    fun `invalid dates throw argument error`() {
        assertThrows(CalcException::class.java) { Days.parse(1332022.0, Days.DateFormat.MDY) }   // 1 月 33 日
        assertThrows(CalcException::class.java) { Days.parse(13122022.0, Days.DateFormat.MDY) }  // 13 月
        assertThrows(CalcException::class.java) { Days.parse(2292023.0, Days.DateFormat.MDY) }   // 2023-02-29
        assertThrows(CalcException::class.java) { Days.parse(1011900.0, Days.DateFormat.MDY) }   // 1900 超范围
        assertThrows(CalcException::class.java) { Days.parse(1012100.0, Days.DateFormat.MDY) }   // 2100 超范围
        assertThrows(CalcException::class.java) { Days.parse(11052022.5, Days.DateFormat.MDY) }  // 非整数
        val e = assertThrows(CalcException::class.java) { Days.parse(13122022.0, Days.DateFormat.MDY) }
        assertEquals(CalcException.Kind.ARGUMENT, e.kind)
    }

    @Test
    fun `jdn round trip over range`() {
        // JDN 往返一致性（覆盖闰年/世纪年；步长取质数 97 天增加覆盖；保持在合法范围内）
        var d = Days.Date(1901, 1, 1)
        val end = Days.Date(2099, 12, 31)
        val safeEnd = Days.minusDays(end, 100)
        while (d <= safeEnd) {
            assertEquals(d, Days.minusDays(Days.plusDays(d, 100), 100))
            d = Days.plusDays(d, 97)
        }
    }
}
