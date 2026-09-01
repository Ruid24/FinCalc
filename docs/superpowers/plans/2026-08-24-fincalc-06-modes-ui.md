# 计划 6：其余 11 模式界面 + VARS 接线 + 数据编辑器 + 学习辅助 + UI 重设计 + 收尾

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成全部 12 模式界面（金融变量列表 + 数据编辑器）、VARS 共享变量接线、学习辅助（长按公式）、UI 重设计（屏键比例 1:3、闪烁光标 + 触控定位、金融模式键面上置）、M+/M-/STO/RCL 全流程，收尾 README 与 APK 签名决策。完成后 FinCalc 功能全量可用。

**Architecture:** 沿用既有分层（core 引擎 + state 状态机 + data 持久化 + ui Compose）。新增：
- `core/expr`：AST 节点加**体属性** `span`（源串区间，不影响 data class 相等性）——支撑光标定位
- `core/render`：MathBox 携带源跨度标签——支撑 MathView 光标命中
- `ui/finance`：金融模式通用框架（FinanceVar/ModeScreenSpec/FinanceController/变量列表屏）
- `ui/editor`：CASH/STAT 数据编辑器
- 模式接线：11 个模式的变量表 + 求解派发（纯数据/小函数）

**Tech Stack:** 既有，无新依赖、无需联网。

**环境前提：** 每次新开 shell 先 `source .dev/env.sh`。

**权威依据：**
- 金融模式交互（设计文档 §6）：多行可滚动变量列表、当前行高亮、方向键移动、EXE 存入、SOLVE 求解，与实体机一致；金融表达式值允许表达式输入（说明书 CN-56 例：16 个月 20 天输 20÷30+16）。
- 设置屏（说明书 CN-19~26）：payment/dn/dateMode/dateInput/bondDate/periodsY/prfRatio/bEven/statFreq/angle/display。
- M+/M-/STO/RCL（说明书 CN-42~45）：M 独立存储器加减、变量赋值与召回。
- 学习辅助（设计文档 §6）：金融模式长按变量查看公式（LaTeX 排版）。
- UI 重设计（2026-08-24 用户反馈，已入设计文档 §6）：屏约占 1/4、闪烁光标 + 触控定位、金融模式键面上置（两行 12 键）、参考 media/FC-200V-2_F.png 与 media/Screenshot_Calc_Business.jpg。

**设计决策（未覆盖处本计划规定）：**

1. **光标定位（用户反馈需求）**：Tokenizer 的 Token 加 `pos`（源串起始下标）；Parser 构造节点时写入 `Node.span`（**体属性** `var span: IntRange? = null`，data class 相等性不受影响，既有测试零影响）；MathBuilder 给 MathBox 打 span 标签；MathView 按 span 命中光标 x 位置 + 触控反查。闪烁用 Compose InfiniteTransition。
2. **金融模式通用框架**：每个模式声明 `ModeScreenSpec`（变量表 + 求解目标 + 公式文本 + 设置联动）；`FinanceController` 统一处理 变量导航（▲▼/触控点选）、输入编辑（复用表达式输入机制，EXE 求值存入）、SOLVE 派发、结果写回 VARS。
3. **输入值允许表达式**（EXE 时经 ExprEngine 求值，支持 CN-56 的 20÷30+16 式输入）；变量列表值按当前显示格式格式化。
4. **数据编辑器**（CASH 的 Csh 列表、STAT 的 X/Y/FREQ 列表）：行式编辑（每行 1~3 列输入框），行数上限按设置（CASH 80；STAT 80/40/26 随 FREQ 显隐）；Del 删行、末行后追加。
5. **VARS 共享**：state.vars 泛型键（"n"、"I%"、"PV"、"PMT"、"FV"、"P/Y"、"C/Y"、"PM1"、"PM2"、"Dys"、"d1"、"d2"、"RDV"、"CPN"、"PRC"、"YLD"、"CST"、"SEL"、"MRG"、"SAL"、"VC"、"FC"、"ITR"、"QTY"、"VCU"、"SBE"、"EIT"、"PRF"、"r%"、"QBE" 等）；模式间共享沿用真机规则（CMPD↔AMRT 的 n/I%/PV/PMT/FV 等；BEVN 内部 QTY 双组联动）。
6. **键面布局重设计**（FC-200V + Calc Business 融合）：顶两行 12 个模式键（SMPL CMPD CASH AMRT COMP STAT / CNVR COST DAYS DEPR BOND BEVN），第三行 SHIFT ◀ ▶ ▲ ▼ DEL/AC……数字区在底部，EXE 右下。SET（设置）键保留。MODE 键改为辅助入口（弹菜单仍在，非主路径）。
7. **学习辅助**：金融模式变量行**长按**弹公式窗（公式文本取自说明书"计算公式"章节的线性形式，用 MathView 无法渲染的说明性公式则以文本显示）。
8. **已知留白**：APK 正式签名仍用调试签名（设计文档 §8：调试签名，注明"个人学习项目"）；定制快捷键、INS 行插入、数字千分位（Digit Sep.）不做；变量持久化不做（计划 5 决策）；统计估计值函数符（m/n/m1/m2 优先级第 4 级）不进 COMP 键盘（真机亦然，STAT 界面内提供）。

**修订记录（执行期）：**

- 2026-09-01（Task 5 双审查）：①规格审查发现 parseFlows 注释修订（MATH→Syntax ERROR）与 CashMode.kt 全限定名→import 的等价改写未提交——已补交。②质量审查发现**行数上限未执行**（CASH 80、STAT 80/40/26 是计划明文验收项，而计划参考实现与实现双方都没写——计划内部矛盾）；已补：CashController.addRow 上限 80、StatController.addRow 按类型/FREQ 取 80/40/26。③非阻塞备注：STAT 存量行在开 FREQ 后其 FREQ 格输入被吞（规避：删行重加）；CashStatTest 缺 NFV 断言与全部 STAT 用例（后续加固）；CASH/STAT 的 Keypad 只有 2 行模式键却占 weight(3f)（键被拉大，外观项）。
- 2026-08-25（Task 4 质量审查发现，三处已修复）：①**设置层计划缺口**——SettingsDialog 原本只有角度/显示/语言，8 项金融设置（payment/dn/days360/dateFormat/bondTerm/periodsPerYear/prfRatio/bevenSales）无 UI 入口（Prefs 持久化已备而界面不可达）；已在 SettingsDialog 补金融设置区（`FinanceSettings` composable + `row2` 助手 + 双语词条），Column 加滚动。②**SHIFT 在金融键盘滞留**——FinanceController 无 clearShift，SHIFT 后按 EXE 会误触其 SOLVE shift 层覆写选中变量；已在 select/insert/delete/clear/exe/solve 各入口统一 `state.clearShift()`。③BOND 的 YLD Date 分支缺 1902~2097 范围检查（PRC 分支有）——已补齐对称。非阻塞备注：BOND Date 形态/AMRT/错误路径无 UI 层用例（引擎层已兜底）；B-Even=Sales 形态下反解走 QBE=0 的计划外组合已声明不做。
- 2026-08-25（Task 3 质量审查发现，已修复）：①**▲▼ 未接线**——financeKeys 无导航键调用 moveUp/moveDown（变量导航曾纯触控，控制器方法成死代码）；已把死键 ◀▶ 换成 ▲▼ 并接线。②`-` 键在 R3/R4 重复出现——重排数字区消除重复（E 只出现一次）。③SHIFT 在金融键盘无 shiftLabel 可用（死键）——EXE 键加 `SOLVE` shift 层使其有意义。顺带：计划 Step 5 参考实现与计划正文的 ▲▼ 描述原本就自相矛盾（正文说 ▲▼、实现没接），已统一为接线版。**给 Task 4 实现者的前瞻提醒（审查提出）**：`FinanceModeBody` 用 `remember(state.mode)` 持控制器，而 BOND 的 spec 结构随 `settings.bondTerm` 变化——在 BOND 模式内改 Bond Date 设置不会重建 spec，Task 4 实现时记得让 spec/controller 的 remember 键包含 `state.settings.bondTerm`（以及 AMRT 的 `payment`、BEV 的 `prfRatio`/`bevenSales`）。（已在 Task 4 落实：remember 键含全部 spec 结构依赖 + bevnSub）
- 2026-08-25（Task 3 实现子代理回报，两处已接纳）：①`expression input allowed` 测试期望值与 spec 冲突——Dys 标 `integer=true`，EXE 存入取整，20÷30+16 → 17 而非 16.67；实现方守约未动 spec/控制器，修正测试断言为 17.0 并加注释（计划文本已同步）。②计划的 `git add` 路径只覆盖 main 不含 test——已在计划全部 6 处统一补上 `app/src/test/java/com/fincalc/app/`。

- 2026-08-25（Task 2 双审查）：①规格审查——实现方省略了计划 import 块中两个未使用 import（无害清理），计划已同步为磁盘版本。②质量审查发现闪烁光标的 alpha 读取挂在组合作用域上，导致**每帧重组 + 每帧两次 解析+测量**（持续空转耗电）；已修复：测量结果 `remember(input, cursor, baseTextSize)` 缓存 + 光标条常驻用 `Modifier.alpha` 控制显隐。③触控落点 `toInt` 向零取整左偏半字符——已改 `roundToInt`。④MathView 降级文本与光标测量的字体不一致（Roboto vs Serif）——降级 Text 补 `FontFamily.Serif`（计划 5 文本同步）。备注（不修）：前缀排版在分数/自动加括号内部的光标 x 有系统性近似偏差（设计决策已声明）；输入行无 auto-scroll-to-cursor（后续 UX 优化）。
- 2026-08-24（Task 2 实现子代理强制修正+控制器收尾）：`setCursor` 函数与 `cursor` 属性委托生成同名 JVM setter 冲突（与计划 5 的 setMode 同类）——实现方按授权加 `@JvmName("setCursorPosition")`（Kotlin 层 API 不变），计划文本已同步；控制器顺手清理了 CompScreen 中因换用 InputLine 而失效的 MathView 导入。构建通过。
- 2026-08-24（Task 1 质量审查发现，已修复）：Keypad 行无 weight、M3 Button 固定最小高 40dp——weight(3f) 只分配槽位未拉伸键，高屏底部留白、矮屏裁切底部行。已修：行加 `weight(1f)`、键加 `fillMaxHeight`（提交 a7aea29）。规格审查 PASS（KeyLayouts 逐字一致 + CompScreen 四处改动点齐全；`media/` 保持未跟踪）。

---

### Task 1: UI 重设计（屏键比例 + 模式键面上置）

**Files:**
- Modify: `app/src/main/java/com/fincalc/app/ui/keyboard/Keyboard.kt`
- Modify: `app/src/main/java/com/fincalc/app/ui/comp/CompScreen.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/keyboard/KeyLayouts.kt`
- Modify: `app/src/main/res/values/strings.xml`、`values-zh-rCN/strings.xml`（模式名不动：键面缩写英文）

- [ ] **Step 1: KeyLayouts.kt（键面布局数据；模式键两行置顶的共享骨架）**

```kotlin
package com.fincalc.app.ui.keyboard

import com.fincalc.app.state.CalcState
import com.fincalc.app.state.Mode

/** 模式键面（FC-200V 风格，置顶两行）：12 模式直接切换，不进二级菜单（2026-08-24 用户反馈）。 */
fun modeKeyRows(state: CalcState): List<List<Key>> = listOf(
    listOf(Mode.SMPL, Mode.CMPD, Mode.CASH, Mode.AMRT, Mode.COMP, Mode.STAT).map { modeKey(state, it) },
    listOf(Mode.CNVR, Mode.COST, Mode.DAYS, Mode.DEPR, Mode.BOND, Mode.BEVN).map { modeKey(state, it) }
)

private fun modeKey(state: CalcState, m: Mode): Key =
    Key(m.name, onPress = { state.switchMode(m) })
```

说明：`Key` 需要支持小字号文本（模式键名 3-4 字母）——`Keyboard.kt` 的 `Keypad` 已用 13.sp，模式行直接用现有 `Keypad` 组件即可（无需改动）。

- [ ] **Step 2: CompScreen.kt 调整（屏:键 = 1:3；键面顶部加模式键两行）**

改动点（以当前磁盘版本为基准修改，保持其余结构不变）：
1. 屏键比例：外层 Column 不变；显示屏 Column 保持 `Modifier.weight(1f)`；`Keypad(...)` 调用处加 `modifier = Modifier.weight(3f)`——屏:键 = 1:3（参考 Calc Business 截图）。
2. `compKeys(...)` 返回值头部插入模式键两行：`return modeKeyRows(s) + listOf(...原 8 行...)`（`KeyLayouts.kt` 已导入）。
3. 原最后一行的 `Key("SET", onPress = onOpenSettings)` 移除（模式键行已含全模式；设置改由 SHIFT+MODE 触发：把第一行 `Key("MODE", onPress = onOpenModes)` 改为 `Key("MODE", "SET", onPress = onOpenModes, onShiftPress = onOpenSettings)`）；最后一行变为六个变量键 `ins("A") ins("B") ins("C") ins("D") ins("X") ins("Y")`（补上此前缺的 D）。

参考实现（CompScreen.kt 的 CompScreen 函数与 compKeys 按上述改动；其余不变）。

- [ ] **Step 3: 构建 + 真机走查**

```bash
source .dev/env.sh
./gradlew assembleDebug
```

走查确认：显示屏约占 1/4 高度；12 模式键在键盘顶部两行；点 CMPD 等切到占位界面（带返回）；COMP 功能不变。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/fincalc/app/ app/src/test/java/com/fincalc/app/ui/
git commit -m "feat(ui): 屏键比例 1:3 + 金融模式键面上置两行（用户反馈，FC-200V 风格）"
```

---

### Task 2: 光标系统（闪烁 + 触控定位）

**Files:**
- Modify: `app/src/main/java/com/fincalc/app/ui/math/MathView.kt`（加前缀宽度测量助手）
- Create: `app/src/main/java/com/fincalc/app/ui/comp/InputLine.kt`
- Modify: `app/src/main/java/com/fincalc/app/ui/comp/CompScreen.kt`（输入行换用 InputLine）

**设计决策（本计划决策 1 的修订——更轻量方案）**：不为光标给 AST 加源跨度（避免动 core/expr 已锁定的大量结构断言）。光标 x 位置 = **前缀（0..cursor）独立排版**的宽度：前缀可解析时精确（光标在顶层 token 边界时与实际排版逐像素一致）；前缀不可解析（光标在分数/根号内部）时按线性文本宽度近似。触控定位按 x 比例映射回字符下标。够用的诚实方案；若真机体验不佳，后续再引入 AST span。

- [ ] **Step 1: MathView.kt 加前缀测量助手**

在 MathView.kt 追加：

```kotlin
/** 前缀排版宽度（光标定位用）：前缀可解析则精确，否则线性文本近似。 */
fun measurePrefixWidth(
    prefix: String,
    m: TextMeasurer,
    baseTextSize: androidx.compose.ui.unit.TextUnit,
    em: Float
): Float {
    if (prefix.isEmpty()) return 0f
    val measure = TextMeasure { text, scale ->
        val layout = m.measure(text, TextStyle(fontSize = baseTextSize * scale, fontFamily = FontFamily.Serif))
        layout.size.width.toFloat() to layout.size.height.toFloat()
    }
    val program = try {
        ExprEngine.parse(prefix)
    } catch (e: Exception) {
        null
    }
    return if (program != null) {
        MathBuilder.build(program, measure, em).width
    } else {
        m.measure(prefix, TextStyle(fontSize = baseTextSize, fontFamily = FontFamily.Serif)).size.width.toFloat()
    }
}
```

（MathView.kt 顶部按需补 import：`com.fincalc.app.core.expr.ExprEngine` 已有。）

- [ ] **Step 2: InputLine.kt（排版输入行 + 闪烁光标 + 触控定位）**

```kotlin
package com.fincalc.app.ui.comp

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincalc.app.ui.math.MathView
import com.fincalc.app.ui.math.measurePrefixWidth
import kotlin.math.roundToInt

/** 排版输入行 + 闪烁光标 + 触控定位（用户反馈 2026-08-24）。 */
@Composable
fun InputLine(
    input: String,
    cursor: Int,
    onCursorTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    baseTextSize: androidx.compose.ui.unit.TextUnit = 22.sp,
    color: Color = Color(0xFFE8F5E9)
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val em = with(density) { baseTextSize.toPx() }
    // 审查修复：测量结果缓存——闪烁动画每帧重组，避免每帧重解析+重测量
    val cursorX = remember(input, cursor, baseTextSize) {
        measurePrefixWidth(input.take(cursor.coerceIn(0, input.length)), textMeasurer, baseTextSize, em)
    }
    val totalW = remember(input, baseTextSize) {
        measurePrefixWidth(input, textMeasurer, baseTextSize, em)
    }

    // 闪烁（500ms 往复）
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )

    Box(
        modifier = modifier.pointerInput(input, totalW) {
            detectTapGestures { offset ->
                val ratio = if (totalW > 0) (offset.x / totalW).coerceIn(0f, 1f) else 0f
                onCursorTap((ratio * input.length).roundToInt())
            }
        }
    ) {
        MathView(input, baseTextSize = baseTextSize, color = color)
        // 光标条（常驻 + alpha 控制显隐）
        Box(
            modifier = Modifier
                .offset(x = with(density) { cursorX.toDp() }, y = 0.dp)
                .width(2.dp)
                .height(with(density) { (em * 1.1f).toDp() })
                .alpha(if (alpha > 0.5f) 1f else 0f)
                .background(color)
        )
    }
}
```

- [ ] **Step 3: CompScreen.kt 输入行换用 InputLine**

输入行的 Row 内容从 `MathView(controller.input, ...)` 改为：

```kotlin
InputLine(
    input = controller.input,
    cursor = controller.cursor,
    onCursorTap = { controller.setCursor(it) },
    baseTextSize = 22.sp
)
```

注意：CompController 需新增光标写入入口（当前 `cursor` 是 `private set`）。在 CompController.kt 加：

```kotlin
    /** 触控定位光标（用户反馈 2026-08-24）。@JvmName 避开与属性 setter 的 JVM 签名冲突。 */
    @JvmName("setCursorPosition")
    fun setCursor(pos: Int) {
        cursor = pos.coerceIn(0, input.length)
        errorText = null
    }
```

（`@JvmName` 需 `import kotlin.jvm.JvmName`；属性委托生成 `setCursor(I)V` 与同名函数冲突，必须改名。）

输入行的 Row 保持 `horizontalScroll`；触控定位的滚动偏移换算本计划从简（可视坐标直接映射，滚动后位置略有偏差可接受，后续再精确）。

- [ ] **Step 4: 构建 + 提交**

```bash
source .dev/env.sh
./gradlew assembleDebug
git add app/src/main/java/com/fincalc/app/ app/src/test/java/com/fincalc/app/
git commit -m "feat(ui): 输入行闪烁光标 + 触控定位（用户反馈）"
```

---

### Task 3: 金融模式通用框架 + SMPL/CNVR/COST/DAYS 接线

**Files:**
- Create: `app/src/main/java/com/fincalc/app/ui/finance/FinanceVar.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/finance/ModeScreenSpec.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/finance/FinanceController.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/finance/FinanceScreen.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/finance/modes/SimpleModes.kt`
- Create: `app/src/test/java/com/fincalc/app/ui/finance/FinanceControllerTest.kt`
- Modify: `app/src/main/java/com/fincalc/app/MainActivity.kt`（模式分发接入）

- [ ] **Step 1: FinanceVar.kt + ModeScreenSpec.kt（模式声明数据类）**

```kotlin
package com.fincalc.app.ui.finance

/** 金融模式变量描述（FC-200V 变量列表的一行）。 */
data class FinanceVar(
    val key: String,             // VARS 键名（共享变量容器）
    val label: String,           // 显示名（英文缩写，真机一致）
    val solvable: Boolean,       // 可否作为 SOLVE 目标
    val formula: String,         // 长按显示的公式（学习辅助，线性文本）
    val integer: Boolean = false // 是否整数输入（如 PM1/PM2、n）
)

/** 模式界面声明：变量表 + 求解目标 + 设置联动说明。 */
data class ModeScreenSpec(
    val title: String,           // 界面标题（模式名）
    val vars: List<FinanceVar>
)
```

- [ ] **Step 2: FinanceController.kt（变量导航 + 输入编辑 + EXE 存入 + SOLVE 派发）**

```kotlin
package com.fincalc.app.ui.finance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState

/**
 * 金融模式控制器（FC-200V 操作逻辑，设计文档 §6）：
 * ▲▼/触控选变量（当前行高亮）→ 键盘输入（允许表达式）→ EXE 存入 → 选目标 → SOLVE 求解写回。
 */
class FinanceController(
    val state: CalcState,
    val spec: ModeScreenSpec,
    /** 求解派发：目标变量 → 结果值（引擎调用由模式接线处给出）。 */
    private val solver: (target: FinanceVar) -> Double
) {
    /** 当前选中行下标。 */
    var selected by mutableStateOf(0)
        private set

    /** 当前编辑串（为空 = 未在编辑，显示选中行现值）。 */
    var editText by mutableStateOf<String?>(null)
        private set

    var errorText by mutableStateOf<String?>(null)
        private set

    /** 结果消息（求解成功后显示：target=result）。 */
    var resultText by mutableStateOf<String?>(null)
        private set

    fun select(index: Int) {
        state.clearShift()
        selected = index.coerceIn(0, spec.vars.size - 1)
        editText = null
        errorText = null
        resultText = null
    }

    fun moveUp() = select(selected - 1)
    fun moveDown() = select(selected + 1)

    /** 输入字符（开始/继续编辑当前行）。 */
    fun insert(text: String) {
        state.clearShift()
        errorText = null
        resultText = null
        editText = (editText ?: "") + text
    }

    fun delete() {
        state.clearShift()
        editText = editText?.let { if (it.isNotEmpty()) it.dropLast(1) else null }
    }

    fun clear() {
        state.clearShift()
        editText = null
        errorText = null
        resultText = null
    }

    /** 当前行显示值：仅选中行在编辑中显示编辑串，其余行显示 VARS 现值（格式化）。 */
    fun displayValue(index: Int, v: FinanceVar): String =
        if (index == selected && editText != null) editText!!
        else NumberFormatter.format(state.getVar(v.key), state.settings.display)

    /** EXE：求值当前编辑串并存入选中变量（允许表达式输入，CN-56）。 */
    fun exe() {
        state.clearShift()
        val text = editText ?: return
        try {
            val value = ExprEngine.eval(text, state.exprContext())
            val v = spec.vars[selected]
            state.setVar(v.key, if (v.integer) kotlin.math.round(value) else value)
            editText = null
            errorText = null
        } catch (e: CalcException) {
            errorText = e.kind.display
        }
    }

    /** SOLVE：求解选中变量并写回 VARS。 */
    fun solve() {
        state.clearShift()
        val target = spec.vars[selected]
        if (!target.solvable) {
            errorText = "Math ERROR"
            return
        }
        try {
            val result = solver(target)
            state.setVar(target.key, result)
            resultText = "${target.label} = ${NumberFormatter.format(result, state.settings.display)}"
            errorText = null
            editText = null
        } catch (e: CalcException) {
            errorText = e.kind.display
            resultText = null
        }
    }
}
```

- [ ] **Step 3: SimpleModes.kt（SMPL/CNVR/COST/DAYS 的 spec + solver 派发）**

```kotlin
package com.fincalc.app.ui.finance.modes

import com.fincalc.app.core.finance.Cnvr
import com.fincalc.app.core.finance.Cost
import com.fincalc.app.core.finance.Days
import com.fincalc.app.core.finance.Smpl
import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec

/** SMPL（CN-50~52）：Set/Dys/I%/PV → SI、SFV。 */
fun smplSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "SMPL",
        vars = listOf(
            FinanceVar("Dys", "Dys", solvable = false, formula = "SI' = Dys÷(365 or 360) × PV × i", integer = true),
            FinanceVar("I%", "I%", solvable = false, formula = "i = I%÷100"),
            FinanceVar("PV", "PV", solvable = false, formula = "本金（现值）"),
            FinanceVar("SI", "SI", solvable = true, formula = "SI = −SI′"),
            FinanceVar("SFV", "SFV", solvable = true, formula = "SFV = −(PV+SI′)")
        )
    ),
    { target: FinanceVar ->
        val days = if (state.settings.days360) 360 else 365
        when (target.key) {
            "SI" -> Smpl.si(state.getVar("Dys"), state.getVar("I%"), state.getVar("PV"), days)
            "SFV" -> Smpl.sfv(state.getVar("Dys"), state.getVar("I%"), state.getVar("PV"), days)
            else -> error("不可求解")
        }
    }
)

/** CNVR（CN-71~73）：n、I% → EFF/APR。 */
fun cnvrSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "CNVR",
        vars = listOf(
            FinanceVar("n", "n", solvable = false, formula = "年复利计算数", integer = true),
            FinanceVar("I%", "I%", solvable = false, formula = "利率（每年）"),
            FinanceVar("EFF", "EFF", solvable = true, formula = "EFF = ((1+APR/100/n)^n − 1)×100"),
            FinanceVar("APR", "APR", solvable = true, formula = "APR = ((1+EFF/100)^(1/n) − 1)×n×100")
        )
    ),
    { target: FinanceVar ->
        when (target.key) {
            "EFF" -> Cnvr.eff(state.getVar("I%"), state.getVar("n"))
            "APR" -> Cnvr.apr(state.getVar("I%"), state.getVar("n"))
            else -> error("不可求解")
        }
    }
)

/** COST（CN-74/75）：CST/SEL/MRG 互求。 */
fun costSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "COST",
        vars = listOf(
            FinanceVar("CST", "CST", solvable = true, formula = "CST = SEL × (1 − MRG/100)"),
            FinanceVar("SEL", "SEL", solvable = true, formula = "SEL = CST ÷ (1 − MRG/100)"),
            FinanceVar("MRG", "MRG", solvable = true, formula = "MRG = (1 − CST/SEL) × 100")
        )
    ),
    { target: FinanceVar ->
        when (target.key) {
            "CST" -> Cost.cst(state.getVar("SEL"), state.getVar("MRG"))
            "SEL" -> Cost.sel(state.getVar("CST"), state.getVar("MRG"))
            "MRG" -> Cost.mrg(state.getVar("CST"), state.getVar("SEL"))
            else -> error("不可求解")
        }
    }
)

/** DAYS（CN-76~79）：d1/d2/Dys 知二求一。 */
fun daysSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "DAYS",
        vars = listOf(
            FinanceVar("d1", "d1", solvable = true, formula = "起始日期", integer = true),
            FinanceVar("d2", "d2", solvable = true, formula = "结束日期", integer = true),
            FinanceVar("Dys", "Dys", solvable = true, formula = "天数（时期）", integer = true)
        )
    ),
    { target: FinanceVar ->
        val fmt = state.settings.dateFormat
        val d360 = state.settings.days360
        when (target.key) {
            "Dys" -> Days.daysBetween(
                Days.parse(state.getVar("d1"), fmt),
                Days.parse(state.getVar("d2"), fmt),
                d360
            ).toDouble()
            "d1" -> Days.format(
                Days.minusDays(Days.parse(state.getVar("d2"), fmt), state.getVar("Dys").toInt()),
                fmt
            )
            "d2" -> Days.format(
                Days.plusDays(Days.parse(state.getVar("d1"), fmt), state.getVar("Dys").toInt()),
                fmt
            )
            else -> error("不可求解")
        }
    }
)
```

- [ ] **Step 4: FinanceScreen.kt（变量列表屏）**

```kotlin
package com.fincalc.app.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 金融模式通用屏：多行可滚动变量列表，当前行高亮（设计文档 §6）。 */
@Composable
fun FinanceScreen(
    controller: FinanceController,
    onLongPressVar: (FinanceVar) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B2A1E))
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        controller.spec.vars.forEachIndexed { index, v ->
            val isCurrent = index == controller.selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isCurrent) Color(0xFF39493B) else Color.Transparent)
                    .clickable { controller.select(index) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${v.label} = ${controller.displayValue(index, v)}",
                    color = Color(0xFFE8F5E9),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
        // 错误/结果行
        controller.errorText?.let {
            Text(it, color = Color(0xFFFFB4A2), fontSize = 18.sp)
        }
        controller.resultText?.let {
            Text(it, color = Color(0xFFE8F5E9), fontSize = 20.sp)
        }
    }
}
```

（注：`onLongPressVar` 参数本任务不用，Task 6 学习辅助接线；保留参数占位。）

- [ ] **Step 5: MainActivity.kt 模式分发接入 + 金融模式键盘**

`FinCalcApp` 的 `when (state.mode)` 扩展：非 COMP 且有 spec 的模式显示 FinanceScreen + 金融键盘（数字/运算符/▲▼/EXE/SOLVE/AC/DEL + 模式键行）。参考实现：

```kotlin
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
```

新增（MainActivity.kt 或独立文件，实现子代理可自行放置）：

```kotlin
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
```

（实现子代理：◀▶ 在金融编辑中本计划暂不接线（退格编辑足够），显示但留空操作即可；若觉得别扭可以移除这两个键并调整为 4 列布局，允许。）

- [ ] **Step 6: FinanceControllerTest.kt（控制器行为测试）**

```kotlin
package com.fincalc.app.ui.finance

import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceControllerTest {

    private fun costController(state: CalcState): FinanceController {
        val (spec, solver) = com.fincalc.app.ui.finance.modes.costSpec(state)
        return FinanceController(state, spec, solver)
    }

    @Test
    fun `navigate and edit and store`() {
        val s = CalcState()
        val c = costController(s)
        // 选中 CST（第 0 行），输入 40，EXE 存入
        c.insert("4"); c.insert("0"); c.exe()
        assertEquals(40.0, s.getVar("CST"), 0.0)
        // ▼ 到 SEL，输入 100，EXE
        c.moveDown()
        c.insert("1"); c.insert("0"); c.insert("0"); c.exe()
        assertEquals(100.0, s.getVar("SEL"), 0.0)
        // ▼ 到 MRG，SOLVE → 60
        c.moveDown()
        c.solve()
        assertEquals(60.0, s.getVar("MRG"), 1e-12)
        assertEquals("MRG = 60", c.resultText)
    }

    @Test
    fun `expression input allowed`() {
        // CN-56：输入值允许表达式（20÷30+16 照常求值；Dys 标 integer，EXE 存入时取整 → 17）
        val s = CalcState()
        val (spec, _) = com.fincalc.app.ui.finance.modes.smplSpec(s)
        val c = FinanceController(s, spec) { 0.0 }
        c.insert("2"); c.insert("0"); c.insert("÷"); c.insert("3"); c.insert("0"); c.insert("+"); c.insert("1"); c.insert("6")
        c.exe()
        assertEquals(17.0, s.getVar("Dys"), 1e-9)
    }

    @Test
    fun `solve non solvable shows error`() {
        val s = CalcState()
        val (spec, _) = com.fincalc.app.ui.finance.modes.smplSpec(s)
        val c = FinanceController(s, spec) { 0.0 }
        c.select(0)  // Dys 不可解
        c.solve()
        assertEquals("Math ERROR", c.errorText)
    }

    @Test
    fun `bad expression shows error and keeps editing`() {
        val s = CalcState()
        val c = costController(s)
        c.insert("1"); c.insert("÷"); c.insert("0"); c.exe()
        assertEquals("Math ERROR", c.errorText)
        assertEquals("1÷0", c.editText)
    }
}
```

- [ ] **Step 7: 测试 + 构建 + 提交**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.ui.finance.FinanceControllerTest"
./gradlew assembleDebug
git add app/src/main/java/com/fincalc/app/ app/src/test/java/com/fincalc/app/
git commit -m "feat(ui): 金融模式通用框架 + SMPL/CNVR/COST/DAYS 界面接线"
```

预期：控制器 4 测全过；构建通过；COMP 不受影响（既有测试全绿）。

---

### Task 4: CMPD + AMRT + BOND + BEVN 接线

**Files:**
- Create: `app/src/main/java/com/fincalc/app/ui/finance/modes/TvmModes.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/finance/modes/BevnModes.kt`
- Modify: `app/src/main/java/com/fincalc/app/MainActivity.kt`（when 分支接入）
- Create: `app/src/test/java/com/fincalc/app/ui/finance/TvmModesTest.kt`

**说明**：BEV 的设置联动（PRF/Ratio、B-Even）通过 Settings 读取；BOND 的 Date/Term 形态同理。

- [ ] **Step 1: TvmModes.kt（CMPD/AMRT/BOND spec + solver）**

```kotlin
package com.fincalc.app.ui.finance.modes

import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.finance.Amrt
import com.fincalc.app.core.finance.Bond
import com.fincalc.app.core.finance.Cmpd
import com.fincalc.app.core.finance.Days
import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec

/** CMPD（CN-53~58）：n、I%、PV、PMT、FV 任求其一。 */
fun cmpdSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "CMPD",
        vars = listOf(
            FinanceVar("n", "n", solvable = true, formula = "付款次数"),
            FinanceVar("I%", "I%", solvable = true, formula = "利率（每年，牛顿法近似）"),
            FinanceVar("PV", "PV", solvable = true, formula = "PV = (−α·PMT − β·FV)/γ"),
            FinanceVar("PMT", "PMT", solvable = true, formula = "PMT = (−γ·PV − β·FV)/α"),
            FinanceVar("FV", "FV", solvable = true, formula = "FV = (−γ·PV − α·PMT)/β")
        )
    ),
    { target: FinanceVar ->
        val s = state.settings
        val py = s.periodsPerYear
        val cy = s.periodsPerYear
        val pay = s.payment
        val dn = s.dn
        when (target.key) {
            "n" -> Cmpd.solveN(state.getVar("I%"), state.getVar("PV"), state.getVar("PMT"), state.getVar("FV"), py, cy, pay, dn)
            "I%" -> Cmpd.solveI(state.getVar("n"), state.getVar("PV"), state.getVar("PMT"), state.getVar("FV"), py, cy, pay, dn)
            "PV" -> Cmpd.solvePV(state.getVar("n"), state.getVar("I%"), state.getVar("PMT"), state.getVar("FV"), py, cy, pay, dn)
            "PMT" -> Cmpd.solvePMT(state.getVar("n"), state.getVar("I%"), state.getVar("PV"), state.getVar("FV"), py, cy, pay, dn)
            "FV" -> Cmpd.solveFV(state.getVar("n"), state.getVar("I%"), state.getVar("PV"), state.getVar("PMT"), py, cy, pay, dn)
            else -> error("不可求解")
        }
    }
)

/** AMRT（CN-65~70）：与 CMPD 共享 n/I%/PV/PMT/FV；PM1/PM2 + 五量。 */
fun amrtSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "AMRT",
        vars = listOf(
            FinanceVar("PM1", "PM1", solvable = false, formula = "范围首笔付款", integer = true),
            FinanceVar("PM2", "PM2", solvable = false, formula = "范围末笔付款", integer = true),
            FinanceVar("n", "n", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("I%", "I%", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("PV", "PV", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("PMT", "PMT", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("FV", "FV", solvable = false, formula = "（与 CMPD 共享）"),
            FinanceVar("BAL", "BAL", solvable = true, formula = "PM2 付款完毕时的本金余额"),
            FinanceVar("INT", "INT", solvable = true, formula = "PM1 的利息部分"),
            FinanceVar("PRN", "PRN", solvable = true, formula = "PM1 的本金部分"),
            FinanceVar("ΣINT", "ΣINT", solvable = true, formula = "PM1 至 PM2 的总利息"),
            FinanceVar("ΣPRN", "ΣPRN", solvable = true, formula = "PM1 至 PM2 的总本金")
        )
    ),
    { target: FinanceVar ->
        val s = state.settings
        val r = Amrt.amortize(
            state.getVar("PM1").toInt(), state.getVar("PM2").toInt(),
            state.getVar("I%"), state.getVar("PV"), state.getVar("PMT"),
            s.periodsPerYear, s.periodsPerYear, s.payment
        )
        when (target.key) {
            "BAL" -> r.bal
            "INT" -> r.int
            "PRN" -> r.prn
            "ΣINT" -> r.sumInt
            "ΣPRN" -> r.sumPrn
            else -> error("不可求解")
        }
    }
)

/** BOND（CN-85~92）：Date 形态（d1/d2）与 Term 形态（n）随设置切换。 */
fun bondSpec(state: CalcState): Pair<ModeScreenSpec, (FinanceVar) -> Double> {
    val term = state.settings.bondTerm
    val vars = mutableListOf<FinanceVar>()
    if (term) {
        vars += FinanceVar("n", "n", solvable = false, formula = "到期前票息支付次数", integer = true)
    } else {
        vars += FinanceVar("d1", "d1", solvable = false, formula = "购买日期", integer = true)
        vars += FinanceVar("d2", "d2", solvable = false, formula = "偿还日期", integer = true)
    }
    vars += listOf(
        FinanceVar("RDV", "RDV", solvable = false, formula = "每 \$100 票面价值的赎回价格"),
        FinanceVar("CPN", "CPN", solvable = false, formula = "息票利率"),
        FinanceVar("PRC", "PRC", solvable = true, formula = "每 \$100 票面价值的价格"),
        FinanceVar("YLD", "YLD", solvable = true, formula = "年收益（牛顿法近似）")
    )
    if (!term) {
        vars += FinanceVar("INT", "INT", solvable = false, formula = "应计利息（随 PRC 同算）")
        vars += FinanceVar("CST", "CST", solvable = false, formula = "含息价格（PRC+INT）")
    }
    val solver = { target: FinanceVar ->
        val s = state.settings
        val m = s.periodsPerYear
        val d360 = s.days360
        when (target.key) {
            "PRC" -> if (term) {
                Bond.prcTerm(state.getVar("n").toInt(), state.getVar("RDV"), state.getVar("CPN"), state.getVar("YLD"), m)
            } else {
                // BOND 专属日期范围（CN-87 L2570-2572）：d1∈1902~2097
                val d1 = Days.parse(state.getVar("d1"), s.dateFormat)
                val d2 = Days.parse(state.getVar("d2"), s.dateFormat)
                if (d1.year !in 1902..2097 || d2.year !in 1902..2097) {
                    throw CalcException(CalcException.Kind.ARGUMENT, "BOND 日期范围 1902~2097")
                }
                val r = Bond.prcDate(d1, d2, state.getVar("RDV"), state.getVar("CPN"), state.getVar("YLD"), m, d360)
                state.setVar("INT", r.int)
                state.setVar("CST", r.cst)
                r.prc
            }
            "YLD" -> if (term) {
                Bond.yldTerm(state.getVar("n").toInt(), state.getVar("RDV"), state.getVar("CPN"), state.getVar("PRC"), m)
            } else {
                val d1 = Days.parse(state.getVar("d1"), s.dateFormat)
                val d2 = Days.parse(state.getVar("d2"), s.dateFormat)
                if (d1.year !in 1902..2097 || d2.year !in 1902..2097) {
                    throw CalcException(CalcException.Kind.ARGUMENT, "BOND 日期范围 1902~2097")
                }
                Bond.yldDate(d1, d2, state.getVar("RDV"), state.getVar("CPN"), state.getVar("PRC"), m, d360)
            }
            else -> error("不可求解")
        }
    }
    return Pair(ModeScreenSpec(title = "BOND", vars = vars), solver)
}
```

- [ ] **Step 2: BevnModes.kt（六子模式 spec + solver）**

```kotlin
package com.fincalc.app.ui.finance.modes

import com.fincalc.app.core.finance.Bevn
import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.FinanceVar
import com.fincalc.app.ui.finance.ModeScreenSpec

/** BEVN 子模式清单（设计文档 §5）。 */
enum class BevnSub { BEV, MOS, DOL, DFL, DCL, QTY }

/** BEVN 子模式 spec（BEV 随 PRF/Ratio 与 B-Even 设置切换第 5/6 变量含义）。 */
fun bevnSpec(state: CalcState, sub: BevnSub): Pair<ModeScreenSpec, (FinanceVar) -> Double> =
    when (sub) {
        BevnSub.BEV -> bevSpec(state)
        BevnSub.MOS -> mosSpec(state)
        BevnSub.DOL -> dolSpec(state)
        BevnSub.DFL -> dflSpec(state)
        BevnSub.DCL -> dclSpec(state)
        BevnSub.QTY -> qtySpec(state)
    }

private fun bevSpec(state: CalcState): Pair<ModeScreenSpec, (FinanceVar) -> Double> {
    val ratio = state.settings.prfRatio
    val sales = state.settings.bevenSales
    val vars = mutableListOf(
        FinanceVar("PRC", "PRC", solvable = true, formula = "销售价格"),
        FinanceVar("VCU", "VCU", solvable = true, formula = "单位可变成本"),
        FinanceVar("FC", "FC", solvable = true, formula = "固定成本")
    )
    vars += if (ratio) {
        FinanceVar("r%", "r%", solvable = true, formula = "利润率")
    } else {
        FinanceVar("PRF", "PRF", solvable = true, formula = "利润")
    }
    vars += if (sales) {
        FinanceVar("SBE", "SBE", solvable = true, formula = "损益平衡销售额")
    } else {
        FinanceVar("QBE", "QBE", solvable = true, formula = "损益平衡销售量")
    }
    val solver = { target: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (target.key) {
            "PRC" -> if (ratio) Bevn.prcFromQbeRatio(g("QBE"), g("VCU"), g("FC"), g("r%")) else Bevn.prcFromQbePrf(g("QBE"), g("VCU"), g("FC"), g("PRF"))
            "VCU" -> if (ratio) Bevn.vcuFromQbeRatio(g("QBE"), g("PRC"), g("FC"), g("r%")) else Bevn.vcuFromQbePrf(g("QBE"), g("PRC"), g("FC"), g("PRF"))
            "FC" -> if (ratio) Bevn.fcFromQbeRatio(g("QBE"), g("PRC"), g("VCU"), g("r%")) else Bevn.fcFromQbePrf(g("QBE"), g("PRC"), g("VCU"), g("PRF"))
            "PRF" -> Bevn.prfFromQbe(g("QBE"), g("PRC"), g("VCU"), g("FC"))
            "r%" -> Bevn.rPercentFromQbe(g("QBE"), g("PRC"), g("VCU"), g("FC"))
            "QBE" -> if (ratio) Bevn.qbeRatio(g("PRC"), g("VCU"), g("FC"), g("r%")) else Bevn.qbePrf(g("PRC"), g("VCU"), g("FC"), g("PRF"))
            "SBE" -> if (ratio) Bevn.sbeRatio(g("PRC"), g("VCU"), g("FC"), g("r%")) else Bevn.sbePrf(g("PRC"), g("VCU"), g("FC"), g("PRF"))
            else -> error("不可求解")
        }
    }
    return Pair(ModeScreenSpec(title = "BEV", vars = vars), solver)
}
```

（实现子代理：BEV 的 SBE 侧反解按设计决策先换算 QBE=SBE/PRC——上面 solver 的 SBE 分支直接正向求解即可；若 UI 选择了 SBE 为已知量再反解他变量属计划外组合，不做。）

MOS/DOL/DFL/DCL/QTY 的 spec 同理（公式见计划 4 Task 4）：

```kotlin
private fun mosSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "MOS",
        vars = listOf(
            FinanceVar("SAL", "SAL", solvable = true, formula = "销售额"),
            FinanceVar("SBE", "SBE", solvable = true, formula = "损益平衡销售额"),
            FinanceVar("MOS", "MOS", solvable = true, formula = "MOS = (SAL−SBE)/SAL")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "SAL" -> Bevn.salFromMos(g("MOS"), g("SBE"))
            "SBE" -> Bevn.sbeFromMos(g("MOS"), g("SAL"))
            "MOS" -> Bevn.mos(g("SAL"), g("SBE"))
            else -> error("不可求解")
        }
    }
)

private fun dolSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "DOL",
        vars = listOf(
            FinanceVar("SAL", "SAL", solvable = true, formula = "销售额"),
            FinanceVar("VC", "VC", solvable = true, formula = "可变成本"),
            FinanceVar("FC", "FC", solvable = true, formula = "固定成本"),
            FinanceVar("DOL", "DOL", solvable = true, formula = "DOL = (SAL−VC)/(SAL−VC−FC)")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "SAL" -> Bevn.salFromDol(g("DOL"), g("VC"), g("FC"))
            "VC" -> Bevn.vcFromDol(g("DOL"), g("SAL"), g("FC"))
            "FC" -> Bevn.fcFromDol(g("DOL"), g("SAL"), g("VC"))
            "DOL" -> Bevn.dol(g("SAL"), g("VC"), g("FC"))
            else -> error("不可求解")
        }
    }
)

private fun dflSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "DFL",
        vars = listOf(
            FinanceVar("EIT", "EIT", solvable = true, formula = "利税前收入（EBIT）"),
            FinanceVar("ITR", "ITR", solvable = true, formula = "利息"),
            FinanceVar("DFL", "DFL", solvable = true, formula = "DFL = EIT/(EIT−ITR)")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "EIT" -> Bevn.eitFromDfl(g("DFL"), g("ITR"))
            "ITR" -> Bevn.itrFromDfl(g("DFL"), g("EIT"))
            "DFL" -> Bevn.dfl(g("EIT"), g("ITR"))
            else -> error("不可求解")
        }
    }
)

private fun dclSpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "DCL",
        vars = listOf(
            FinanceVar("SAL", "SAL", solvable = true, formula = "销售额"),
            FinanceVar("VC", "VC", solvable = true, formula = "可变成本"),
            FinanceVar("FC", "FC", solvable = true, formula = "固定成本"),
            FinanceVar("ITR", "ITR", solvable = true, formula = "利息"),
            FinanceVar("DCL", "DCL", solvable = true, formula = "DCL = (SAL−VC)/(SAL−VC−FC−ITR)")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "SAL" -> Bevn.salFromDcl(g("DCL"), g("VC"), g("FC"), g("ITR"))
            "VC" -> Bevn.vcFromDcl(g("DCL"), g("SAL"), g("FC"), g("ITR"))
            "FC" -> Bevn.fcFromDcl(g("DCL"), g("SAL"), g("VC"), g("ITR"))
            "ITR" -> Bevn.itrFromDcl(g("DCL"), g("SAL"), g("VC"), g("FC"))
            "DCL" -> Bevn.dcl(g("SAL"), g("VC"), g("FC"), g("ITR"))
            else -> error("不可求解")
        }
    }
)

private fun qtySpec(state: CalcState) = Pair(
    ModeScreenSpec(
        title = "QTY CONV",
        vars = listOf(
            FinanceVar("SAL", "SAL", solvable = true, formula = "销售额 = PRC×QTY"),
            FinanceVar("PRC", "PRC", solvable = true, formula = "销售价格"),
            FinanceVar("QTY", "QTY", solvable = true, formula = "销售数量（两组联动）"),
            FinanceVar("VC", "VC", solvable = true, formula = "可变成本 = VCU×QTY"),
            FinanceVar("VCU", "VCU", solvable = true, formula = "单位可变成本")
        )
    ),
    { t: FinanceVar ->
        val g = { k: String -> state.getVar(k) }
        when (t.key) {
            "SAL" -> Bevn.sal(g("PRC"), g("QTY"))
            "PRC" -> Bevn.prcFromSal(g("SAL"), g("QTY"))
            "QTY" -> if (g("PRC") != 0.0) Bevn.qtyFromSal(g("SAL"), g("PRC")) else Bevn.qtyFromVc(g("VC"), g("VCU"))
            "VC" -> Bevn.vc(g("VCU"), g("QTY"))
            "VCU" -> Bevn.vcuFromVc(g("VC"), g("QTY"))
            else -> error("不可求解")
        }
    }
)
```

- [ ] **Step 3: MainActivity.kt 接入 + BEVN 子模式切换条**

`FinCalcApp` 的 when 扩展：

```kotlin
                Mode.CMPD -> cmpdSpec(state)
                Mode.AMRT -> amrtSpec(state)
                Mode.BOND -> bondSpec(state)
                ...
                Mode.BEVN -> bevnSpec(state, bevnSub)   // bevnSub 为 remember 的子模式状态
```

BEVN 界面在 FinanceScreen 上方加一行子模式切换（6 个小按钮）。参考实现：在 FinanceModeBody 外套一层 Column，顶部 Row 放 BEV/MOS/DOL/DFL/DCL/QTY 按钮（仅 BEVN 模式显示）。

（实现子代理：BEVN 子模式状态用 `remember { mutableStateOf(BevnSub.BEV) }` 放在 FinCalcApp 或 FinanceModeBody 层均可，模式切换时重置为 BEV。）

- [ ] **Step 4: TvmModesTest.kt（接线测试：走一遍说明书例）**

```kotlin
package com.fincalc.app.ui.finance

import com.fincalc.app.state.CalcState
import com.fincalc.app.ui.finance.modes.bondSpec
import com.fincalc.app.ui.finance.modes.cmpdSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class TvmModesTest {

    @Test
    fun `cmpd manual example through controller`() {
        // 说明书例1：n=48、I%=4、PV=−1000、PMT=−300 → FV
        val s = CalcState()
        val (spec, solver) = cmpdSpec(s)
        val c = FinanceController(s, spec, solver)
        c.insert("4"); c.insert("8"); c.exe()                    // n=48
        c.moveDown(); c.insert("4"); c.exe()                      // I%=4
        c.moveDown(); c.insert("-"); c.insert("1"); c.insert("0"); c.insert("0"); c.insert("0"); c.exe()  // PV=−1000
        c.moveDown(); c.insert("-"); c.insert("3"); c.insert("0"); c.insert("0"); c.exe()                 // PMT=−300
        c.moveDown(); c.solve()                                   // FV
        assertEquals(16761.07896780279, s.getVar("FV"), 1e-4)
    }

    @Test
    fun `bond term mode through controller`() {
        // 例3：Term、n=3、RDV=100、CPN=3、YLD=4 → PRC
        val s = CalcState()
        s.settings = s.settings.copy(bondTerm = true)
        val (spec, solver) = bondSpec(s)
        val c = FinanceController(s, spec, solver)
        c.insert("3"); c.exe()                                    // n=3
        c.moveDown(); c.insert("1"); c.insert("0"); c.insert("0"); c.exe()  // RDV=100
        c.moveDown(); c.insert("3"); c.exe()                      // CPN=3
        c.moveDown(); c.insert("4"); c.exe()                      // YLD=4
        c.moveDown(); c.solve()                                   // PRC
        assertEquals(-97.22490896677286, s.getVar("PRC"), 1e-9)
    }
}
```

- [ ] **Step 5: 测试 + 构建 + 提交**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.ui.finance.TvmModesTest"
./gradlew assembleDebug
git add app/src/main/java/com/fincalc/app/ app/src/test/java/com/fincalc/app/
git commit -m "feat(ui): CMPD/AMRT/BOND/BEVN 模式接线（TVM 家族 + 损益六子模式）"
```

预期：2 测全过；构建通过。

---

### Task 5: CASH 现金流编辑器 + STAT 统计（编辑器 + 回归结果）

**Files:**
- Create: `app/src/main/java/com/fincalc/app/ui/editor/ListEditor.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/finance/modes/CashMode.kt`
- Create: `app/src/main/java/com/fincalc/app/ui/finance/modes/StatMode.kt`
- Modify: `app/src/main/java/com/fincalc/app/MainActivity.kt`（接入 CASH/STAT）
- Create: `app/src/test/java/com/fincalc/app/ui/finance/CashStatTest.kt`

**说明**：CASH 的 Csh 列表与 STAT 的 X/Y/FREQ 列表共用 `ListEditor` 组件。STAT 的 FREQ 列随 Settings.statFreq 显隐。

- [ ] **Step 1: ListEditor.kt（行式列表编辑器：数值行 + 删行 + 末行追加）**

```kotlin
package com.fincalc.app.ui.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 行式数据编辑器（CASH 的 Csh 列 / STAT 的 X(,Y,FREQ) 列）。
 * 每行每列一个输入框（文本，EXE 后由调用层解析）；行尾 DEL 删除；末行追加。
 */
@Composable
fun ListEditor(
    rows: List<List<String>>,
    columns: List<String>,
    onCellChange: (row: Int, col: Int, text: String) -> Unit,
    onDeleteRow: (row: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // 表头
        Row(modifier = Modifier.fillMaxWidth()) {
            columns.forEach { c ->
                Text(c, modifier = Modifier.weight(1f).padding(4.dp))
            }
            Text("", modifier = Modifier.padding(4.dp))
        }
        rows.forEachIndexed { r, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { ci, cell ->
                    OutlinedTextField(
                        value = cell,
                        onValueChange = { onCellChange(r, ci, it) },
                        modifier = Modifier.weight(1f).padding(2.dp),
                        singleLine = true
                    )
                }
                TextButton(onClick = { onDeleteRow(r) }) {
                    Text("DEL")
                }
            }
        }
    }
}
```

- [ ] **Step 2: CashMode.kt（I% + Csh 列表 + NPV/IRR/NFV/PBP）**

```kotlin
package com.fincalc.app.ui.finance.modes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.finance.Cash
import com.fincalc.app.state.CalcState

/** CASH 模式控制器：I% + 现金流列表（≤80 项，CN-63）→ NPV/IRR/NFV/PBP。 */
class CashController(val state: CalcState) {

    /** 现金流编辑行（文本态，求解时解析；mutableStateListOf 驱动重组）。 */
    val rows = androidx.compose.runtime.mutableStateListOf<String>()
    var resultText by mutableStateOf<String?>(null)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    fun addRow() { if (rows.size < Cash.MAX_ITEMS) rows += "" }   // ≤80 项（CN-63）
    fun deleteRow(i: Int) { if (i in rows.indices) rows.removeAt(i) }
    fun editRow(i: Int, text: String) { if (i in rows.indices) rows[i] = text }

    /** 解析现金流列表（空行跳过；非法数值 → Syntax ERROR）。 */
    private fun parseFlows(): List<Double> {
        val flows = rows.filter { it.isNotBlank() }.map {
            it.trim().toDoubleOrNull() ?: throw com.fincalc.app.core.expr.CalcException(
                com.fincalc.app.core.expr.CalcException.Kind.SYNTAX, "非法数值: $it"
            )
        }
        return flows
    }

    fun solve(target: String) {
        try {
            val flows = parseFlows()
            val i = state.getVar("I%")
            val r = when (target) {
                "NPV" -> Cash.npv(i, flows)
                "IRR" -> Cash.irr(flows)
                "NFV" -> Cash.nfv(i, flows)
                "PBP" -> Cash.pbp(i, flows)
                else -> error("不可求解")
            }
            resultText = "$target = ${com.fincalc.app.core.format.NumberFormatter.format(r, state.settings.display)}"
            errorText = null
        } catch (e: com.fincalc.app.core.expr.CalcException) {
            errorText = e.kind.display
            resultText = null
        }
    }
}
```

- [ ] **Step 3: StatMode.kt（类型选择 + 编辑器 + 统计量/回归结果浏览）**

```kotlin
package com.fincalc.app.ui.finance.modes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fincalc.app.core.expr.CalcException
import com.fincalc.app.core.finance.Stat
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState

/** STAT 模式控制器：类型（1-VAR/7 回归）+ 数据编辑器 + 统计量/回归结果。 */
class StatController(val state: CalcState) {

    var regType by mutableStateOf<Stat.RegType?>(null)   // null = 1-VAR
        private set
    val xs = mutableStateListOf<String>()
    val ys = mutableStateListOf<String>()
    val freqs = mutableStateListOf<String>()
    var resultLines by mutableStateOf<List<String>>(emptyList())
        private set
    var errorText by mutableStateOf<String?>(null)
        private set

    fun setType(type: Stat.RegType?) {
        regType = type
        xs.clear(); ys.clear(); freqs.clear()   // 切类型清数据（真机行为，CN-131）
        resultLines = emptyList()
        errorText = null
    }

    fun addRow() {
        // 行数上限（CN-132）：1-VAR 80 / 2-VAR 40 / 2-VAR+FREQ 26
        val cap = if (regType == null) {
            if (state.settings.statFreq) 40 else 80
        } else {
            if (state.settings.statFreq) 26 else 40
        }
        if (xs.size >= cap) return
        xs += ""; if (regType != null) ys += ""; if (state.settings.statFreq) freqs += ""
    }
    fun deleteRow(i: Int) {
        if (i in xs.indices) xs.removeAt(i)
        if (i in ys.indices) ys.removeAt(i)
        if (i in freqs.indices) freqs.removeAt(i)
    }

    private fun entries(): List<Stat.Entry> {
        val n = xs.size
        return (0 until n).mapNotNull { i ->
            val x = xs[i].trim().toDoubleOrNull() ?: return@mapNotNull null
            val y = if (regType != null) ys.getOrNull(i)?.trim()?.toDoubleOrNull() else null
            if (regType != null && y == null) return@mapNotNull null
            val f = if (state.settings.statFreq) freqs.getOrNull(i)?.trim()?.toDoubleOrNull() ?: 1.0 else 1.0
            Stat.Entry(x, y, f)
        }
    }

    /** 计算当前类型的统计量/回归结果（文本行列表）。 */
    fun compute() {
        try {
            val data = entries()
            val lines = mutableListOf<String>()
            val fmt = { v: Double -> NumberFormatter.format(v, state.settings.display) }
            val type = regType
            if (type == null) {
                // 1-VAR
                lines += "n = ${fmt(Stat.count(data))}"
                lines += "Σx = ${fmt(Stat.sumX(data))}"
                lines += "Σx² = ${fmt(Stat.sumX2(data))}"
                lines += "x̄ = ${fmt(Stat.meanX(data))}"
                lines += "xσn = ${fmt(Stat.stdXn(data))}"
                lines += "xσn-1 = ${fmt(Stat.stdXn1(data))}"
                lines += "minX = ${fmt(Stat.minX(data))}"
                lines += "maxX = ${fmt(Stat.maxX(data))}"
            } else {
                val r = Stat.regress(type, data)
                lines += "A = ${fmt(r.a)}"
                lines += "B = ${fmt(r.b)}"
                r.c?.let { lines += "C = ${fmt(it)}" }
                r.r?.let { lines += "r = ${fmt(it)}" }
            }
            resultLines = lines
            errorText = null
        } catch (e: CalcException) {
            errorText = e.kind.display
            resultLines = emptyList()
        }
    }
}
```

（实现子代理：x̂/ŷ 估计值输入本计划从简——只在结果区给回归系数与 r；x̂/ŷ 的引擎函数已备，UI 输入框后续加。如你愿意顺手加：回归模式下结果区底部加一行输入框 + X̂/Ŷ 两个按钮调用 estimateX/estimateY——属加分项，非必须。）

- [ ] **Step 4: MainActivity 接入 CASH/STAT**

`when` 分支加：

```kotlin
        Mode.CASH -> CashModeBody(state)
        Mode.STAT -> StatModeBody(state)
```

`CashModeBody`/`StatModeBody` 参考实现（结构：显示屏=编辑器+结果行、键盘=模式键两行 + 精简编辑键 + 求解键）：

```kotlin
@Composable
private fun CashModeBody(state: CalcState) {
    val controller = remember(state.mode) { CashController(state) }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF121712))) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // I% 输入行 + Csh 编辑器 + 结果/错误 + NPV/IRR/NFV/PBP 求解按钮行
            ...
        }
        Keypad(rows = modeKeyRows(state), shift = state.shift, modifier = Modifier.weight(3f))
    }
}
```

（实现子代理：CASH/STAT 的具体布局可自由组织，但须含：I% 输入（CASH）、现金流/数据编辑器、NPV/IRR/NFV/PBP 四个求解钮（CASH）、类型选择器 + 计算钮（STAT）、结果区、错误行、模式键行。结构细节可调整，行为不变。）

- [ ] **Step 5: CashStatTest.kt（控制器测试）**

```kotlin
package com.fincalc.app.ui.finance

import com.fincalc.app.ui.finance.modes.CashController
import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashStatTest {

    @Test
    fun `cash manual example through controller`() {
        // 说明书例（L1742）：CF=[−10000,−1000,4500,5000,4000]，I%=3
        val s = CalcState()
        s.setVar("I%", 3.0)
        val c = CashController(s)
        listOf("-10000", "-1000", "4500", "5000", "4000").forEach { c.addRow(); c.editRow(c.rows.size - 1, it) }
        c.solve("NPV")
        assertTrue(c.resultText!!.startsWith("NPV = 1400.464293"))
        c.solve("IRR")
        assertTrue(c.resultText!!.startsWith("IRR = 7.443619297"))
        c.solve("PBP")
        assertTrue(c.resultText!!.startsWith("PBP = 3.605941275"))
    }

    @Test
    fun `cash bad number shows error`() {
        val s = CalcState()
        val c = CashController(s)
        c.addRow(); c.editRow(0, "abc")
        c.solve("NPV")
        assertEquals("Syntax ERROR", c.errorText)
    }
}
```

- [ ] **Step 6: 测试 + 构建 + 提交**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.ui.finance.CashStatTest"
./gradlew assembleDebug
git add app/src/main/java/com/fincalc/app/ app/src/test/java/com/fincalc/app/
git commit -m "feat(ui): CASH 现金流编辑器 + STAT 统计（数据编辑器 + 回归结果浏览）"
```

预期：2 测全过；构建通过。

---

### Task 6: STO/RCL/M+ 存储器 + 学习辅助（长按公式）+ 振动反馈

**Files:**
- Modify: `app/src/main/java/com/fincalc/app/ui/keyboard/Keyboard.kt`（Key 加长按钩子）
- Create: `app/src/main/java/com/fincalc/app/ui/comp/MemoryDialogs.kt`（变量弹窗）
- Modify: `app/src/main/java/com/fincalc/app/ui/comp/CompScreen.kt`（键面加 STO/RCL/M+ 行）
- Modify: `app/src/main/java/com/fincalc/app/ui/finance/FinanceScreen.kt`（长按变量弹公式）
- Create: `app/src/test/java/com/fincalc/app/ui/comp/MemoryTest.kt`

- [ ] **Step 1: Keyboard.kt 加长按支持**

`Key` 加字段与 Keypad 的 Button 换 combinedClickable：

```kotlin
data class Key(
    val label: String,
    val shiftLabel: String? = null,
    val onPress: () -> Unit,
    val onShiftPress: (() -> Unit)? = null,
    val onLongPress: (() -> Unit)? = null
)
```

Keypad 的 Button 改为支持长按（参考实现：`Button` 不直接支持 onLongClick——用 `Surface` + `Modifier.combinedClickable(onClick=..., onLongClick=...)` 替换 Button，样式保持。允许实现子代理调整实现方式，行为不变）。

- [ ] **Step 2: MemoryDialogs.kt（STO/RCL 变量弹窗 + M+/M-）**

```kotlin
package com.fincalc.app.ui.comp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.fincalc.app.core.format.NumberFormatter
import com.fincalc.app.state.CalcState

/** COMP 存储器操作：STO（存当前结果到变量）/RCL（插入变量名到输入行）/M+（M+=Ans）/M-（M-=Ans）。 */
object Memory {

    val VAR_NAMES = listOf("A", "B", "C", "D", "X", "Y", "M")

    fun store(state: CalcState, name: String) {
        state.setVar(name, state.getVar("Ans"))
    }

    fun memPlus(state: CalcState) {
        state.setVar("M", state.getVar("M") + state.getVar("Ans"))
    }

    fun memMinus(state: CalcState) {
        state.setVar("M", state.getVar("M") - state.getVar("Ans"))
    }
}

/** 变量选择弹窗（STO/RCL 共用）：列出 A B C D X Y M Ans 及当前值。 */
@Composable
fun VarPickerDialog(
    title: String,
    state: CalcState,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                (Memory.VAR_NAMES + "Ans").forEach { name ->
                    TextButton(onClick = { onPick(name); onDismiss() }) {
                        Text("$name = ${NumberFormatter.format(state.getVar(name), state.settings.display)}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
```

- [ ] **Step 3: CompScreen.kt 键面加 STO/RCL/M+ 行**

在 compKeys 的变量行前加一行：

```kotlin
        listOf(
            Key("STO", onPress = { stoPicker = true }),
            Key("RCL", onPress = { rclPicker = true }),
            Key("M+", "M-", onPress = { Memory.memPlus(s) }, onShiftPress = { Memory.memMinus(s) }),
            ...
        )
```

（实现子代理：stoPicker/rclPicker 为 CompScreen 层的 `remember { mutableStateOf(false) }`；弹窗开启时显示 VarPickerDialog，STO 的 onPick = `Memory.store(state, it)`，RCL 的 onPick = `c.insert(it)`。将原变量行（A B C D X Y）与该行合并排布——保持 6 列网格整齐即可，细节可调。）

- [ ] **Step 4: FinanceScreen.kt 长按变量弹公式**

变量行的 `clickable` 换 `combinedClickable(onClick = 选中, onLongClick = { onLongPressVar(v) })`；FinanceModeBody 加公式弹窗状态，长按时弹 AlertDialog 显示 `v.formula`（线性文本）+ 标题 `v.label`。

- [ ] **Step 5: MemoryTest.kt + 振动**

```kotlin
package com.fincalc.app.ui.comp

import com.fincalc.app.core.expr.ExprEngine
import com.fincalc.app.state.CalcState
import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryTest {
    @Test
    fun `store recall and mem plus minus`() {
        val s = CalcState()
        ExprEngine.eval("3×4", s.exprContext())          // Ans=12
        Memory.store(s, "A")
        assertEquals(12.0, s.getVar("A"), 0.0)
        Memory.memPlus(s)                                // M += 12
        Memory.memPlus(s)                                // M += 12
        assertEquals(24.0, s.getVar("M"), 0.0)
        Memory.memMinus(s)                               // M -= 12
        assertEquals(12.0, s.getVar("M"), 0.0)
        // Ans 未被存储器操作改变
        assertEquals(12.0, s.getVar("Ans"), 0.0)
    }
}
```

振动：按键触发 `HapticFeedbackConstants.KEYBOARD_TAP`——在 Keypad 的 onClick 包一层 `haptic.performHapticFeedback(...)`（`LocalHapticFeedback.current`）。

- [ ] **Step 6: 测试 + 构建 + 提交**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest --tests "com.fincalc.app.ui.comp.MemoryTest"
./gradlew assembleDebug
git add app/src/main/java/com/fincalc/app/ app/src/test/java/com/fincalc/app/
git commit -m "feat(ui): STO/RCL/M+/M- 存储器 + 长按公式学习辅助 + 按键振动"
```

预期：1 测过；构建通过。

---

### Task 7: 收尾（README + 签名说明 + 整体验证）

**Files:**
- Modify: `README.md`（功能列表补全 + 构建说明 + 截图占位）
- Modify: `docs/superpowers/HANDOVER.md`（当前进度）

- [ ] **Step 1: README.md 更新**

补全功能列表（12 模式全量）、界面说明、自研排版器说明、构建方式（本机 .dev 工具链 / Android Studio / GitHub Actions）、许可。截图留占位（`docs/screenshots/`，真机截图后补）。双语保持。

- [ ] **Step 2: 签名说明**

README 的发布说明里写明：Release APK 为未签名包（计划 1 决策），debug APK 用调试签名可直接安装；正式签名密钥配置留待首次发布前（用户自行决定 keystore 方案）。

- [ ] **Step 3: 整体验证**

```bash
source .dev/env.sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

预期：全绿（213 + 计划 6 新增 ≈ 220+ 测）；APK 产出。

- [ ] **Step 4: 提交**

```bash
git add README.md docs/superpowers/HANDOVER.md
git commit -m "docs: README 功能补全 + 发布/签名说明 + 交接文档进度更新"
```

---

## 完成标准（计划 6 验收 = 项目功能全量）

- [ ] 12 模式全部有界面且可用：COMP（已有）+ SMPL/CMPD/CASH/AMRT/CNVR/COST/DAYS/DEPR/BOND/BEVN（六子模式）/STAT
- [ ] UI 重设计落地：屏:键 ≈ 1:3、闪烁光标 + ◀▶ + 触控定位、12 模式键面上置两行
- [ ] 金融模式操作逻辑与真机一致：▲▼ 选变量 → 输入（允许表达式）→ EXE 存入 → SOLVE 求解写回 VARS
- [ ] CASH/STAT 数据编辑器可用（80/40/26 行上限遵循设置）
- [ ] STO/RCL/M+/M- 与长按公式学习辅助可用
- [ ] `./gradlew testDebugUnitTest` 全绿；`./gradlew assembleDebug` 产出 APK
- [ ] 说明书例题端到端复核：每个模式至少一个例题经界面操作验证（真机走查，用户执行）
- [ ] 提交历史清晰，工作区干净
