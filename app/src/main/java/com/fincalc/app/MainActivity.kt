package com.fincalc.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.core.finance.Stat
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.data.Prefs
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode
import com.fincalc.app.ui.comp.CompController
import com.fincalc.app.ui.comp.CompScreen
import com.fincalc.app.ui.dialogs.ModeDialog
import com.fincalc.app.ui.dialogs.SettingsDialog
import com.fincalc.app.ui.editor.ListEditor
import com.fincalc.app.ui.finance.FinanceController
import com.fincalc.app.ui.finance.FinanceScreen
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec
import com.fincalc.app.ui.finance.modes.BevnSub
import com.fincalc.app.ui.finance.modes.CashController
import com.fincalc.app.ui.finance.modes.StatController
import com.fincalc.app.ui.finance.modes.amrtSpec
import com.fincalc.app.ui.finance.modes.bevnSpec
import com.fincalc.app.ui.finance.modes.bondSpec
import com.fincalc.app.ui.finance.modes.cmpdSpec
import com.fincalc.app.ui.finance.modes.cnvrSpec
import com.fincalc.app.ui.finance.modes.costSpec
import com.fincalc.app.ui.finance.modes.daysSpec
import com.fincalc.app.ui.finance.modes.smplSpec
import com.fincalc.app.ui.keyboard.Key
import com.fincalc.app.ui.keyboard.Keypad
import com.fincalc.app.ui.keyboard.modeKeyRows
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val calcState = CalcState()

    /**
     * 按持久化的语言注入 locale（审查修复：Locale.setDefault 不会改变重建后的资源配置，
     * 必须在 attachBaseContext 用 createConfigurationContext 注入，对所有 API 级别确定生效）。
     */
    override fun attachBaseContext(newBase: Context) {
        val chinese = Prefs.loadLocaleBlocking(newBase)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(if (chinese) Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动时装入持久化的设置与历史（一次性同步读取，量小）
        runBlocking { Prefs.load(this@MainActivity, calcState) }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinCalcApp(calcState)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CoroutineScope(Dispatchers.IO).launch { Prefs.save(this@MainActivity, calcState) }
    }
}

/** 应用入口：按当前模式分发界面；模式菜单与设置由对话框弹出。 */
@Composable
fun FinCalcApp(state: CalcState) {
    var showModes by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    // BEVN 子模式（BEV/MOS/DOL/DFL/DCL/QTY）；remember 键 = state.mode，切模式即重置为 BEV
    var bevnSub by remember(state.mode) { mutableStateOf(BevnSub.BEV) }

    when (state.mode) {
        Mode.COMP -> CompScreen(
            controller = remember { CompController(state) },
            onOpenModes = { showModes = true },
            onOpenSettings = { showSettings = true }
        )
        Mode.CASH -> CashModeBody(state)
        Mode.STAT -> StatModeBody(state)
        else -> {
            val specPair = when (state.mode) {
                Mode.SMPL -> smplSpec(state)
                Mode.CNVR -> cnvrSpec(state)
                Mode.COST -> costSpec(state)
                Mode.DAYS -> daysSpec(state)
                Mode.CMPD -> cmpdSpec(state)
                Mode.AMRT -> amrtSpec(state)
                Mode.BOND -> bondSpec(state)
                Mode.BEVN -> bevnSpec(state, bevnSub)
                else -> null
            }
            if (specPair == null) {
                // 计划 6 后续任务实现；占位界面（带返回通道）
                Column {
                    Text(stringResource(R.string.mode_coming_soon))
                    Button(onClick = { state.switchMode(Mode.COMP) }) {
                        Text(stringResource(R.string.back))
                    }
                }
            } else {
                FinanceModeBody(
                    state, specPair.first, specPair.second,
                    bevnSub = if (state.mode == Mode.BEVN) bevnSub else null,
                    onBevnSubChange = { bevnSub = it }
                )
            }
        }
    }

    if (showModes) ModeDialog(state, onDismiss = { showModes = false })
    if (showSettings) SettingsDialog(state, onDismiss = { showSettings = false })
}

/** 金融模式主体：变量列表屏（BEVN 含子模式切换条）+ 金融键盘。 */
@Composable
private fun FinanceModeBody(
    state: CalcState,
    spec: ModeScreenSpec,
    solver: (FinanceVar) -> Double,
    bevnSub: BevnSub? = null,
    onBevnSubChange: (BevnSub) -> Unit = {}
) {
    // Task 3 审查前瞻提醒：spec 结构随 bondTerm/prfRatio/bevenSales/bevnSub 变化，
    // AMRT solver 语义随 payment 变化——全部纳入 remember 键，变更即重建 spec/controller。
    val controller = remember(
        state.mode,
        state.settings.bondTerm,
        state.settings.payment,
        state.settings.prfRatio,
        state.settings.bevenSales,
        bevnSub
    ) { FinanceController(state, spec, solver) }
    // 长按变量弹出的公式目标（null = 不显示）
    var formulaVar by remember { mutableStateOf<FinanceVar?>(null) }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121712))) {
        if (bevnSub != null) {
            // BEVN 子模式切换条（仅 BEVN 显示；点击更新 bevnSub，spec 随之重建）
            Row(modifier = Modifier.fillMaxWidth()) {
                BevnSub.values().forEach { sub ->
                    Button(
                        onClick = { onBevnSubChange(sub) },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sub == bevnSub) Color(0xFF4E6B52) else Color(0xFF2E3B30)
                        )
                    ) {
                        Text(sub.name, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            FinanceScreen(controller, onLongPressVar = { formulaVar = it })
        }
        Keypad(rows = financeKeys(controller, state), shift = state.shift, modifier = Modifier.weight(3f))
    }
    // 长按变量行的公式弹窗（学习辅助）
    formulaVar?.let { v ->
        AlertDialog(
            onDismissRequest = { formulaVar = null },
            title = { Text(v.label) },
            text = { Text(v.formula) },
            confirmButton = { TextButton(onClick = { formulaVar = null }) { Text("OK") } }
        )
    }
}

/** 金融模式键盘：模式键两行 + 精简编辑键 + 数字区 + EXE/SOLVE。 */
private fun financeKeys(c: FinanceController, state: CalcState): List<List<Key>> {
    fun ins(text: String): Key = Key(text, onPress = { c.insert(text) })
    return modeKeyRows(state) + listOf(
        listOf(
            Key("SHIFT", onPress = { state.toggleShift() }),
            Key("▲", onPress = { c.moveUp() }),
            Key("▼", onPress = { c.moveDown() }),
            Key("DEL", onPress = { c.delete() }),
            Key("AC", onPress = { c.clear() }),
            Key("SOLVE", onPress = { c.solve() })
        ),
        listOf(ins("7"), ins("8"), ins("9"), ins("("), ins(")"), ins("÷")),
        listOf(ins("4"), ins("5"), ins("6"), ins("×"), ins("+"), ins("E")),
        listOf(ins("1"), ins("2"), ins("3"), ins("."), ins("%"), ins(",")),
        listOf(ins("0"), ins("Ans"), ins("π"), ins("-"), Key("EXE", "SOLVE", onPress = { c.exe() }, onShiftPress = { c.solve() }))
    )
}

/** CASH 模式主体：I% 输入行 + Csh 列表编辑器 + 结果/错误 + NPV/IRR/NFV/PBP 求解钮 + 模式键行。 */
@Composable
private fun CashModeBody(state: CalcState) {
    val controller = remember(state.mode) { CashController(state) }
    // I% 文本态：输入即解析入 VARS（非法中间态保留旧值），求解时由 CashController 读取
    var iText by remember(state.mode) {
        mutableStateOf(NumberFormatter.format(state.getVar("I%"), state.settings.display))
    }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121712))) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("I% =", color = Color(0xFFE8F5E9), fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                OutlinedTextField(
                    value = iText,
                    onValueChange = { t ->
                        iText = t
                        t.trim().toDoubleOrNull()?.let { state.setVar("I%", it) }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                ListEditor(
                    rows = controller.rows.map { listOf(it) },
                    columns = listOf("Csh"),
                    onCellChange = { r, _, t -> controller.editRow(r, t) },
                    onDeleteRow = { controller.deleteRow(it) }
                )
            }
            TextButton(onClick = { controller.addRow() }) {
                Text("ADD", color = Color(0xFFE8F5E9))
            }
            controller.errorText?.let {
                Text(it, color = Color(0xFFFFB4A2), fontSize = 18.sp)
            }
            controller.resultText?.let {
                Text(it, color = Color(0xFFE8F5E9), fontSize = 20.sp)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("NPV", "IRR", "NFV", "PBP").forEach { target ->
                    Button(
                        onClick = { controller.solve(target) },
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3B30))
                    ) {
                        Text(target, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
        Keypad(rows = modeKeyRows(state), shift = state.shift, modifier = Modifier.weight(3f))
    }
}

/** STAT 模式主体：类型选择条（1-VAR + 7 回归）+ 数据编辑器 + CALC + 结果/错误 + 模式键行。 */
@Composable
private fun StatModeBody(state: CalcState) {
    val controller = remember(state.mode) { StatController(state) }
    // 类型选择（CN-130 模型名）：null = 1-VAR
    val types = listOf<Pair<String, Stat.RegType?>>(
        "1-VAR" to null,
        "A+BX" to Stat.RegType.LINEAR,
        "_+CX²" to Stat.RegType.QUADRATIC,
        "ln X" to Stat.RegType.LOG,
        "e^X" to Stat.RegType.EXP,
        "A•B^X" to Stat.RegType.AB_EXP,
        "A•X^B" to Stat.RegType.POWER,
        "1/X" to Stat.RegType.RECIPROCAL
    )
    val columns = buildList {
        add("X")
        if (controller.regType != null) add("Y")
        if (state.settings.statFreq) add("FREQ")
    }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121712))) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)) {
            types.chunked(4).forEach { rowTypes ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowTypes.forEach { (label, type) ->
                        Button(
                            onClick = { controller.setType(type) },
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (controller.regType == type) Color(0xFF4E6B52) else Color(0xFF2E3B30)
                            )
                        ) {
                            Text(label, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                ListEditor(
                    rows = controller.xs.indices.map { i ->
                        buildList {
                            add(controller.xs[i])
                            if (controller.regType != null) add(controller.ys.getOrElse(i) { "" })
                            if (state.settings.statFreq) add(controller.freqs.getOrElse(i) { "" })
                        }
                    },
                    columns = columns,
                    onCellChange = { r, c, t ->
                        when (columns.getOrNull(c)) {
                            "X" -> if (r in controller.xs.indices) controller.xs[r] = t
                            "Y" -> if (r in controller.ys.indices) controller.ys[r] = t
                            "FREQ" -> if (r in controller.freqs.indices) controller.freqs[r] = t
                        }
                    },
                    onDeleteRow = { controller.deleteRow(it) }
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { controller.addRow() }) {
                    Text("ADD", color = Color(0xFFE8F5E9))
                }
                TextButton(onClick = { controller.compute() }) {
                    Text("CALC", color = Color(0xFFE8F5E9))
                }
            }
            controller.errorText?.let {
                Text(it, color = Color(0xFFFFB4A2), fontSize = 18.sp)
            }
            controller.resultLines.forEach {
                Text(it, color = Color(0xFFE8F5E9), fontSize = 14.sp, maxLines = 1)
            }
        }
        Keypad(rows = modeKeyRows(state), shift = state.shift, modifier = Modifier.weight(3f))
    }
}
