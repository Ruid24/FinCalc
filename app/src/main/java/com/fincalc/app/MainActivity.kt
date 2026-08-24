package com.fincalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode
import com.fincalc.app.ui.comp.CompController
import com.fincalc.app.ui.comp.CompScreen
import com.fincalc.app.ui.dialogs.ModeDialog
import com.fincalc.app.ui.dialogs.SettingsDialog

class MainActivity : ComponentActivity() {

    private val calcState = CalcState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinCalcApp(calcState)
                }
            }
        }
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
            // 计划 6 实现其余模式；占位界面
            Text("Mode ${state.mode.name} — under construction")
        }
    }

    if (showModes) ModeDialog(state, onDismiss = { showModes = false })
    if (showSettings) SettingsDialog(state, onDismiss = { showSettings = false })
}
