package com.fincalc.app.ui.dialogs

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.fincalc.app.core.finance.Cmpd
import com.fincalc.app.core.finance.Days
import com.fincalc.app.data.Prefs
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Settings
import kotlinx.coroutines.launch

/** 设置对话框：角度单位、数值显示、界面语言 + 金融设置（持久化后重建 Activity 生效）。 */
@Composable
fun SettingsDialog(state: CalcState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
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
                // ---- 金融设置（Task 4 审查发现补入：8 项原本无 UI 入口） ----
                FinanceSettings(state)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

/** 金融设置区（CMPD/AMRT/BOND/BEVN/DAYS/SMPL 共用）。 */
@Composable
private fun FinanceSettings(state: CalcState) {
    val s = state.settings
    @Composable
    fun <T> row2(title: String, cur: T, a: T, b: T, labelA: String, labelB: String, set: (T) -> Settings) {
        Text(title)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = cur == a, onClick = { state.settings = set(a) })
            Text(labelA)
            RadioButton(selected = cur == b, onClick = { state.settings = set(b) })
            Text(labelB)
        }
    }
    row2(stringResource(R.string.payment_setting), s.payment, Cmpd.Payment.END, Cmpd.Payment.BEGIN, "End", "Begin") { s.copy(payment = it) }
    row2(stringResource(R.string.dn_setting), s.dn, Cmpd.OddPeriod.CI, Cmpd.OddPeriod.SI, "CI", "SI") { s.copy(dn = it) }
    row2(stringResource(R.string.date_mode), s.days360, false, true, "365", "360") { s.copy(days360 = it) }
    row2(stringResource(R.string.date_input), s.dateFormat, Days.DateFormat.MDY, Days.DateFormat.DMY, "MDY", "DMY") { s.copy(dateFormat = it) }
    row2(stringResource(R.string.bond_term), s.bondTerm, false, true, "Date", "Term") { s.copy(bondTerm = it) }
    row2(stringResource(R.string.periods_per_year), s.periodsPerYear, 1, 2, "Annual", "Semi") { s.copy(periodsPerYear = it) }
    row2(stringResource(R.string.prf_ratio), s.prfRatio, false, true, "PRF", "r%") { s.copy(prfRatio = it) }
    row2(stringResource(R.string.beven), s.bevenSales, false, true, "Quantity", "Sales") { s.copy(bevenSales = it) }
    row2(stringResource(R.string.stat_freq), s.statFreq, false, true, "Off", "On") { s.copy(statFreq = it) }
}
