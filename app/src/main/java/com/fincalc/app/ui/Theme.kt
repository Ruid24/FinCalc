package com.fincalc.app.ui

import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 全局色板（FC-200V 真机配色，计划 7 Task 3 收口；对照 media/mockup_cmpd.png / mockup_comp.png）。
 * 屏区 = 浅色液晶屏（浅绿灰底 + 深墨字）；机身/键面 = 深色 + shift 黄 / alpha 红印刷标注。
 * CASH/STAT 编辑区同属屏区（计划 7 Task 5.1 浅色化）；其按钮行/类型选择条保留 BTN_NORMAL 深底键帽感。
 */

// ── 屏区（浅色液晶屏）──
val SCREEN_BG = Color(0xFFD8E2CE)        // 液晶底色（浅绿灰）
val SCREEN_TXT = Color(0xFF141A12)       // 主文字（深墨）：输入行/结果行/变量行
val SCREEN_SEL_BG = Color(0xFF1A221A)    // 选中行反色底（深底）
val SCREEN_SEL_TXT = SCREEN_BG           // 选中行反色字（浅字，跟随屏底色）
val SCREEN_HINT = Color(0xFF4A5A48)      // 状态行小字（COMP 屏顶部模式/角度/SHIFT/ALPHA 指示）
val SCREEN_ERROR = Color(0xFF8C3A2E)     // 错误文字（浅底上的可读深红）
val SCREEN_CURSOR = Color(0xFFC83C1E)    // COMP 输入行闪烁光标（红，渲染图 (200,60,30)）

// ── 机身 ──
val APP_BG = Color(0xFF121712)           // 机身底色（屏外区域）

// ── 键帽文字与标注 ──
val KEY_TEXT = Color(0xFFE8F5E9)         // 键帽主文字（含圆盘符号；CASH/STAT 屏文字自 Task 5.1 起改用 SCREEN_TXT）
val KEY_SHIFT_MARK = Color(0xFFF0C040)   // shift 标注黄（真机印刷色）
val KEY_ALPHA_MARK = Color(0xFFE05544)   // alpha 标注红（真机印刷色）
val KEY_SHIFT_ACTIVE = Color(0xFF39493B) // SHIFT 激活态键帽底
val KEY_ALPHA_ACTIVE = Color(0xFF4B3230) // ALPHA 激活态键帽底

// ── 键帽底色（KeyColor 映射见 Keyboard.baseColor）──
val KEY_NORMAL = Color(0xFF232B25)
val KEY_NUM = Color(0xFF34403A)          // 数字稍亮
val KEY_OP = Color(0xFF3A483E)
val KEY_MODE = Color(0xFF1E261F)         // 模式/功能稍暗
val KEY_MODE_ACTIVE = Color(0xFF4E6B52)  // 当前模式高亮；BEVN/DEPR 子模式条、STAT 类型行选中态同用
val KEY_FUNC = KEY_MODE                  // 与 MODE 同色，枚举分立留作后续分化
val KEY_BLUE = Color(0xFF2B5EA7)         // DEL/AC/SOLVE 蓝键

// ── 按钮（CASH/STAT 按钮行、子模式条未选中态；选中态用 KEY_MODE_ACTIVE）──
val BTN_NORMAL = Color(0xFF2E3B30)

// ── 菱形方向盘 ──
val DPAD_BASE = Color(0xFF2C362E)        // 圆形底盘
val DPAD_KEY = Color(0xFF3A463C)         // 四向键

/** 浅屏输入框配色（CASH 的 I% 框与 ListEditor 单元格共用）：深墨文字/光标，透明容器透出屏底色。 */
@Composable
fun screenTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = SCREEN_TXT,
    unfocusedTextColor = SCREEN_TXT,
    cursorColor = SCREEN_TXT,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = SCREEN_TXT,
    unfocusedIndicatorColor = SCREEN_HINT
)
