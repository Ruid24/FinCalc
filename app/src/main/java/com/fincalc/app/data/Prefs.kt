package com.fincalc.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fincalc.app.core.expr.AngleUnit
import com.fincalc.app.core.expr.DisplayMode
import com.fincalc.app.core.finance.Cmpd
import com.fincalc.app.core.finance.Days
import com.fincalc.app.state.CalcState
import com.fincalc.app.state.HistoryEntry
import com.fincalc.app.state.Settings
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "fincalc")

/** DataStore 持久化：设置 + COMP 历史（变量不持久化，计划 6 再议）。 */
object Prefs {

    private val KEY_ANGLE = stringPreferencesKey("angle")
    private val KEY_DISPLAY = stringPreferencesKey("display")
    private val KEY_PAYMENT = stringPreferencesKey("payment")
    private val KEY_DN = stringPreferencesKey("dn")
    private val KEY_DAYS360 = booleanPreferencesKey("days360")
    private val KEY_DATE_FORMAT = stringPreferencesKey("dateFormat")
    private val KEY_BOND_TERM = booleanPreferencesKey("bondTerm")
    private val KEY_PPY = intPreferencesKey("periodsPerYear")
    private val KEY_PRF_RATIO = booleanPreferencesKey("prfRatio")
    private val KEY_BEVEN_SALES = booleanPreferencesKey("bevenSales")
    private val KEY_STAT_FREQ = booleanPreferencesKey("statFreq")
    private val KEY_CHINESE = booleanPreferencesKey("chinese")
    private val KEY_HISTORY = stringPreferencesKey("history")

    suspend fun load(context: Context, state: CalcState) {
        val p = context.dataStore.data.first()
        val settings = Settings(
            angle = p[KEY_ANGLE]?.let { runCatching { AngleUnit.valueOf(it) }.getOrNull() } ?: AngleUnit.DEG,
            display = parseDisplay(p[KEY_DISPLAY]),
            payment = p[KEY_PAYMENT]?.let { runCatching { Cmpd.Payment.valueOf(it) }.getOrNull() }
                ?: Cmpd.Payment.END,
            dn = p[KEY_DN]?.let { runCatching { Cmpd.OddPeriod.valueOf(it) }.getOrNull() } ?: Cmpd.OddPeriod.CI,
            days360 = p[KEY_DAYS360] ?: false,
            dateFormat = p[KEY_DATE_FORMAT]?.let { runCatching { Days.DateFormat.valueOf(it) }.getOrNull() }
                ?: Days.DateFormat.MDY,
            bondTerm = p[KEY_BOND_TERM] ?: false,
            periodsPerYear = p[KEY_PPY] ?: 1,
            prfRatio = p[KEY_PRF_RATIO] ?: false,
            bevenSales = p[KEY_BEVEN_SALES] ?: false,
            statFreq = p[KEY_STAT_FREQ] ?: false,
            chinese = p[KEY_CHINESE] ?: true
        )
        state.settings = settings
        val hist = p[KEY_HISTORY].orEmpty()
        if (hist.isNotEmpty()) {
            state.history.clear()
            state.history += hist.split("\n").mapNotNull { line ->
                val i = line.indexOf('\t')
                if (i <= 0) null else HistoryEntry(line.substring(0, i), line.substring(i + 1).toDoubleOrNull() ?: return@mapNotNull null)
            }
        }
    }

    suspend fun save(context: Context, state: CalcState) {
        context.dataStore.edit { p ->
            p[KEY_ANGLE] = state.settings.angle.name
            p[KEY_DISPLAY] = formatDisplay(state.settings.display)
            p[KEY_PAYMENT] = state.settings.payment.name
            p[KEY_DN] = state.settings.dn.name
            p[KEY_DAYS360] = state.settings.days360
            p[KEY_DATE_FORMAT] = state.settings.dateFormat.name
            p[KEY_BOND_TERM] = state.settings.bondTerm
            p[KEY_PPY] = state.settings.periodsPerYear
            p[KEY_PRF_RATIO] = state.settings.prfRatio
            p[KEY_BEVEN_SALES] = state.settings.bevenSales
            p[KEY_STAT_FREQ] = state.settings.statFreq
            p[KEY_CHINESE] = state.settings.chinese
            p[KEY_HISTORY] = state.history.joinToString("\n") { "${it.input}\t${it.result}" }
        }
    }

    private fun parseDisplay(s: String?): DisplayMode = when {
        s == null -> DisplayMode.Norm1
        s == "Norm1" -> DisplayMode.Norm1
        s == "Norm2" -> DisplayMode.Norm2
        s.startsWith("Fix:") -> s.removePrefix("Fix:").toIntOrNull()?.let { DisplayMode.Fix(it) } ?: DisplayMode.Norm1
        s.startsWith("Sci:") -> s.removePrefix("Sci:").toIntOrNull()?.let { DisplayMode.Sci(it) } ?: DisplayMode.Norm1
        else -> DisplayMode.Norm1
    }

    private fun formatDisplay(d: DisplayMode): String = when (d) {
        DisplayMode.Norm1 -> "Norm1"
        DisplayMode.Norm2 -> "Norm2"
        is DisplayMode.Fix -> "Fix:${d.digits}"
        is DisplayMode.Sci -> "Sci:${d.digits}"
    }
}
