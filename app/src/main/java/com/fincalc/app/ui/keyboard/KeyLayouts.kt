package com.fincalc.app.ui.keyboard

import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode
import com.fincalc.app.ui.KEY_ALPHA_MARK
import com.fincalc.app.ui.KEY_SHIFT_MARK
import com.fincalc.app.ui.comp.CompController
import com.fincalc.app.ui.comp.Memory
import com.fincalc.app.ui.finance.FinanceController

/** 模式键面（FC-200V 风格）：12 模式直接切换，当前模式高亮 MODE_ACTIVE（2026-08-24 用户反馈 + 计划 7）。 */
fun modeKeyRows(state: CalcState): List<List<Key>> = listOf(
    listOf(Mode.SMPL, Mode.CMPD, Mode.CASH, Mode.AMRT, Mode.COMP, Mode.STAT).map { modeKey(state, it) },
    listOf(Mode.CNVR, Mode.COST, Mode.DAYS, Mode.DEPR, Mode.BOND, Mode.BEVN).map { modeKey(state, it) }
)

private fun modeKey(
    state: CalcState,
    m: Mode,
    alphaLetter: String? = null,
    insert: ((String) -> Unit)? = null
): Key = Key(
    m.name,
    onPress = { state.switchMode(m) },
    alphaLabel = alphaLetter,
    onAlphaPress = if (alphaLetter != null && insert != null) ({ insert(alphaLetter) }) else null,
    color = if (state.mode == m) KeyColor.MODE_ACTIVE else KeyColor.MODE
)

/** 顶部功能行（行0/行1）数据：4 个两键列表（TopFunctionRows 的左右上下）+ DPad 四向回调与旋转回调。 */
class Fc200vTopRows(
    val leftTop: List<Key>,
    val leftBottom: List<Key>,
    val rightTop: List<Key>,
    val rightBottom: List<Key>,
    val onUp: () -> Unit,
    val onDown: () -> Unit,
    val onLeft: () -> Unit,
    val onRight: () -> Unit,
    val onRotateCW: () -> Unit,
    val onRotateCCW: () -> Unit
)

/**
 * FC-200V 行0/行1：SHIFT ALPHA | SHORT CUT1/2 | SETUP ON | ESC SOLVE + 菱形方向盘。
 * comp 与 fin 二选一非空：COMP 模式传 comp，金融变量屏模式传 fin（行为映射见计划 7）。
 */
fun fc200vTopRows(
    state: CalcState,
    comp: CompController?,
    fin: FinanceController?,
    onOpenSettings: () -> Unit
): Fc200vTopRows {
    require((comp != null) != (fin != null)) { "comp 与 fin 必须二选一非空" }

    fun shortcutKey(slot: Int): Key {
        val bound = if (slot == 1) state.shortcut1 else state.shortcut2
        return Key(
            label = "SHORT\nCUT$slot",
            shiftLabel = "FMEM$slot",
            onPress = {
                state.clearModifiers()
                bound?.let { state.switchMode(it) }   // 未绑定→无操作
            },
            onShiftPress = { state.clearShortcut(slot); state.clearModifiers() },
            onLongPress = { state.bindShortcut(slot, state.mode); state.clearModifiers() },
            color = KeyColor.FUNC
        )
    }

    val esc = Key("ESC", onPress = {
        when {
            comp != null -> { state.clearModifiers(); comp.clear() }
            fin!!.editText != null -> fin.clear()          // 编辑中→取消编辑（clear 内部清修饰键）
            else -> state.switchMode(Mode.COMP)            // 否则回 COMP
        }
    }, color = KeyColor.FUNC)

    val solve = Key(
        "SOLVE", "S-MENU",
        onPress = { if (fin != null) fin.solve() else state.clearModifiers() },   // COMP 下无操作
        onShiftPress = { state.clearModifiers(); onOpenSettings() },
        color = KeyColor.BLUE
    )

    return Fc200vTopRows(
        leftTop = listOf(
            Key("SHIFT", onPress = { state.toggleShift() }, color = KeyColor.FUNC, labelColor = KEY_SHIFT_MARK),
            Key("ALPHA", onPress = { state.toggleAlpha() }, color = KeyColor.FUNC, labelColor = KEY_ALPHA_MARK)
        ),
        leftBottom = listOf(shortcutKey(1), shortcutKey(2)),
        rightTop = listOf(
            Key("SETUP", onPress = { state.clearModifiers(); onOpenSettings() }, color = KeyColor.FUNC),
            Key("ON", onPress = {
                if (comp != null) { state.clearModifiers(); comp.clear() }      // 清屏
                else state.switchMode(Mode.COMP)                                 // 金融模式：回 COMP
            }, color = KeyColor.FUNC)
        ),
        rightBottom = listOf(esc, solve),
        onUp = { if (comp != null) { state.clearModifiers(); comp.historyBack() } else fin!!.moveUp() },
        onDown = { if (comp != null) { state.clearModifiers(); comp.historyForward() } else fin!!.moveDown() },
        onLeft = { if (comp != null) { state.clearModifiers(); comp.moveLeft() } else state.clearModifiers() },   // 金融模式暂无行为
        onRight = { if (comp != null) { state.clearModifiers(); comp.moveRight() } else state.clearModifiers() },
        // 方向盘旋转手势：COMP 顺/逆时针=光标右/左移；金融模式=选中行下/上移（fin 的 select 内部先 clearModifiers）
        onRotateCW = { if (comp != null) { state.clearModifiers(); comp.moveRight() } else fin!!.moveDown() },
        onRotateCCW = { if (comp != null) { state.clearModifiers(); comp.moveLeft() } else fin!!.moveUp() }
    )
}

/**
 * FC-200V 行2-行8（键面总谱逐键对齐真机）：
 * 行2/3 模式键（当前模式高亮，行3 前四键带 ALPHA 红字 A/B/C/D）；
 * 行4 存储/括号/目录/M±；行5-8 数字运算区（5 列）。
 * comp 与 fin 二选一非空；shift/alpha 层插入文本与改造前 compKeys（已删除）一致。
 */
fun fc200vKeys(
    state: CalcState,
    comp: CompController?,
    fin: FinanceController?,
    onFinish: () -> Unit,
    onCatalog: () -> Unit,
    onVars: () -> Unit,
    onSto: () -> Unit,
    onRcl: () -> Unit
): List<List<Key>> {
    require((comp != null) != (fin != null)) { "comp 与 fin 必须二选一非空" }

    // 插入文本：COMP 入输入行，金融模式入 editText（两个控制器的 insert 内部都会 clearModifiers）
    val insertText: (String) -> Unit = { t -> comp?.insert(t) ?: fin!!.insert(t) }
    fun ins(text: String): () -> Unit = { insertText(text) }

    val clearAction: () -> Unit = {
        if (comp != null) { state.clearModifiers(); comp.clear() } else fin!!.clear()
    }

    val row2 = listOf(Mode.SMPL, Mode.CMPD, Mode.CASH, Mode.AMRT, Mode.COMP, Mode.STAT)
        .map { modeKey(state, it) }
    val row3 = listOf(
        modeKey(state, Mode.CNVR, "A", insertText),
        modeKey(state, Mode.COST, "B", insertText),
        modeKey(state, Mode.DAYS, "C", insertText),
        modeKey(state, Mode.DEPR, "D", insertText),
        modeKey(state, Mode.BOND),
        modeKey(state, Mode.BEVN)
    )

    val row4 = listOf(
        Key("(−)", onPress = ins("-"), color = KeyColor.OP),
        Key("RCL", "STO",
            onPress = { state.clearModifiers(); onRcl() },
            onShiftPress = { state.clearModifiers(); onSto() }),
        Key("(", "%", onPress = ins("("), onShiftPress = ins("%")),
        Key(")", ",", alphaLabel = "X",
            onPress = ins(")"), onShiftPress = ins(","), onAlphaPress = ins("X")),
        Key("CTLG", "VARS", alphaLabel = "Y",
            onPress = { state.clearModifiers(); onCatalog() },
            onShiftPress = { state.clearModifiers(); onVars() },
            onAlphaPress = ins("Y")),
        Key("M+", "M−", alphaLabel = "M",
            onPress = { state.clearModifiers(); Memory.memPlus(state) },
            onShiftPress = { state.clearModifiers(); Memory.memMinus(state) },
            onAlphaPress = ins("M"))
    )

    val row5 = listOf(
        Key("7", "eˣ", onPress = ins("7"), onShiftPress = ins("e^("), color = KeyColor.NUM),
        Key("8", "ln", onPress = ins("8"), onShiftPress = ins("ln("), color = KeyColor.NUM),
        Key("9", "CLR", onPress = ins("9"), onShiftPress = clearAction, color = KeyColor.NUM),
        Key("DEL", "INS",   // INS 仅印刷，不绑定（shift+DEL 回落主功能 delete）
            onPress = { if (comp != null) { state.clearModifiers(); comp.delete() } else fin!!.delete() },
            color = KeyColor.BLUE),
        Key("AC", "OFF",
            onPress = clearAction,
            onShiftPress = { state.clearModifiers(); onFinish() },
            color = KeyColor.BLUE)
    )

    val row6 = listOf(
        Key("4", "x²", onPress = ins("4"), onShiftPress = ins("²"), color = KeyColor.NUM),
        Key("5", "√(", onPress = ins("5"), onShiftPress = ins("√("), color = KeyColor.NUM),
        Key("6", "^(", onPress = ins("6"), onShiftPress = ins("^("), color = KeyColor.NUM),
        Key("×", onPress = ins("×"), color = KeyColor.OP),
        Key("÷", onPress = ins("÷"), color = KeyColor.OP)
    )

    val row7 = listOf(
        Key("1", "sin(", onPress = ins("1"), onShiftPress = ins("sin("), color = KeyColor.NUM),
        Key("2", "cos(", onPress = ins("2"), onShiftPress = ins("cos("), color = KeyColor.NUM),
        Key("3", "tan(", onPress = ins("3"), onShiftPress = ins("tan("), color = KeyColor.NUM),
        Key("+", onPress = ins("+"), color = KeyColor.OP),
        Key("−", onPress = ins("-"), color = KeyColor.OP)
    )

    val row8 = listOf(
        Key("0", "Rnd(", onPress = ins("0"), onShiftPress = ins("Rnd("), color = KeyColor.NUM),
        // Δ% 引擎不支持（core/expr 无该函数，仅后缀 %）——shiftLabel 仅印刷，不绑定
        Key(".", "Δ%", onPress = ins("."), color = KeyColor.NUM),
        Key("×10ˣ", "π", alphaLabel = "e",
            onPress = ins("E"), onShiftPress = ins("π"), onAlphaPress = ins("e"),
            color = KeyColor.NUM),
        Key("Ans", "DRG►",
            onPress = ins("Ans"),
            onShiftPress = { state.clearModifiers(); cycleAngle(state) }),
        Key("EXE", onPress = {
            if (comp != null) { state.clearModifiers(); comp.execute() } else fin!!.exe()
        })
    )

    return listOf(row2, row3, row4, row5, row6, row7, row8)
}

/** DRG►：角度单位按 AngleUnit 枚举序循环（DEG→RAD→GRA→DEG，真机 DRG► 行为）。 */
private fun cycleAngle(state: CalcState) {
    val units = AngleUnit.entries
    state.settings = state.settings.copy(angle = units[(state.settings.angle.ordinal + 1) % units.size])
}
