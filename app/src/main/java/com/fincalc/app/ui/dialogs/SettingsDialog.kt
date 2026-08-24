package com.fincalc.app.ui.dialogs

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fincalc.app.R
import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.core.expr.DisplayMode
import com.fincalc.app.data.Prefs
import com.fincalc.app.state.CalcState
import kotlinx.coroutines.launch

/** 设置对话框：角度单位、数值显示、界面语言（持久化后重建 Activity 生效）。 */
@Composable
fun SettingsDialog(state: CalcState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column {
                // 角度单位
                Text(stringResource(R.string.angle_unit))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (unit in AngleUnit.entries) {
                        RadioButton(
                            selected = state.settings.angle == unit,
                            onClick = { state.settings = state.settings.copy(angle = unit) }
                        )
                        Text(unit.name)
                    }
                }
                // 数值显示
                Text(stringResource(R.string.display_mode))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.settings.display == DisplayMode.Norm1,
                        onClick = { state.settings = state.settings.copy(display = DisplayMode.Norm1) }
                    )
                    Text("Norm1")
                    RadioButton(
                        selected = state.settings.display == DisplayMode.Norm2,
                        onClick = { state.settings = state.settings.copy(display = DisplayMode.Norm2) }
                    )
                    Text("Norm2")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.settings.display is DisplayMode.Fix,
                        onClick = { state.settings = state.settings.copy(display = DisplayMode.Fix(3)) }
                    )
                    Text("Fix 3")
                    RadioButton(
                        selected = state.settings.display is DisplayMode.Sci,
                        onClick = { state.settings = state.settings.copy(display = DisplayMode.Sci(5)) }
                    )
                    Text("Sci 5")
                }
                // 界面语言
                Text(stringResource(R.string.language))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.settings.chinese,
                        onClick = {
                            state.settings = state.settings.copy(chinese = true)
                            scope.launch { Prefs.save(context, state) }
                            activity?.recreate()
                        }
                    )
                    Text("中文")
                    RadioButton(
                        selected = !state.settings.chinese,
                        onClick = {
                            state.settings = state.settings.copy(chinese = false)
                            scope.launch { Prefs.save(context, state) }
                            activity?.recreate()
                        }
                    )
                    Text("English")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
