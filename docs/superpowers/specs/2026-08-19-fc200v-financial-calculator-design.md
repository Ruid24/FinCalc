# FinCalc —— 对标卡西欧 FC-200V 的开源安卓金融计算器 · 设计文档

日期：2026-08-19
状态：已与用户逐段确认

## 1. 项目目标

开发一款开源安卓金融计算器 App，功能全量对标卡西欧 FC-200V（12 个计算模式），交互上模仿实体计算器（仿真键盘 + 多行显示屏），并在显示上超越实体机：用 LaTeX 排版实现"自然显示"（如幂函数显示为 $x^y$ 而非 `x^y`，类似 FX-991CN 的 textbook display）。项目托管 GitHub 开源（MIT 协议），通过 GitHub Releases 发布 APK 供安卓手机安装。

参考生态调研结论（2026-08-19）：GitHub 上无 FC-200V 的现成开源克隆；最接近的项目为 HP 12C 模拟器（RPN 范式不同）、CalcHub（印度个人理财表单类，非计算器仪器）、FinCalc（仅 FD/RD/EMI）。因此从零开发。

## 2. 关键决策（用户已确认）

| 决策点 | 结论 |
|---|---|
| 功能范围 | 一次做全 FC-200V 的 12 个模式 |
| 交互范式 | 仿真键盘 + 点阵屏风格（仅保留布局神韵，高分辨率矢量渲染，不做像素颗粒模拟），金融模式沿用"翻改变量 → 输入 → SOLVE"的实体机操作 |
| 界面语言 | 中英文双语（默认中文，设置中可切换；键面缩写保持英文） |
| 技术路线 | 方案 A：自研表达式引擎（语法树 → 数值 + LaTeX 双输出）+ 自研 Compose 排版器（Canvas 矢量绘制，无 WebView、零外部依赖）。注：原定 AndroidMath 库已不可用（GitHub 仓库下架、不在 Maven Central，2026-08-24 实测验证），经用户确认改为自研排版器 |
| 开源协议 | MIT |
| 发布方式 | GitHub Releases（GitHub Actions 云端自动构建 APK） |
| 本地构建 | 项目目录下 `.dev/` 内安装免安装命令行工具链（JDK 17 + Android SDK cmdline-tools），不进系统目录 |

## 3. 技术栈

- 语言：Kotlin
- UI：Jetpack Compose + Material 3（声明式 UI）
- 架构：MVVM；计算核心不依赖安卓框架，可纯 JUnit 测试
- LaTeX 渲染：自研 Compose 排版器（Canvas 矢量绘制；输入为 core/expr 自产的 LaTeX 子集对应的 AST）
- 持久化：Jetpack DataStore（设置与历史；无需数据库）
- 构建：Gradle (KTS) + GitHub Actions

## 4. 模块划分

- `core/expr` — 表达式引擎：分词器 → 递归下降解析器 → 语法树（AST）；AST 两个消费者：
  - 求值器：计算数值结果（双精度浮点）
  - 排版器：生成 LaTeX 字符串（`x^{y}`、`\frac{a}{b}`、`\sqrt{x}`、`\sum` 等），供自研排版器渲染
- `core/solver` — 数值求根：牛顿法为主、二分法兜底；用于 IRR、I%、债券收益率等无解析解场景
- `core/finance` — 12 个模式的公式引擎（纯函数，公式取自说明书"计算公式"章节）
- `state` — 计算器状态机：当前模式、Ans/M 存储器、变量 A~D/X/Y、金融变量 VARS、设置（Fix/Sci/Norm、角度单位、日期格式、日年基准等）、计算历史
- `ui` — 虚拟键盘、点阵屏风格显示屏、12 个模式界面、模式选择菜单
- `data` — DataStore 持久化（设置、历史记录）

## 5. 功能范围（12 模式，全部对标说明书）

- **COMP**：四则、百分比、三角/反三角、双曲、ln/log/eˣ/10ˣ、x²/xʸ/√/∛/ˣ√、Pol/Rec 坐标转换、阶乘、nPr/nCr、Rnd、Ran#、Abs、π/e、多语句（:）、Ans、独立存储器 M、变量 A~D/X/Y、历史回溯
- **SMPL**：Dys、I%、PV → SI / SFV；360/365 日年基准
- **CMPD**：n、I%、PV、PMT、FV、P/Y、C/Y、期初/期末；任求其一；不完整月数处理
- **CASH**：Csh 列表编辑器（≤80 项）→ NPV、IRR、NFV、PBP
- **AMRT**：与 CMPD 共享变量 → BAL、INT、PRN、ΣINT、ΣPRN
- **CNVR**：APR ⇄ EFF 名义/实际利率转换
- **COST**：CST、SEL、MAR 互求（含加价率）
- **DAYS**：日期间天数、日期±天数；360/365 基准；DMY/MDY/YMD 格式
- **DEPR**：直线法 SL、定率法 FP、年数总和法 SYD、余额递减法 DB（含月折算）
- **BOND**：PRC、YLD、应计利息；日期或年期输入；30/360 与 ACT 基准
- **BEVN**（6 子模式）：BEV（含利润/利润率设定）、MOS、DOL、DFL、DCL、QTY CONV
- **STAT**：单变量统计 + 7 种回归（线性、二次、对数、e 指数、ab 指数、幂、倒数）；X/Y/FREQ 数据编辑器

## 6. 界面设计

- 竖屏，上"屏"下"键"：
  - 屏幕区：约占屏幕高度 1/4（参考 Calc Business 的屏键比例，不过度挤压键盘）；COMP 模式第一行实时 LaTeX 排版输入表达式、第二行结果；金融模式为多行可滚动变量列表（当前行高亮），方向键移动、EXE 存入、SOLVE 求解，操作逻辑与实体机一致
  - 输入光标：屏幕区实时显示闪烁光标指示当前输入位置；◀▶ 方向键移动光标，也支持触控点击设置光标位置（2026-08-24 用户反馈）
  - 键盘区：仿 FC-200V 键面网格；SHIFT 切换第二功能（键面标签动态变化）；**金融模式直接印在键面上（仿 FC-200V 的 SMPL/CMPD/CASH/AMRT/COMP/STAT 与 CNVR/COST/DAYS/DEPR/BOND/BEVN 两行模式键），不藏进 MODE 二级菜单**；MODE 键保留作为辅助入口；按键振动与高亮反馈（2026-08-24 用户反馈：结合 FC-200V 与 Calc Business 布局）
- **显示风格**：屏幕区可保留卡西欧计算器的点阵屏神韵（深色液晶屏底色、布局与指示符位置），但**必须高分辨率渲染**——所有文字与公式用矢量字体/自研排版器（LaTeX 排版本身即矢量），禁止模拟低分辨率像素颗粒。这是手机 App 相对实体机的显示优势，显示效果对标 calc business 的清晰排版而非实体机的粗糙点阵。
- LaTeX 渲染：输入过程实时排版；自研排版器不支持的画面降级为线性文本
- 学习辅助：金融模式长按变量可查看该变量的计算公式（LaTeX 排版）
- 精度：内部双精度，显示模拟卡西欧 10 位有效数字与 Fix/Sci/Norm 规则

## 7. 测试策略

- `core/finance`、`core/expr`、`core/solver` 为纯 Kotlin，JUnit 单元测试
- 测试用例取自说明书例题（每个模式均有带标准答案的例题，作为权威验收标准）；目标：全部说明书例题通过
- 求解器单独测试收敛性与边界（如 IRR 多解/无解）
- UI 层不做强制自动化测试，以人工验证为主

## 8. 构建、发布与开源

- GitHub 仓库，MIT 协议，README 中英双语（含截图与功能列表）
- GitHub Actions：打版本标签（tag）即自动构建 APK 并发布到 Releases；调试签名，注明"个人学习项目"
- 本地开发：`.dev/` 内免安装工具链（JDK 17 + Android SDK cmdline-tools + Gradle wrapper），已 gitignore；用户可选装 Android Studio 学习，非必需

## 9. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 自研排版器渲染能力有限 | 排版子集自控（\frac/\sqrt/上下标/\pi/\mathrm 等，即 core/expr 排版器的全部输出）；不支持的片段降级为线性文本 |
| IRR/收益率求解不收敛 | 牛顿法 + 二分法兜底；边界返回说明书一致的错误提示（如 Math ERROR） |
| 12 模式工作量大 | 核心引擎优先（expr + CMPD/CASH/AMRT），每完成一个模式即过一遍说明书例题 |
| 本机无 Java/Android 环境 | `.dev/` 内免安装工具链 + GitHub Actions 云端构建双保险 |
