# 计划 7：FC-200V 真机布局改造

> 来源：2026-09-10 用户反馈 + 确认的渲染预览图（`media/mockup_cmpd.png`、`media/mockup_comp.png`，生成脚本 `.dev/mockup.py`）。
> 目标：界面贴近 FC-200V 真机——浅色液晶屏、功能行（SHIFT/ALPHA/菱形方向盘/SETUP/ON）、SHORT CUT 行（含独立 SOLVE）、12 模式键上键面、ALPHA 红字层、字号加大。
> 约束：**引擎与金融逻辑零改动**；CASH/STAT 表格编辑屏保持现状（系统软键盘输入），本次只改 COMP 与金融变量屏的键盘与屏幕外观。

## 键面总谱（确认版，逐键对齐真机）

```
行0：SHIFT    ALPHA   [菱形方向盘,占2列×2行]  SETUP   ON
行1：SHORT CUT1(FMEM1) SHORT CUT2(FMEM2) [方向盘]  ESC  SOLVE(蓝,shift=S-MENU)
行2：SMPL CMPD CASH AMRT COMP STAT            （当前模式高亮）
行3：CNVR(A) COST(B) DAYS(C) DEPR(D) BOND BEVN  （红字=ALPHA层）
行4：(−)  RCL(STO)  ((%)  )(shift=,,alpha=X)  CTLG(VARS,alpha=Y)  M+(M−,alpha=M)
行5：7(eˣ)  8(ln)  9(CLR)  DEL(INS)  AC(OFF)
行6：4(x²)  5(√()  6(^()  ×  ÷
行7：1(sin()  2(cos()  3(tan()  +  −
行8：0(Rnd()  .(Δ%)  ×10ˣ(π,alpha=e)  Ans(DRG►)  EXE
```

- 行 0/1 为 6 列网格，方向盘占中间 2 列并跨两行（圆形底盘，▲▼◄► 菱形排布）。
- 行 2/3/4 为 6 列；行 5-8 为 5 列（真机数字区更宽）。
- shift 标注黄色印键帽上方、alpha 标注红色印下方（真机印刷风格，非激活也显示）。
- DEL/AC/SOLVE 蓝色键；当前模式键绿色高亮。

## 行为映射

| 键 | COMP 模式 | 金融模式（SMPL/CMPD/AMRT/DEPR/BOND/BEVN/CNVR/COST/DAYS） |
|---|---|---|
| ▲▼ | 历史回溯（historyBack/Forward） | 选行（moveUp/Down） |
| ◄► | 光标移动（moveLeft/Right） | 暂无行为（行内光标留待后续） |
| ON | 清屏 | 回 COMP + 清屏 |
| ESC | 清除输入 | 编辑中→取消编辑（editText=null）；否则回 COMP |
| EXE | execute() | exe() |
| SOLVE | 无操作 | solve() |
| S-MENU（shift+SOLVE） | 打开设置对话框 | 同左 |
| SETUP | 打开设置对话框 | 同左 |
| OFF（shift+AC） | activity.finish() | 同左 |
| DRG►（shift+Ans） | 角度单位 DEG→RAD→GRA 循环切换（AngleUnit 实际有三值，真机 DRG► 即三态循环；初稿误写两态，以代码为准） | 同左 |
| CTLG | 目录弹窗：上栏变量（A~D,X,Y,M,Ans）点击插入、下栏函数目录（x³ ∛( ˣ√( log( 10^( Abs( sin⁻¹( cos⁻¹( tan⁻¹( nPr nCr ! : Ran# Pol( 等现有 compKeys 里有但新键面放不下的功能）点击插入 | 同左（插入到 editText） |
| VARS（shift+CTLG） | 仅变量弹窗（等同现 VarPickerDialog） | 同左 |
| RCL | VarPickerDialog 选中插入（现有） | 同左 |
| STO（shift+RCL） | VarPickerDialog 选中 Memory.store（现有） | 同左 |
| SHORT CUT1/2 | 短按：跳转到绑定模式（未绑定→无操作或提示）；长按：绑定当前模式；shift：清除绑定 | 同左 |
| ALPHA | 进入 alpha 态（与 shift 互斥），下一次按键输入红字字母/常数后自动解除 | 同左 |
| INS（shift+DEL） | 不绑定功能（仅印刷，留待后续） | 同左 |
| Δ%（shift+.） | 若引擎支持 Δ%( 则插入 "Δ%("，否则插入 "%" 或不绑定——实现前先查 core/expr 支持的函数表 | 同左 |
| 数字/运算符/函数 shift 层 | 插入对应文本（e^(、ln(、²、√(、^(、sin( 等，参照现有 compKeys 的 insShift 文本） | 同左（插入 editText） |
| 模式键 12 个 | switchMode，当前模式高亮 | 同左 |

## Task 1：键盘基础设施

**文件**：`ui/keyboard/Keyboard.kt`、`state/CalcState.kt`、`ui/keyboard/KeyLayouts.kt`

1. `Key` 加字段：`alphaLabel: String? = null`、`onAlphaPress: (() -> Unit)? = null`、`color: KeyColor = KeyColor.NORMAL`（枚举：NORMAL/NUM/OP/MODE/MODE_ACTIVE/FUNC/BLUE）。
2. `CalcState` 加 `alpha: Boolean`（mutableStateOf，private set）+ `toggleAlpha()` + `clearAlpha()`；`toggleShift()` 时清 alpha，`toggleAlpha()` 时清 shift（互斥）；`switchMode` 同时清两者。所有 insert/delete/exe 等动作后 clearShift 处同步 clearAlpha（查现有 clearShift 调用点，逐个补）。注意 FinanceController/CompController 内的 clearShift 调用。
3. `Keypad` 重构：
   - 键帽同时印刷 shift（黄，顶部小字）与 alpha（红，底部小字）标注；
   - 激活优先级：alpha 态显示 alphaLabel 并触发 onAlphaPress；shift 态显示 shiftLabel 触发 onShiftPress；否则主 label；
   - 颜色按 KeyColor 取值（色板沿用现有 #232B25 系 + 蓝 #2B5EA7 + 高亮 #4E6B52）；
   - 字号加大：数字/主标签/模式/标注分别比现有大 2-4sp（现有统一 13sp → 数字 20sp、主 16sp、模式 14sp、标注 9sp；测试机型 1080×2400，注意 maxLines=1 防溢出，SHORT CUT 允许两行）。
4. 新组件 `DPad`（方向盘）：圆形底盘（CircleShape Surface）+ 四向键（▲▼◄► 四键菱形偏移：▲ 上中、▼ 下中、◄ 左中、► 右中，各键为小圆/方块，点击触发对应回调，带振动反馈）。
5. 新 Composable `TopFunctionRows(row0左2键, row1左2键, row0右2键, row1右2键, dpad回调)`：6 列网格中，左 2 列与右 2 列放功能键，中间 2 列跨 2 行放 DPad。

## Task 2：统一键面与行为接线

**文件**：`ui/keyboard/KeyLayouts.kt`（重写）、`MainActivity.kt`、`ui/comp/CompScreen.kt`、`ui/dialogs/`（新 CatalogDialog）、`data/Prefs.kt`

1. `fc200vKeys(state, comp: CompController?, fin: FinanceController?, onFinish, ...)`：按总谱生成行 2-8 共 7 行（行 0/1 由 `fc200vTopRows` 生成，合计 9 行）；行为按上表按模式分发（comp/fin 二选一非空）。
2. `modeKeyRows` 改造：加当前模式高亮（KeyColor.MODE_ACTIVE）。
3. `CatalogDialog`（新）：变量区（A~D、X、Y、M、Ans 显示现值，点击插入变量名）+ 函数目录区（从现有 compKeys 收纳的函数列表，点击插入文本）；`VARS` 弹只含变量区的简化版（可复用 VarPickerDialog）。
4. SHORT CUT：CalcState 加 `shortcut1/shortcut2: Mode?`（mutableStateOf）；Prefs 加两个 stringPreferencesKey 持久化；长按绑定=当前模式、短按 switchMode、shift 清除。
5. ON/ESC/OFF/DRG►/S-MENU 行为按表实现；`Δ%(` 支持性先查 `core/expr`（Tokenizer/Parser/Evaluator 的函数表），不支持则该 shift 不绑定。
6. MainActivity：FinanceModeBody 键盘换 `fc200vKeys`；CompScreen 同。ModeDialog 入口移除（模式键已上键面），但 ModeDialog 文件保留不删。
7. 键盘与屏的 weight 从 3:1 改 4:1（屏约 20%）。

## Task 3：浅色液晶屏 + 测试

**文件**：`ui/finance/FinanceScreen.kt`、`ui/comp/CompScreen.kt`、`app/src/test/...`

1. 屏区改浅色液晶：背景 #FFD8E2CE、主文字 #FF141A12、选中行反色（深底 #FF1A221A 浅字 #FFD8E2CE）、错误文字保持可读红（深红 #FF8C3A2E）；COMP 屏状态行/输入行/结果行同色板。光标保持红色闪烁。
2. 测试：
   - `CalcStateTest` 补 alpha 态用例（toggle 互斥、按键后自动解除）；
   - 键面 smoke test：fc200vKeys 行数=7（行 2-8；另 fc200vTopRows 供行 0/1）、行 5-8 列数=5、关键键存在（SOLVE 独立键、CTLG 在行 4、模式行 12 键）；
   - SHORT CUT 绑定/跳转/清除的状态机用例（Prefs 持久化不测，依赖 Context）；
   - 现有 223 测保持全绿（键面重排不应影响控制器/引擎测试）。
3. `./gradlew testDebugUnitTest assembleDebug` 全绿。

## 验收

- 走查对照 `media/mockup_cmpd.png`：布局、配色、标注位置一致；
- 真机 `./gradlew installDebug` 验证：SOLVE/ESC/ON/SHORT CUT/CTLG/ALPHA/DRG► 行为；
- 全部测试绿 + APK 构建通过。

## 修订记录

- **Task 1 双审查后修复**（规格审查：全条符合；质量审查：无阻断、3 项应修）：
  1. COMP 模式 5 个键（◀▶ = ▲▼）的 onPress 补 `clearModifiers()`——既有遗漏，alpha 引入后 SHIFT/ALPHA 卡亮影响翻倍（CompScreen.kt）；
  2. DPad 四向键 `fillMaxHeight(0.38f)` 相邻点击区重叠约 7% → 降 0.33f（DPad.kt）；
  3. `CalcState.clearShift()` 全局替换后成死代码且只清 shift 属"诱捕器" → 删除（CalcState.kt）。
  质量审查另记录两条留待 Task 2/3 处理：状态行补 ALPHA 指示符；颜色常量待 Task 3 换屏色板时一并收口。
- **Task 2 双审查后修复**（规格审查：1 项中等；质量审查：无阻断、4 项应修）：
  1. `⁻¹`（倒数）功能入口丢失（旧 compKeys 有、新键面未收纳）→ 补入 CatalogDialog 函数目录（两个审查同时发现）；
  2. `fc200vKeys` 的 `onOpenSettings` 死参数删除（SETUP/S-MENU 均在 fc200vTopRows）+ 两个调用点同步；
  3. 金融模式 DPad ◄► 空操作分支补 `clearModifiers()`（武装态挂起隐患）；
  4. `ModeDialog.kt` 整文件死代码删除 + 双语 `mode_select` 字符串清理；
  5. SHORT CUT `onLongPress` 补 `clearModifiers()`（绑定后修饰态挂起）；
  6. "旧 compKeys" 注释三处改为"改造前 compKeys（已删除）"。
  确认项：DRG► 实为 DEG/RAD/GRA 三态循环（计划初稿误写两态，已回改上文行为映射表）；INS/Δ% 仅印刷不绑定，shift 按下回落主功能（KeyCap 既定回退语义）。
  记录未修（后续任务候选）：CASH/STAT 屏无 SETUP/ESC/ON 入口不一致；弹窗四元组在 CompScreen/MainActivity 重复（可抽 CalcDialogsHost）；键表每次重组重建（既有设计）。
- **Task 3 双审查后修复**（规格审查：通过；质量审查：无阻断、2 项应修）：
  1. DPad 四向箭头 `Color.White` 漏收口 → 改用 `KEY_TEXT`；
  2. SHORT CUT 绑定路径补键级测试（`onLongPress` 回调绑定当前模式 + 修饰态清除断言）；
  顺手清理：InputLine 未使用的 TextStyle import；Theme.kt 补 KEY_FUNC/SCREEN_SEL_TXT 耦合意图注释。
  计划文本回改：fc200vKeys 实为行 2-8 共 7 行（行 0/1 由 fc200vTopRows 生成），初稿"9 行"表述已修正。
  光标颜色纠偏说明：任务书"保持红色光标"与史实不符（光标从未红过），按渲染图落地为 `SCREEN_CURSOR` 红。
- **Task 4 双审查后修复**（规格审查：通过；质量审查：1 项阻断 + 3 项建议，全部修复）：
  1. **B1（阻断）**：即输即改的自引用表达式（如 A+1）被中间提交重复求值（EXE 时 11→12）→ `editBase` 快照机制：编辑开始时记录本行变量原值，commitEdit 求值时本行变量以快照为准，exe/select/clear/solve 清快照（FinanceController.kt）。附带发现：金融变量键（n、I%、PV…）不在表达式变量体系内（tokenizer 仅认 ABCDXYM/Ans/e），真实自引用不可达，快照为防御性修复；测试用手工 spec（key=A）覆盖幂等性。
  2. **S1**：旋转手势加最小半径门限（<30% 半径只更新基准角不累积），防直线划过圆心/抖动连发（DPad.kt）。
  3. **S2**：圆心坐标改为在 onDrag/onDragStart 内读 PointerInputScope.size（动态属性，分屏 resize 不过期），替代启动时缓存（DPad.kt）。
  4. **S3**：FinanceController.delete() 补清 errorText/resultText，与 insert 对称。
  规格审查注记在案：4.1.1 规格文本"行权重 0.82→1.0"与代码史实不符（0.82 从未存在，TopFunctionRows 本就均分），终态（9 行等高）符合意图。
  测试 235 → 241（Task 4 新增 4 项即输即改 + 2 项自引用幂等），全绿。

## Task 4：真机走查修复与功能增强（2026-09-10 用户真机反馈）

> 来源：真机截图 `media/Screenshot_2026-09-10-14-32-40-056_com.fincalc.ap.jpg` + 用户反馈。

### 4.1 UI 修复

1. **SHORT CUT1/2 文字重叠**：FMEM 黄标与两行主标签在三行空间内重叠。修法：行 0/1 权重 0.82→1.0（与其他行同高）；SHORT CUT 两行主标签缩小字号（约 11sp）；KeyCap 对"shift 标注 + 两行 label"组合重排垂直布局（标注 ~0.14h、label 两行中心 ~0.42h/0.74h），保证三行互不重叠。
2. **键帽形状**：鹅卵石（RoundedCornerShape 50%）→ 小曲率圆角矩形（RoundedCornerShape(12.dp)，对照渲染图 radius≈11%）。DPad 底盘与四向键保持圆形不变。
3. **SHIFT/ALPHA 键常态文字着色**（用户两次澄清后定稿）：SHIFT 键文字黄色 `KEY_SHIFT_MARK`、ALPHA 键文字红色 `KEY_ALPHA_MARK`（对齐渲染图与真机印刷色；整键底色方案被文字着色方案取代）。实现：Key 加 `labelColor: Color? = null`（null=默认 KEY_TEXT），KeyCap 应用；SHIFT/ALPHA 两键传入。激活态行为不变（shift 态带 shiftLabel 键 #39493B 底、alpha 态 #4B3230 底）。

### 4.2 方向盘旋转手势

- DPad 圆盘加单指画圈检测：`pointerInput` + `detectDragGestures`，逐帧计算触点相对圆心的角度差并累积；累积超过阈值（25°）触发一次回调并扣减（支持连续快速旋转多次触发）。
- 回调语义：`onRotateCW`（顺时针）/`onRotateCCW`（逆时针）。接线（fc200vTopRows 加这两个参数）：
  - COMP 模式：CW=moveRight、CCW=moveLeft（光标左右）；
  - 金融模式（上下选择菜单）：CW=moveDown、CCW=moveUp。
- 每次触发视同一次按键：清修饰态 + KEYBOARD_TAP 振动。
- 四向键点击行为保持不变（旋转是增量手势，不替代点击）。

### 4.3 金融模式即输即改（免 EXE）

FinanceController 语义调整（真机 EXE 确认保留，但输入即时生效）：

- `insert`/`delete` 后：尝试 `ExprEngine.eval(editText, ...)`，**成功立即 setVar 写回当前行变量**（integer 行 round）；中间态/非法表达式静默保持旧值（不报错误）。
- `select`/`moveUp`/`moveDown`：先提交当前编辑（eval 成功才写回），再移动选中行——不再丢弃未确认输入。
- `exe()` 保持现有行为（提交 + 错误提示）。
- 测试：FinanceControllerTest 新增/更新用例（输入即写回、移动提交、非法中间态不写回、integer 行 round）；确认 TvmModesTest 等现有金融测试不破。

### 4.4 验收

- 真机对照截图复查 4.1 三项；
- 旋转手势两模式手感；即输即改全流程；
- 全部测试绿 + assembleDebug。
