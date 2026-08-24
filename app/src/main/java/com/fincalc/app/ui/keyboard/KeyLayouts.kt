package com.fincalc.app.ui.keyboard

import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode

/** 模式键面（FC-200V 风格，置顶两行）：12 模式直接切换，不进二级菜单（2026-08-24 用户反馈）。 */
fun modeKeyRows(state: CalcState): List<List<Key>> = listOf(
    listOf(Mode.SMPL, Mode.CMPD, Mode.CASH, Mode.AMRT, Mode.COMP, Mode.STAT).map { modeKey(state, it) },
    listOf(Mode.CNVR, Mode.COST, Mode.DAYS, Mode.DEPR, Mode.BOND, Mode.BEVN).map { modeKey(state, it) }
)

private fun modeKey(state: CalcState, m: Mode): Key =
    Key(m.name, onPress = { state.switchMode(m) })
