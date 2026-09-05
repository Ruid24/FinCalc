# FinCalc

[English](#english) | [中文](#中文)

## 中文

开源安卓金融计算器（Kotlin + Jetpack Compose）。支持全部 12 种计算模式，输入公式实时矢量排版自然显示（自研排版器，无 WebView、零外部渲染依赖）。

### 功能

- **COMP**：四则、百分比（卡西欧语义：`2500+15%`=2875）、三角/反三角、双曲、ln/log/eˣ/10ˣ、x²/xʸ/√/∛/ˣ√、Pol/Rec 坐标转换、阶乘、nPr/nCr、Rnd、Ran#、Abs、π/e、多语句（:）、Ans、变量 A~D/X/Y、独立存储器 M（M+/M-/STO/RCL）、历史回溯、Fix/Sci/Norm 显示
- **SMPL**：单利（Dys/I%/PV → SI/SFV，360/365 日年基准）
- **CMPD**：复利 TVM（n/I%/PV/PMT/FV 任求其一，P/Y、C/Y、期初/期末、奇数期 CI/SI）
- **CASH**：现金流列表（≤80 项）→ NPV/IRR/NFV/PBP
- **AMRT**：摊销（与 CMPD 共享变量 → BAL/INT/PRN/ΣINT/ΣPRN）
- **CNVR**：APR ⇄ EFF 利率转换
- **COST**：CST/SEL/MRG 互求
- **DAYS**：日期间天数、日期±天数（360/365 基准，MDY/DMY 格式）
- **DEPR**：直线法 SL、定率法 FP、年数总和法 SYD、余额递减法 DB（含月折算）
- **BOND**：债券 PRC/YLD/应计利息（日期或年期输入，360/365 基准，Annual/Semi）
- **BEVN**：损益分析六子模式（BEV/MOS/DOL/DFL/DCL/QTY CONV）
- **STAT**：单变量统计 + 7 种回归（线性、二次、对数、e 指数、ab 指数、幂、倒数），X/Y/FREQ 数据编辑器

### 特性

- 金融模式沿用实体机操作：▲▼ 翻改变量 → 输入（支持表达式）→ EXE 存入 → SOLVE 求解
- 输入公式实时 LaTeX 自然排版（自研排版器：AST → 盒式布局 → Canvas 矢量绘制；中间态自动降级为线性文本）
- 闪烁光标 + ◀▶ 移动 + 触控定位
- 金融模式长按变量查看计算公式
- 界面中英文双语（默认中文，设置中切换）
- 设置与历史持久化（Jetpack DataStore）

### 截图

（待补：见 `docs/screenshots/`）

### 构建

- Android Studio 打开本仓库，或命令行 `./gradlew assembleDebug`
- GitHub Actions：push 自动构建（Artifacts 可下载 debug APK）；打 `v*` 标签自动发布 Release

### 发布与签名说明

Release APK（CI 产出）为未签名包，不可直接安装；调试 APK 使用调试签名可直接安装（个人学习项目）。正式发布签名配置留待后续。

### 许可

MIT（详见 LICENSE）。

## English

An open-source Android financial calculator (Kotlin + Jetpack Compose). All 12 calculation modes, with real-time vector-typeset natural display of expressions (self-built typesetter — no WebView, zero external rendering dependencies).

### Features

- **COMP**: arithmetic, Casio-style percent (`2500+15%`=2875), trig/inverse/hyperbolic, ln/log/eˣ/10ˣ, powers/roots, Pol/Rec, factorial, nPr/nCr, Rnd, Ran#, Abs, π/e, multi-statement (:), Ans, variables A~D/X/Y, independent memory M (M+/M-/STO/RCL), history, Fix/Sci/Norm display
- **SMPL**: simple interest (Dys/I%/PV → SI/SFV, 360/365 basis)
- **CMPD**: TVM (solve any of n/I%/PV/PMT/FV; P/Y, C/Y, Begin/End, odd-period CI/SI)
- **CASH**: cash-flow list (≤80 items) → NPV/IRR/NFV/PBP
- **AMRT**: amortization (shared with CMPD → BAL/INT/PRN/ΣINT/ΣPRN)
- **CNVR**: APR ⇄ EFF conversion
- **COST**: CST/SEL/MRG
- **DAYS**: days between dates, date ± days (360/365, MDY/DMY)
- **DEPR**: SL / FP / SYD / DB (incl. monthly proration)
- **BOND**: PRC/YLD/accrued interest (date or term input, 360/365, Annual/Semi)
- **BEVN**: six break-even sub-modes (BEV/MOS/DOL/DFL/DCL/QTY CONV)
- **STAT**: 1-var statistics + 7 regressions (linear, quadratic, logarithmic, e-exp, ab-exp, power, reciprocal), X/Y/FREQ data editor

### Highlights

- Financial modes follow the real machine: ▲▼ navigate variables → input (expressions allowed) → EXE to store → SOLVE
- Real-time LaTeX-style natural display (self-built typesetter: AST → box layout → vector Canvas; graceful fallback to linear text mid-input)
- Blinking cursor + ◀▶ movement + touch positioning
- Long-press a variable in financial modes to see its formula
- Bilingual UI (Chinese default, switchable in settings)
- Settings & history persisted (Jetpack DataStore)

### Screenshots

(TBD: see `docs/screenshots/`)

### Build

- Open in Android Studio, or `./gradlew assembleDebug`
- GitHub Actions: every push builds (debug APK in Artifacts); pushing a `v*` tag publishes a Release

### Release & signing

Release APKs from CI are unsigned (not installable); debug APKs are signed with the debug key and installable (personal learning project). Proper release signing will be configured before the first official release.

### License

MIT (see LICENSE).
