package com.fincalc.app.ui.keyboard

import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode
import com.fincalc.app.ui.comp.CompController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FC-200V 键面 smoke test（计划 7 Task 3）：行/列数、关键键位置、12 模式键齐全与当前模式高亮、
 * 顶部功能行结构（SOLVE 蓝键在右下）、SHORT CUT 键短按跳转/shift 清除行为。
 */
class KeyLayoutsTest {

    private fun compKeys(state: CalcState): List<List<Key>> = fc200vKeys(
        state, comp = CompController(state), fin = null,
        onFinish = {}, onCatalog = {}, onVars = {}, onSto = {}, onRcl = {}
    )

    private fun compTopRows(state: CalcState): Fc200vTopRows =
        fc200vTopRows(state, comp = CompController(state), fin = null, onOpenSettings = {})

    @Test
    fun `fc200vKeys returns 7 rows in 6-6-6-5-5-5-5 columns`() {
        val rows = compKeys(CalcState())
        assertEquals(7, rows.size)                                // 行2-行8 共 7 行
        rows.subList(0, 3).forEach { assertEquals(6, it.size) }   // 行2/3/4 各 6 列
        rows.subList(3, 7).forEach { assertEquals(5, it.size) }   // 行5-8 各 5 列
    }

    @Test
    fun `mode rows carry all 12 modes with current highlighted`() {
        val rows = compKeys(CalcState())                          // 默认模式 COMP
        val labels = (rows[0] + rows[1]).map { it.label }
        assertEquals(
            listOf(
                "SMPL", "CMPD", "CASH", "AMRT", "COMP", "STAT",
                "CNVR", "COST", "DAYS", "DEPR", "BOND", "BEVN"
            ),
            labels
        )
        assertEquals(Mode.entries.size, labels.size)              // 12 模式键齐全
        assertEquals(KeyColor.MODE_ACTIVE, rows[0].single { it.label == "COMP" }.color)
        assertEquals(KeyColor.MODE, rows[0].single { it.label == "SMPL" }.color)
    }

    @Test
    fun `key keys exist at expected rows`() {
        val rows = compKeys(CalcState())
        assertTrue(rows[2].any { it.label == "CTLG" })   // 行4 含 CTLG
        assertTrue(rows[3].any { it.label == "AC" })     // 行5 含 AC
        assertTrue(rows[6].any { it.label == "EXE" })    // 行8 含 EXE
    }

    @Test
    fun `top rows are four two-key lists with blue SOLVE at right bottom`() {
        val top = compTopRows(CalcState())
        listOf(top.leftTop, top.leftBottom, top.rightTop, top.rightBottom)
            .forEach { assertEquals(2, it.size) }
        val solve = top.rightBottom.single { it.label == "SOLVE" }
        assertEquals(KeyColor.BLUE, solve.color)
        assertEquals("S-MENU", solve.shiftLabel)
    }

    @Test
    fun `shortcut key short press jumps to bound mode, noop when unbound`() {
        val state = CalcState()
        // 未绑定→短按无操作（shortcutKey 在构键时捕获 bound，绑定后需重建——真机上由重组完成）
        compTopRows(state).leftBottom[0].onPress()
        assertEquals(Mode.COMP, state.mode)
        // 长按绑定（bindShortcut）后短按跳转到绑定模式
        state.bindShortcut(1, Mode.CMPD)
        compTopRows(state).leftBottom[0].onPress()
        assertEquals(Mode.CMPD, state.mode)
        // shift 清除绑定后回到无操作
        compTopRows(state).leftBottom[0].onShiftPress!!.invoke()
        assertNull(state.shortcut1)
        state.switchMode(Mode.COMP)
        compTopRows(state).leftBottom[0].onPress()
        assertEquals(Mode.COMP, state.mode)
    }

    @Test
    fun `shortcut long press binds current mode via key callback and clears modifiers`() {
        val state = CalcState()
        state.toggleShift()                                   // 武装 shift
        compTopRows(state).leftBottom[0].onLongPress!!.invoke()
        assertEquals(Mode.COMP, state.shortcut1)              // 长按绑定=当前模式（经键回调，非直接调 CalcState）
        assertEquals(false, state.shift)                      // 绑定后修饰态清除
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fc200vKeys rejects missing controllers`() {
        fc200vKeys(
            CalcState(), comp = null, fin = null,
            onFinish = {}, onCatalog = {}, onVars = {}, onSto = {}, onRcl = {}
        )
    }
}
