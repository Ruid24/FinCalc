package com.fincalc.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.fincalc.app.data.Prefs
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode
import com.fincalc.app.ui.comp.CompController
import com.fincalc.app.ui.comp.CompScreen
import com.fincalc.app.ui.dialogs.ModeDialog
import com.fincalc.app.ui.dialogs.SettingsDialog
import com.fincalc.app.ui.finance.FinanceController
import com.fincalc.app.ui.finance.FinanceScreen
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec
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

    when (state.mode) {
        Mode.COMP -> CompScreen(
            controller = remember { CompController(state) },
            onOpenModes = { showModes = true },
            onOpenSettings = { showSettings = true }
        )
        else -> {
            val specPair = when (state.mode) {
                Mode.SMPL -> smplSpec(state)
                Mode.CNVR -> cnvrSpec(state)
                Mode.COST -> costSpec(state)
                Mode.DAYS -> daysSpec(state)
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
                FinanceModeBody(state, specPair.first, specPair.second)
            }
        }
    }

    if (showModes) ModeDialog(state, onDismiss = { showModes = false })
    if (showSettings) SettingsDialog(state, onDismiss = { showSettings = false })
}

/** 金融模式主体：变量列表屏 + 金融键盘。 */
@Composable
private fun FinanceModeBody(
    state: CalcState,
    spec: ModeScreenSpec,
    solver: (FinanceVar) -> Double
) {
    val controller = remember(state.mode) { FinanceController(state, spec, solver) }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121712))) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            FinanceScreen(controller)
        }
        Keypad(rows = financeKeys(controller, state), shift = state.shift, modifier = Modifier.weight(3f))
    }
}

/** 金融模式键盘：模式键两行 + 精简编辑键 + 数字区 + EXE/SOLVE。 */
private fun financeKeys(c: FinanceController, state: CalcState): List<List<Key>> {
    fun ins(text: String): Key = Key(text, onPress = { c.insert(text) })
    return modeKeyRows(state) + listOf(
        listOf(
            Key("SHIFT", onPress = { state.toggleShift() }),
            Key("◀", onPress = { /* 表达式光标留计划内简化：金融编辑先退格 */ }),
            Key("▶", onPress = { }),
            Key("DEL", onPress = { c.delete() }),
            Key("AC", onPress = { c.clear() }),
            Key("SOLVE", onPress = { c.solve() })
        ),
        listOf(ins("7"), ins("8"), ins("9"), ins("("), ins(")"), ins("÷")),
        listOf(ins("4"), ins("5"), ins("6"), ins("-"), ins("×"), ins("+")),
        listOf(ins("1"), ins("2"), ins("3"), ins("."), ins("-"), ins("%")),
        listOf(ins("0"), ins("E"), ins("Ans"), Key("EXE", onPress = { c.exe() }), ins("π"), ins(","))
    )
}
