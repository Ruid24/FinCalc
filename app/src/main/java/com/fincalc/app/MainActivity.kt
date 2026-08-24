package com.fincalc.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import com.fincalc.app.data.Prefs
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode
import com.fincalc.app.ui.comp.CompController
import com.fincalc.app.ui.comp.CompScreen
import com.fincalc.app.ui.dialogs.ModeDialog
import com.fincalc.app.ui.dialogs.SettingsDialog
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
            // 计划 6 实现其余模式；占位界面（带返回通道，避免单向门）
            Column {
                Text(stringResource(R.string.mode_coming_soon))
                Button(onClick = { state.switchMode(Mode.COMP) }) {
                    Text(stringResource(R.string.back))
                }
            }
        }
    }

    if (showModes) ModeDialog(state, onDismiss = { showModes = false })
    if (showSettings) SettingsDialog(state, onDismiss = { showSettings = false })
}
