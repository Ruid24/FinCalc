package com.fincalc.app.ui.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.ui.DPAD_BASE
import com.fincalc.app.ui.DPAD_KEY
import com.fincalc.app.ui.KEY_TEXT

/**
 * 菱形方向盘（FC-200V 真机 REPLAY 键）：圆形底盘 + ▲▼◄► 四键菱形排布
 * （▲ 上中、▼ 下中、◄ 左中、► 右中），点击触发对应回调，带 KEYBOARD_TAP 振动。
 * 尺寸由调用方经 modifier 给出（稍大于 2 列宽 2 行高的键区）。
 */
@Composable
fun DPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 圆形底盘：直径取区域高（区域约方形时近似占满）
        Surface(
            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
            shape = CircleShape,
            color = DPAD_BASE
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                DPadKey("▲", Modifier.align(Alignment.TopCenter), onUp)
                DPadKey("▼", Modifier.align(Alignment.BottomCenter), onDown)
                DPadKey("◄", Modifier.align(Alignment.CenterStart), onLeft)
                DPadKey("►", Modifier.align(Alignment.CenterEnd), onRight)
            }
        }
    }
}

@Composable
private fun DPadKey(symbol: String, modifier: Modifier, onPress: () -> Unit) {
    val view = LocalView.current
    Surface(
        modifier = modifier
            .fillMaxHeight(0.33f)
            .aspectRatio(1f)
            .clickable {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                onPress()
            },
        shape = CircleShape,
        color = DPAD_KEY
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, color = KEY_TEXT, fontSize = 14.sp, maxLines = 1)
        }
    }
}

/**
 * 顶部功能行（行0/行1）：6 列 2 行网格——左 2 列放 leftTop/leftBottom（各 2 键），
 * 右 2 列放 rightTop/rightBottom（各 2 键），中间 2 列跨 2 行放 DPad。
 * 键帽视觉与 Keypad 一致（含 shift/alpha 标注印刷与激活态）。
 */
@Composable
fun TopFunctionRows(
    leftTop: List<Key>,
    leftBottom: List<Key>,
    rightTop: List<Key>,
    rightBottom: List<Key>,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    shift: Boolean,
    alpha: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Column(
            modifier = Modifier.weight(2f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                for (key in leftTop) KeyCap(key, shift, alpha, Modifier.weight(1f).fillMaxHeight())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                for (key in leftBottom) KeyCap(key, shift, alpha, Modifier.weight(1f).fillMaxHeight())
            }
        }
        DPad(
            onUp = onUp,
            onDown = onDown,
            onLeft = onLeft,
            onRight = onRight,
            modifier = Modifier.weight(2f).fillMaxHeight()
        )
        Column(
            modifier = Modifier.weight(2f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                for (key in rightTop) KeyCap(key, shift, alpha, Modifier.weight(1f).fillMaxHeight())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                for (key in rightBottom) KeyCap(key, shift, alpha, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}
