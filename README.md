# CustomTamingFramework

维护自定义驯兽框架及其配置、实体交互与游戏内容。

## 工程布局

- Gradle 工程：[CustomTamingFramework](./CustomTamingFramework/)
- 工程详细说明：[CustomTamingFramework/README.md](./CustomTamingFramework/README.md)
- 项目注册信息：[PycodersMod.projects.json](../../PycodersMod.projects.json)
- 工作区统一测试入口：[launch.ps1](../../launch.ps1)
- 启动参数填写说明：[launch-parameters.md](../../launch-parameters.md)

## 构建

在 CustomTamingFramework 目录执行 .\gradlew.bat clean build。

工程使用 Gradle Java Toolchain 声明所需 Java 版本。机器本地的 JDK 路径位于工作区 local-config，不写入 Mod 仓库。

## 标识

| 项目 | 值 |
|---|---|
| Minecraft | 1.20.1 |
| Loader | Forge 47.4.20 |
| Java | 17 |
| Mod ID | custom_taming_framework |
| Java Package | com.pycoder.customtamingframework |

Mod ID、Registry Namespace 和存档标识保持原值。工程目录、Gradle group、Java package 和 GitHub 仓库名属于工程组织信息。


## ????

# v0.1.5-alpha

## What this version is for

This version continues from the cleaned `0.1.4-alpha` baseline. The branch is centered on controlled pet-owner management, permission-aware interaction, and the next expansion of the modular tab system.

## In progress / planned for this version

- Build a controllable workflow for changing a pet's owner.
- Replace the old transfer-only entry with a dedicated pet permission management GUI.
- Move to per-player permission control for each instantiated pet: owner, read-only, editable, and private.
- Keep online / offline players grouped in the permission list, pin the owner to the first row, and auto-add newly seen players.
- Make transfer a dedicated confirmation flow that saves immediately and refreshes the current permission view.
- Remove the old `Shift + L` global permission cycling logic.
- Keep permission-aware broadcasts and death broadcasts aligned with the new ownership model.
- Continue expanding tab modules, especially the inventory module and its topmost container overlay.
- Prepare a future path where pets can help their owners complete eligible advancements and contribute their non-death statistics to the owner.
- Preserve the release rule that ship builds must not contain shell-based launch helpers.

## 这个版本做什么

`0.1.5-alpha` 继续沿着已经整理好的 `0.1.4-alpha` 基础推进，重点放在宠物主人修改通路、按玩家逐个管理的权限系统，以及标签页模块的继续扩展上。

## 进行中 / 计划内内容

- 宠物权限管理 GUI，替代原先单一的“转赠”入口。
- 按玩家逐个保存权限：主人、只读、可编辑、保密。
- 在线 / 离线玩家分组显示，主人固定第一行，新加入玩家自动补入权限表。
- 转赠确认流程：确认后立即保存，并同步原版 owner 数据。
- 删除旧的 `Shift + L` 全局循环切换逻辑。
- 继续扩展标签页模块类型，重点推进物品栏模块与其顶层容器覆盖层。
- 为宠物协助完成进度、把非死亡统计归并给主人预留完整链路。
- 保持正式发布包不包含基于 shell 的启动辅助代码。

## 说明

- 这个文件记录的是 `0.1.5-alpha` 版本的开发方向与进行中内容，不等同于最终发布日志。
- 等实现继续落地后，再把真正完成的内容补进正式更新日志。


---

# v0.1.4-alpha

## Why `0.1.3-alpha` was rejected

The previous `0.1.3-alpha` upload was rejected because two GUI classes relied on PowerShell-based image selection helpers. This release removes that shell-driven path and replaces it with a pure Java file dialog so the build stays free of local launch code.

## What this release adds

v0.1.4-alpha completes the settings-and-manual phase that followed the modular GUI work from `0.1.3-alpha`. The release adds an in-game editor for the new `settings` section, expands the manual system, and tightens the permission and data-display rules used by both the general and pet configuration screens.

## Major additions

- Added a top-level `settings` section to `basic.snbt` and a dedicated in-game editor for it.
- Added a new `User Settings` entry on the general configuration screen for OP players.
- Added visibility switches so non-OP players can independently hide or show the general configuration entry points, the pet list, the food list, the extra food list, the taming core, the data list, and the total-value editor.
- Added synchronized tab-module and data-variable behavior so the general configuration screen and pet configuration screen keep the same rules in both places.
- Added a full data-list configuration tool with cumulative / stage total-value modes, stage naming, lower bounds, infinity handling, reverse progression, and stage-count / cumulative-count behavior.
- Added `CTF用户手册` and `CTF管理员手册` as full mod items with cover, table of contents, chapter navigation, and return-to-index behavior.
- Added `manual.snbt` as a content file for both manuals, separating cover text from chapter text.
- Added a hidden internal record plus a visible advancement for manual unlock so the book unlock can drive both recipe rewards and player-facing feedback.
- Added player-name-based owner display to the pet screen, with UUID kept only as a fallback.
- Replaced the shell-based image selection helper with a standard Java file dialog, so the release build does not bundle shell startup logic.

## Release notes

- The release package is meant to stay free of local launch helpers and shell-driven startup code.
- The work in this version focuses on in-game behavior, not on machine-specific debugging shortcuts.
- Future work will continue from the `0.1.4-alpha` baseline with owner-edit workflows and more module types.

---

# v0.1.4-alpha

## 为什么 `0.1.3-alpha` 被拒

上一个 `0.1.3-alpha` 上传被审核拒绝，原因是两个 GUI 类里还在使用基于 PowerShell 的图片选择辅助逻辑。这个版本把这条 shell 驱动路径移除，改成纯 Java 文件选择器，正式构建里不再包含本机启动代码。

## 这个版本新增了什么

`0.1.4-alpha` 完成了 `0.1.3-alpha` 之后的设置与手册阶段。这个版本新增了 `basic.snbt` 里的 `settings` 栏目对应的游戏内可编辑内容，同时继续统一通用配置界面和宠物配置界面的可见性、权限和数据展示规则。

## 主要新增

- 在 `basic.snbt` 中新增顶层 `settings` 栏目，并做成对应的游戏内设置编辑界面。
- 在通用配置界面中新增 `用户设置` 入口，供 OP 玩家编辑。
- 为非 OP 玩家新增独立可见性开关，分别控制通用配置入口、宠物列表、食物列表、更多食物、驯兽核心、数据列表和总值设置。
- 让通用配置界面与宠物配置界面的标签页模块和数据变量规则继续保持同步。
- 将数据列表扩展成完整的配置工具，支持累计 / 阶段总值模式、阶段命名、下限、无穷大、可逆、阶段计数和累计计数。
- 新增 `CTF用户手册` 和 `CTF管理员手册` 两本物品，拥有完整的书本式翻页、目录跳转和返回目录能力。
- 新增 `manual.snbt` 作为手册内容配置文件，将封面简介和章节正文分开保存。
- 新增隐藏记录与可见成就两条手册获取路径，既保留内部状态，也保留玩家能看到的奖励提示。
- 新增宠物界面左侧的“主人”显示，优先显示玩家名字，而不是直接显示 UUID。
- 将图片选择改为使用标准 Java 文件选择器，正式版里不再包含任何 shell 启动辅助代码。

## 发布说明

- 正式包中不应包含任何本机启动脚本或 shell 驱动的调试逻辑。
- 这个版本的重点是游戏内行为，不是机器专用的开发辅助流程。
- 下一阶段会继续以 `0.1.4-alpha` 为基础，推进宠物主人修改方式和标签页模块扩展。


---

# v0.1.3-alpha

## What this version is for

v0.1.3-alpha upgrades the CTF GUI from a tabbed text configuration surface into a modular visual editor. The general configuration screen and the pet configuration screen now share the same right-panel tab system, module rendering pipeline, data-variable definitions, and OP / non-OP permission model.

The main goal of this release is to make the GUI itself configurable: OP players can design tab content with reusable modules, define named data variables, and decide how each variable should display its current value and total value. Non-OP players can browse the parts that are allowed by configuration, while editing controls stay locked.

## Major changes since v0.1.2-alpha

### Shared Tab Module System

- The right side of `GeneralConfigScreen` and `PetScreen` now uses the same tab container and module model.
- OP players can right-click inside a tab page to add module blocks.
- Each tab can contain multiple modules of the same type. Plain text, image, and data modules are no longer limited to one instance per tab.
- OP players can drag modules within the tab body, right-click existing modules to configure them, and save the resulting layout into the GUI SNBT data.
- Images render behind text and data modules so they can be used as backgrounds, labels, icons, or decorative visual anchors.

### Plain Text Modules

- Plain text modules provide simple editable text blocks inside tab pages.
- They are kept lightweight and are mainly intended for labels, notes, short instructions, or section headers inside a configured tab.
- Plain text modules participate in the same drag, save, and repeated-module workflow as the other module types.

### Image Modules

- Image modules can be added from the tab right-click menu and configured through a dedicated image editor popup.
- The image editor uses a native file picker for selecting PNG files.
- Selected images are copied into the mod-managed resource directory and then referenced by stable file name in the saved GUI data.
- The configuration popup shows a live preview on the left and file / size controls on the right.
- Width and height are controlled by sliders instead of numeric text boxes.
- Width and height are independent: resizing can stretch the image horizontally or vertically instead of forcing proportional scaling.
- The preview area clamps slider ranges to the actual preview box, so the preview size matches the final tab rendering more predictably.
- Image loading is cached for rendering, and missing or failed images show a safe placeholder instead of crashing the screen.

### Data Modules

- Data modules bind a tab module to a key in `data_variables`.
- The data module editor validates the entered key against the current data-variable list.
- Invalid keys are highlighted and cannot be saved.
- The display format uses a segmented choice between `current value` and `current value / total value`.
- New data modules default to the current-value display mode.
- In tab rendering, data modules automatically widen to fit `name:value` or `name:current/total(stage name)` on one line when possible.
- Data module contents refresh after the data list is changed, so updated initial values and total rules do not require reopening the whole screen.

### Data Variable List

- A dedicated data-list screen is available from the general configuration screen and the pet configuration screen.
- The data list is now a configuration editor, not a live pet-instance editor.
- Each variable stores an initial value. That initial value is the value assigned when a concrete pet instance is created; it is not the same thing as a live instance's current value.
- Adding and editing variable values validates numeric input before saving.
- Numeric validation allows one leading minus sign and one decimal point. Invalid characters, misplaced minus signs, or multiple decimal points are rejected and highlighted.
- Saved numeric text is normalized, including removal of unnecessary leading zeroes and normalization of `-0` to `0`.
- The list displays each variable's initial value and whether its total rule is cumulative or staged.
- OP players can add, edit, save, and cancel changes.
- Non-OP players can open the list only when the relevant visibility setting allows it; in that mode the add row, save button, and edit buttons are hidden.

### Total Value Rules

- Each data variable now stores both `current` and `total` values.
- Total-value configuration is opened from the data list through a full-screen rule editor.
- The rule editor supports two total modes:
  - **Cumulative mode**: increases to current value also increase total value; decreases to current value do not reduce total value.
  - **Stage mode**: current value is evaluated against configured thresholds, and the displayed total becomes the current stage's total target.
- Stage mode supports named finite stages. Empty stage names automatically fall back to default names such as `一阶`, `二阶`, and so on.
- Stage mode includes an infinite stage. When enabled, values beyond the highest finite stage display total as `∞` with the configured infinite-stage name.
- Stage mode includes a minimum value row. If `no lower bound` is disabled, decreases cannot push current value below the configured minimum.
- The `no lower bound` toggle preserves the typed minimum value while locking the input field.
- The `reversible` toggle controls whether a variable can fall back to a lower stage when current value decreases.
- `Cumulative count` evaluates stages by absolute accumulated current value.
- `Stage count` resets current value when moving into a higher stage; when reversible, falling to a lower stage resumes counting from that lower stage's total.
- Stage thresholds must be strictly increasing from top to bottom. Invalid rows are highlighted and cannot be committed.
- Non-OP players can view the total-rule screen only when allowed by settings; all editing controls are locked or hidden except the close button.

### General and Pet Configuration Sync

- `GeneralConfigScreen` and `PetScreen` now share recent module behavior, data-list behavior, total-rule behavior, and non-OP read-only behavior.
- The pet configuration screen's one-click import now imports both general tab modules and the full `data_variables` definition from the general configuration.
- Imported data-variable definitions include current / total fields, cumulative or stage mode, stage names, minimum rules, infinite-stage settings, reversible rules, and count mode.
- Pet settings now persist data-variable definitions separately from tab pages, preventing `data_variables` from being mistaken for a normal tab.
- When a pet configuration screen is opened, its GUI SNBT includes the pet's configured `data_variables` for OP editing and module validation, while runtime instance data remains separate.
- A new OP-only "User Settings" entry opens a dedicated settings GUI from the left side of the general configuration screen.
- The settings screen uses a search bar and scrollable list of fixed boolean options, with dependent items locking automatically when their parent option is disabled.

### Non-OP Visibility Settings

- `basic.snbt` now has a top-level `settings` section alongside `pets`, `taming_core`, and `pet_foods`.
- This section is a foundation for the future in-game settings editor planned for the next version.
- v0.1.3-alpha defines six boolean visibility switches, all defaulting to `true`:
  - `show_non_op_pet_list_button`
  - `show_non_op_food_list_button`
  - `show_non_op_more_food_button`
  - `show_non_op_taming_core_button`
  - `show_non_op_data_list_button`
  - `show_non_op_total_settings_button`
- The first four switches control non-OP visibility inside the general configuration flow.
- The data-list and total-settings switches apply to both the general configuration screen and the pet configuration screen.
- No GUI editor for this `settings` section is included yet; that is reserved for v0.1.4-alpha.

### SNBT and Compatibility

- `GeneralGUI.snbt` and pet setting SNBT now preserve data variables in a structured state format.
- Older flat data-variable values can still be read and upgraded into the new state format.
- Pet setting files preserve nickname data while allowing general GUI content and data-variable definitions to be imported.
- The config writer keeps `data_variables` separate from tab entries to avoid invalid tab conversion and accidental GUI corruption.

## Current scope

v0.1.3-alpha completes the first usable modular GUI editor and data-variable configuration layer. It does not yet include the future visual logic editor, full skill mechanics, equipment evolution gameplay, or the planned in-game editor for the new `settings` section.

---

# v0.1.3-alpha

## 这个版本做什么

v0.1.3-alpha 将 CTF 的 GUI 从标签页文本配置界面升级为模块化可视编辑器。通用配置界面和宠物配置界面现在共用右侧标签页系统、模块渲染流程、数据变量定义和 OP / 非 OP 权限模型。

这个版本的核心目标是让 GUI 本身可以被配置：OP 玩家可以用可复用模块设计标签页内容，定义带名称的数据变量，并决定每个变量如何显示当前值和总值。非 OP 玩家只能浏览配置允许显示的部分，所有编辑控件都会保持锁定。

## 自 v0.1.2-alpha 以来的主要变化

### 共享标签页模块系统

- `GeneralConfigScreen` 和 `PetScreen` 的右侧区域现在使用同一套标签页容器和模块模型。
- OP 玩家可以在标签页页面中右键添加模块块。
- 同一个标签页可以添加多个同类型模块；纯文本、图片、数据模块都不再限制为每种只能存在一个。
- OP 玩家可以在标签页内拖动模块，右键已有模块进行配置，并把布局保存进 GUI SNBT 数据。
- 图片模块会渲染在文本和数据模块下方，因此可以作为背景、标识、图标或装饰性视觉元素。

### 纯文本模块

- 纯文本模块提供标签页内的简单可编辑文本块。
- 它主要用于标签、注释、简短说明或页面内分区标题。
- 纯文本模块与其他模块共用拖动、保存和重复添加流程。

### 图片模块

- 图片模块可以从标签页右键菜单添加，并通过专用图片编辑弹窗配置。
- 图片编辑器使用原生文件选择器选择 PNG 文件。
- 被选择的图片会复制到模组管理的资源目录，再以稳定文件名写入 GUI 数据。
- 配置弹窗左侧显示实时预览，右侧显示文件和尺寸控制。
- 宽度和高度改用滑块控制，不再使用数字输入框。
- 宽度和高度相互独立：调整尺寸可以横向或纵向拉伸图片，不再强制等比例缩放。
- 预览区域会把滑块范围限制在实际预览框内，使预览尺寸和标签页渲染结果更一致。
- 图片加载使用缓存；图片缺失或加载失败时会显示占位内容，不会让界面崩溃。

### 数据模块

- 数据模块把标签页中的模块绑定到 `data_variables` 的某个键。
- 数据模块编辑器会根据当前数据变量列表校验输入键名。
- 无效键名会标红，且无法保存。
- 显示格式使用紧贴式分段按钮，在“当前值”和“当前值 / 总值”之间切换。
- 新增数据模块默认选择“当前值”显示模式。
- 标签页渲染时，数据模块会尽量自动扩展宽度，以便将 `变量名:变量值` 或 `变量名:当前值/总值（阶段名）` 放在一行显示。
- 数据列表修改后，标签页中的数据模块会跟随更新，不需要重新打开整个界面。

### 数据变量列表

- 通用配置界面和宠物配置界面都可以打开专用数据列表。
- 数据列表现在是配置编辑器，不是实时宠物实例编辑器。
- 每个变量保存一个初始值。初始值表示具体宠物实例创建时写入当前值的默认起点，不等同于运行时实例的当前值。
- 添加和编辑变量值时会在保存前校验数字格式。
- 数字校验允许一个前置负号和一个小数点。非法字符、负号位置错误或多个小数点都会被拒绝并标红。
- 保存后的数字文本会按数学写法规整，包括去掉多余前导零，并把 `-0` 规整为 `0`。
- 列表行显示变量初始值，以及该变量当前使用“累计”还是“阶段”总值规则。
- OP 玩家可以添加、编辑、保存或取消修改。
- 非 OP 玩家只有在对应可见性设置允许时才能打开数据列表；此模式下添加行、保存按钮和编辑按钮都会隐藏。

### 总值规则

- 每个数据变量现在都保存 `current` 和 `total` 两个值。
- 总值设置从数据列表进入，并使用独立的全屏规则编辑界面。
- 规则编辑器支持两种总值模式：
  - **累计模式**：当前值增加时，总值同步增加；当前值减少时，总值不会降低。
  - **阶段模式**：当前值按配置阈值判断所在阶段，显示总值会变成当前阶段的总值目标。
- 阶段模式支持命名的有限阶段。阶段名称为空时，会自动使用 `一阶`、`二阶` 等默认名称。
- 阶段模式包含无穷大阶段。启用后，超过最高有限阶段的数值会以 `∞` 作为总值，并显示对应阶段名称。
- 阶段模式包含最小值行。未勾选“无下限”时，当前值降低不能低于设定最小值。
- “无下限”会保留已经输入的最小值，但会把输入框锁定为灰色。
- “可逆”决定当前值降低时，变量能否掉回更低阶段。
- “累计计数”按照绝对累计当前值判断阶段。
- “阶段计数”在进入更高阶段后会把当前值从本阶段重新计数；如果同时允许可逆，掉回低阶段时会从低阶段总值继续扣减。
- 阶段阈值必须从上到下严格递增。无效行会标红，且无法提交。
- 非 OP 玩家只有在设置允许时才能查看总值设置界面；除关闭按钮外，其余编辑控件会锁定或隐藏。

### 通用配置与宠物配置同步

- `GeneralConfigScreen` 和 `PetScreen` 已同步近期模块行为、数据列表行为、总值规则行为和非 OP 只读行为。
- 宠物配置界面的一键导入现在会导入通用配置中的标签页模块和完整 `data_variables` 定义。
- 被导入的数据变量定义包含当前值 / 总值字段、累计或阶段模式、阶段名称、下限规则、无穷大设置、可逆规则和计数模式。
- 宠物设置现在会把数据变量定义与标签页页面分开保存，避免 `data_variables` 被误当成普通标签页。
- 打开宠物配置界面时，GUI SNBT 会包含宠物配置级的 `data_variables`，用于 OP 编辑和数据模块校验；运行时实例数据仍然保持独立。
- 通用配置界面左侧新增了仅 OP 可见的“用户设置”入口，会打开独立的设置 GUI。
- 这个设置界面使用搜索框和可滚动列表来编辑固定的布尔开关，附属项会在父项关闭时自动锁定。

### 非 OP 可见性设置

- `basic.snbt` 新增顶层 `settings` 栏目，与 `pets`、`taming_core`、`pet_foods` 并列。
- 这个栏目是为下一版本的游戏内设置界面预留的基础结构。
- v0.1.3-alpha 先定义 6 个布尔可见性开关，默认全部为 `true`：
  - `show_non_op_pet_list_button`
  - `show_non_op_food_list_button`
  - `show_non_op_more_food_button`
  - `show_non_op_taming_core_button`
  - `show_non_op_data_list_button`
  - `show_non_op_total_settings_button`
- 前 4 个开关控制通用配置流程中非 OP 玩家可见的入口。
- 数据列表和总值设置两个开关会同时影响通用配置界面和宠物配置界面。
- 这个 `settings` 栏目暂时没有 GUI 编辑器；对应编辑界面计划放到 v0.1.4-alpha。

### SNBT 与兼容性

- `GeneralGUI.snbt` 和宠物设置 SNBT 现在会用结构化状态格式保存数据变量。
- 旧式扁平数据变量值仍可读取，并会升级为新的状态结构。
- 宠物设置会保留昵称数据，同时允许导入通用 GUI 内容和数据变量定义。
- 配置写出逻辑会把 `data_variables` 与标签页条目分开，避免非法标签页转换和 GUI 数据损坏。

## 当前范围

v0.1.3-alpha 完成了第一版可用的模块化 GUI 编辑器和数据变量配置层。它还不包含后续计划中的可视逻辑编辑器、完整技能机制、装备进化玩法，以及新增 `settings` 栏目的游戏内编辑界面。


---

# v0.1.2-alpha

## What this test version is for

v0.1.2-alpha transforms the CTF configuration system from static preset files into an interactive, GUI-driven experience. Where v0.1.1-alpha introduced the SNBT config directory and tabbed GUI, this version adds the ability to browse, search, and reconfigure every aspect of the mod through dedicated sub-screens — no file editing required.

The core architectural change is that CTF_Settings is now fully self-sufficient: it no longer depends on any mod-internal test items or hard-coded defaults. The config system can generate every living entity type as an independently toggleable pet candidate, categorise foods interactively, and persist custom item effects — all through network-synced GUIs.

## What has been implemented since v0.1.1-alpha

### Config Sub-screen Architecture

The general configuration screen became a hub with 3 navigation buttons in its left panel:
- **宠物列表 (PetList)** — browse, search, and toggle CTF capability for every living entity type in the game
- **食物列表 (FoodList)** — classify each food as normal, special, or disabled, with a search bar and per-row type toggles
- **驯兽核心 (TamingCore)** — edit the Taming Core recipe pattern, display name, and consume-on-success flag

Each sub-screen opens via network request, receives its data from the server, and sends changes back over the network for immediate persistence to `basic.snbt`.

### Pet List Management

- Every living entity type registered in the game is returned from the server and displayed in a scrollable, searchable list.
- Each entry shows the translated display name, registry ID, optional nickname, and an allowCTF checkbox.
- Tamable and rideable creatures default to allowCTF=true; all other entities default to allowCTF=false.
- OP players can toggle checkboxes and save; non-OP players see existing enabled entries in read-only mode.
- Disabling a species triggers server-side cleanup: CTF persistent data and equipment are removed from every loaded entity of that type.

### Food Classification GUI

- A single scrollable list contains every food item known to the config, with per-row 3-state toggle buttons:
  - 普通 (normal) — white, contributes evolution points based on food nutrition
  - 特殊 (special) — gold, triggers custom rule effects configurable per player
  - 禁用 (disabled) — grey, the item is silently ignored when fed to pets
- Search bar filters by registry ID.
- The "更多\"食物\"" button opens the SpecialFoodScreen for configuring non-food special items (items that trigger special-rule behavior but are not edible).
- Data is persisted as three independent lists: `normal_food_item_ids`, `special_food_item_ids`, `disabled_food_item_ids`.

### Taming Core Sub-screen

- Reuses the existing TamingCoreScreen (previously standalone) as a config sub-screen accessible from the general config hub.
- Fields: recipe string, display name, consume-on-success toggle.

### Network Protocol (3 New Packets)

| Packet | Direction | idx | Description |
|--------|-----------|-----|-------------|
| C2SRequestConfigScreenPacket | C→S | 8 | Request config sub-screen data (0=PetList, 1=FoodList, 2=TamingCore, 3=SpecialFood) |
| S2CConfigScreenDataPacket | S→C | 9 | Server responds with full sub-screen CompoundTag data, parent SNBT, and OP status |
| C2SSaveConfigScreenPacket | C→S | 10 | Client saves modified sub-screen data back to the server |

### Dynamic Config Field Additions

The CTF_Settings/basic.snbt schema gained three new lists:
- `general_entity_ids` — entity types that default to allowCTF=false (all non-tameable, non-rideable creatures)
- `disabled_food_item_ids` — items the player has explicitly disabled as pet food
- `special_item_ids` — non-food items that can trigger special-rule behavior

### Server-side State Cleanup

When a species is disabled through the PetList GUI, the server:
1. Resolves the entity type from its registry ID
2. Iterates all loaded entities of that type in the current level
3. Drops any equipped module items
4. Removes CTF persistent data (PetProgress, PetEquipmentState)
5. Restores the original entity display name

### Config Reference Independence

CTF_Settings defaults no longer reference any mod-internal items. Previous versions included `custom_taming_framework:special_food` in the default special food list. v0.1.2-alpha generates purely vanilla defaults (golden_apple, enchanted_golden_apple, golden_carrot). The mod's test items (special_food, test_module) were removed entirely, making every config portable across installations.

### Autocomplete Z-Ordering Fix

The autocomplete suggestion popup in TamingCoreScreen and SpecialFoodScreen now renders on top of grid items and text. The previous `super.render()` after popup caused deferred item models in the shared GuiGraphics buffer to draw on top at GPU flush time. Fixed by isolating the popup as an independent GPU draw pass: `endBatch()` before and after the popup, with a higher Z-level via `pose().translate(0,0,500)`.

### Removed v0.1.x Prototype Items

| Removed Item | Registration | Resource Files | Effects |
|---|---|---|---|
| special_food | CtfItems | lang, model, texture | CtfFoods.java emptied |
| test_module | CtfItems | lang, model, texture | Handler removed from CtfForgeEvents |

### Version Sync

- The project version was aligned to `0.1.2-alpha`.
- The runtime and packaging flow were adjusted to keep development and release artifacts separate.

## Current scope

This version completes the configuration system lifecycle: the mod can now be fully configured from within the game through GUI sub-screens, with instant network persistence. The config is no longer tied to any mod-internal items or hard-coded entity lists — every registered living entity and food item is available in-game for discovery and classification.

Still not covered: skill system mechanics, equipment evolution gameplay, S2C broadcast of config changes to all online players, and the drag-and-drop graphical programming interface planned for the formal release.

---

# v0.1.2-alpha

## 这个测试版是做什么的

v0.1.2-alpha 将 CTF 配置系统从静态预设文件转变为交互式 GUI 驱动的体验。如果说 v0.1.1-alpha 引入了 SNBT 配置目录和标签页界面，那么这个版本则通过专用子界面实现了对模组每一项配置的可视化浏览、搜索和修改——无需手动编辑文件。

核心架构变化是 CTF_Settings 现在完全独立自给，不再依赖任何模组内置测试物品或硬编码默认值。配置系统可以枚举游戏中每个活体生物类型作为独立可切换的宠物候选，交互式地对食物进行分类，并通过网络同步的 GUI 持久化自定义物品效果。

## 自 v0.1.1-alpha 以来实现了什么

### 配置子界面架构

通用配置界面升级为导航中心，左侧面板包含三个导航按钮：
- **宠物列表**— 浏览、搜索、切换游戏中每种活体生物的 CTF 启用状态
- **食物列表**— 将每种食物分类为普通/特殊/禁用，带搜索栏和逐行类型切换按钮
- **驯兽核心**— 编辑驯兽核心的配方图案、显示名及其消耗行为

每个子界面通过网络请求打开，从服务端接收完整数据，修改后通过网络包即时持久化到 `basic.snbt`。

### 宠物列表管理

- 服务端返回游戏中已注册的每种活体生物，以可滚动的可搜索列表呈现。
- 每行显示翻译名、注册 ID、可选昵称和 allowCTF 复选框。
- 可驯服和可骑乘生物默认 allowCTF=true；其他生物默认 allowCTF=false。
- OP 玩家可切换复选框并保存；非 OP 玩家只读查看已启用的条目。
- 禁用某个物种会触发服务端清理：从该类型的所有已加载实体中移除 CTF 持久数据和装备。

### 食物分类 GUI

- 游戏已知的所有食物以单列表呈现，每行显示 3 种状态的切换按钮：
  - 普通 — 白色，基于食物营养值贡献进化点
  - 特殊 — 金色，触发玩家可配置的自定义规则效果
  - 禁用 — 灰色，喂宠物时该物品被静默忽略
- 搜索栏按注册 ID 过滤。
- "更多\"食物\""按钮打开 SpecialFoodScreen，配置非食物类的特殊物品。
- 数据持久化为三条独立列表：`normal_food_item_ids`、`special_food_item_ids`、`disabled_food_item_ids`。

### 驯兽核心子界面

- 复用原有的 TamingCoreScreen，通过通用配置中心导航访问。
- 字段：配方图案、显示名、消耗行为开关。

### 网络协议（新增 3 个包）

| 包名 | 方向 | idx | 说明 |
|------|------|-----|------|
| C2SRequestConfigScreenPacket | C→S | 8 | 请求配置子界面数据（0=宠物列表, 1=食物列表, 2=驯兽核心, 3=特殊物品） |
| S2CConfigScreenDataPacket | S→C | 9 | 服务端返回完整子界面 CompoundTag 数据、父级 SNBT 和 OP 状态 |
| C2SSaveConfigScreenPacket | C→S | 10 | 客户端保存修改后的子界面数据到服务端 |

### 动态配置字段扩展

CTF_Settings/basic.snbt 新增三种列表：
- `general_entity_ids` — 默认 allowCTF=false 的生物类型（所有不可驯服不可骑乘的生物）
- `disabled_food_item_ids` — 玩家手动禁用的宠物食物
- `special_item_ids` — 可触发特殊规则行为的非食物类物品

### 服务端状态清理

通过宠物列表 GUI 禁用某个物种时，服务端执行以下操作：
1. 从注册 ID 解析实体类型
2. 遍历当前维度中该类型的所有已加载实体
3. 丢弃已装备的测试模块物品
4. 移除 CTF 持久数据（PetProgress、PetEquipmentState）
5. 恢复实体原始显示名

### 配置引用独立性

CTF_Settings 默认值不再引用任何模组内置物品。旧版本在默认特殊食物列表中包含 `custom_taming_framework:special_food`。v0.1.2-alpha 只生成纯原版默认值（金苹果、附魔金苹果、金胡萝卜）。模组的测试物品（special_food、test_module）被完全移除，使每份配置在不同安装环境下均可移植。

### 自动补全弹出列表层级修复

TamingCoreScreen 和 SpecialFoodScreen 中的自动补全建议列表现在完全覆盖底层网格物品和文字。原因为共享 GuiGraphics 缓冲区的延迟绘制导致物品模型在弹窗之后刷新到 GPU。修复方式为将弹窗隔离为独立 GPU 绘制批次：弹窗前 `endBatch()` 刷新所有延迟绘制 → `pose().translate(0,0,500)` 抬高 Z 层级 → 弹窗渲染 → 弹窗后再次 `endBatch()`。

### 移除 v0.1.x 原型测试物品

| 移除项 | 注册 | 资源文件 | 影响 |
|--------|------|----------|------|
| special_food | CtfItems | lang、模型、纹理 | CtfFoods.java 清空 |
| test_module | CtfItems | lang、模型、纹理 | CtfForgeEvents 移除处理器 |

### 版本同步

- 项目版本已同步到 `0.1.2-alpha`。
- 开发运行和发布产物保持分离，避免不同映射环境混用。

## 当前范围

这个版本完成了配置系统的完整生命周期：模组现在可以通过游戏内 GUI 子界面进行全方位配置，修改即时网络持久化。配置不再与任何模组内置物品或硬编码生物列表绑定——每种已注册的生物和食物都在游戏中可发现、可分类。

尚未覆盖：技能系统机制、装备进化玩法、向所有在线玩家广播配置变化（S2C），以及正式版规划中的拖拽式图形化编程界面。


---

# v0.1.1-alpha

## What this test version is for

v0.1.1-alpha is the second playable test version of Custom Taming Framework. Where v0.1.0-alpha proved the core concept — enabling creatures, storing pet progress, feeding, and equipping — this version builds a proper graphical configuration system, adds visible pet identity features, and introduces permission separation between OP and non-OP players.

The focus is on making the framework usable day-to-day: a tabbed settings GUI, formatted pet names above entities, real-time nametag editing, and a full SNBT-based config directory that persists across restarts. This version also introduces the general configuration screen that was only sketched out in v0.1.0-alpha.

## What has been implemented since v0.1.0-alpha

### SNBT Configuration System
- Added CTF_Settings/ directory with three config layers: basic.snbt, GeneralGUI.snbt, and per-entity PetSettings/*.snbt files.
- Added auto-generation of default config files on first launch, with automatic repair of missing entries on subsequent loads.
- Added recursive multi-line pretty-printer for SNBT output covering CompoundTag, ListTag, StringTag, and all numeric tag types.
- Changed SNBT format from the earlier non-persistent defaults to proper file-based persistence with per-key tab representation.
- Added fallback conversion from the old `{tabs: [...]}` array format to the new individual `{tab_id: {data}}` format.

### TabContainer Shared Component
- Added TabContainer class supporting create, read, update, and delete of tab pages.
- Up to 3 visible tab buttons with pagination arrows when there are more tabs than fit.
- Pre-switch callback to save pending edits before tab changes.
- Order-based sort field for manual tab rearrangement.

### Pet Configuration Screen (PetScreen)
- Complete rewrite: left-right 1:2 split layout with a grey vertical divider.
- Title format: `CTF 宠物界面-<aqua nickname>（translated name）` with CTF in gold bold italic.
- Left panel contains the nametag editor ("命名") and the title/nickname ("头衔") label and optional edit box.
- Right panel contains the tab container and tab body editing area with title, summary, and slots fields.
- Bottom buttons (import, save config, close) arranged in a single centered row with equal spacing.
- All tab editing widgets are confined to the right side of the divider.

### General Configuration Screen (GeneralConfigScreen)
- Added proper general configuration screen (previously existing but minimal).
- Title format: `CTF 通用配置界面` with CTF in gold bold italic.
- Same 1:2 left-right split as PetScreen: empty left panel, tab container and edit boxes on the right.
- Default tabs: "pet_evolution" and "equipment_evolution" with optional slot lists.
- Edit boxes initially placed in the center are now fully contained in the right panel.

### Pet Entity Display Name
- Added updateEntityDisplayName() that sets the entity custom name to `§o§b[nickname]§r [baseName]`.
- Nickname shown in italic aqua above the pet; updates dynamically when the pet setting is saved.
- Empty nickname falls back to a generated placeholder based on the entity type ID.

### Nametag Interception
- Added a second EntityInteract event handler (EventPriority.HIGH) that intercepts right-clicks with name tags.
- The name from the tag is saved as CtfBaseName in the entity's persistent data.
- The display name is rebuilt as `§o§b[nickname]§r [nameFromNametag]`.
- In-GUI nametag editor mirrors this behavior with real-time C2S packet sync.

### In-GUI Nametag Editor
- Added a text field below "命名:" always accessible to all players regardless of OP status.
- Every keystroke sends a C2SSetEntityBaseNamePacket to the server for instant persistence.
- The field is pre-populated with the current CtfBaseName from the server when the pet screen opens.
- The feature works independently of the SNBT pet configuration file.

### Permission Control (OP vs Non-OP)
- OP players: all edit boxes editable, all action buttons visible (save, import, add/delete tabs, etc.).
- Non-OP players: nickname/title displayed as colored read-only text; no edit boxes for config fields; save/import/add-tab buttons hidden.
- Nametag editor remains always editable.
- "普通玩家：仅阅览" hint messages removed entirely from both screens.
- "ID:" lines hidden for non-OP players in both pet and general tab bodies; body content shifts up.

### Network Packets
- Added C2SSetEntityBaseNamePacket (idx=7): real-time nametag sync from client to server.
- Added C2SImportGeneralToPetPacket (idx=6): import general config into a pet.
- Extended S2COpenPetScreenPacket with the baseName field so the nametag editor always shows the correct value.
- Extended C2SSavePetSettingPacket with entityNetworkId to update the entity display name after save.

### Other Changes
- Pet nickname/title label changed from "昵称" to "头衔", displayed in aqua for all players.
- Entity overhead pet name colour changed from gold (§e) to aqua (§b) for visual consistency.
- Tab bar moved to the top of both screens, spanning the full GUI width above the divider.
- Left-right panel ratio adjusted from 1:3 to 1:2 on both screens.
- Increased vertical spacing between nametag editor and title edit field to prevent overlap.
- Game start automation: CTF_Settings/ and saves/ directories are cleaned before each launch to ensure a fresh test environment.
- PCL (PCL2 launcher) is not affected by the process cleanup.
- Version bumped from 0.1.0-alpha to 0.1.1-alpha.

## Current scope

This version transforms the framework from a proof-of-concept into a practical daily-use tool. A fully functional SNBT configuration system, tabbed GUI, named entity display, nametag integration, and OP/non-OP permission separation are all operational. The GUIs are structured to support future skill and equipment evolution features as additional tab pages.

Still not covered: skill system mechanics, equipment evolution gameplay, and S2C broadcast of configuration changes to all online players. The broadcast architecture is prepared but not activated.

---

# v0.1.1-alpha

## 这个测试版是做什么的

v0.1.0-alpha 验证了核心思路：启用生物、存储宠物进度、喂食、装备。v0.1.1-alpha 在此基础上构建了完整的图形化配置系统，增加了可见的宠物身份标识功能，并引入了 OP 与非 OP 玩家的权限分离。

重点是把框架变成日常可用的工具：分标签页的设置界面、宠物头顶的格式化名称、实时命名牌编辑，以及全套 SNBT 配置目录系统。这个版本还正式添加了 v0.1.0-alpha 中只有草稿的通用配置界面。

## 自 v0.1.0-alpha 以来实现了什么

### SNBT 配置系统
- 新增 CTF_Settings/ 配置目录，包含三层结构：basic.snbt、GeneralGUI.snbt 和按生物的 PetSettings/*.snbt。
- 首次启动自动生成默认配置文件；后续启动自动补齐缺失条目。
- 新增递归多行缩进打印器，覆盖 CompoundTag、ListTag、StringTag 和所有数值标签类型的 SNBT 输出。
- 从早期的非持久化默认值改为基于文件的持久化配置，采用独立的 `{tab_id:{data}}` 段落格式。
- 支持从旧 `{tabs:[...]}` 数组格式自动转换为新格式。

### TabContainer 共享组件
- 新增 TabContainer 类，支持标签页的增、删、改、查。
- 最多同时显示 3 个标签按钮，超出时显示翻页箭头。
- 切换标签前回调，用于保存未完成的编辑。
- 基于 order 字段的排序支持。

### 宠物配置界面（PetScreen）
- 完全重写：左右 1:2 分栏布局，灰色垂直分隔线。
- 标题格式：`CTF 宠物界面-<天蓝色昵称>（翻译名）`，CTF 为金色花体粗体。
- 左侧面板：命名牌编辑框（"命名"）+ 头衔标签及编辑框（"头衔"）。
- 右侧面板：标签页容器及标签体编辑区（标题、摘要、部位）。
- 底部按钮（导入、保存配置、关闭）等间距水平排列。
- 所有标签编辑组件限制在分隔线右侧。

### 通用配置界面（GeneralConfigScreen）
- 正式添加通用配置界面（之前版本只有原型）。
- 标题格式：`CTF 通用配置界面`，CTF 为金色花体粗体。
- 与 PetScreen 相同的 1:2 左右分栏：左空、右标签。
- 默认标签页："pet_evolution" 和 "equipment_evolution"，可选部位列表。
- 所有编辑框从旧居中位置移至右侧面板。

### 宠物实体显示名
- 新增 updateEntityDisplayName()，将实体自定义名设为 `§o§b[昵称]§r [基名]`。
- 昵称以斜体天蓝色显示在宠物头顶，保存设置后动态更新。
- 空白昵称回退为基于生物类型 ID 生成的占位符。

### 命名牌拦截
- 新增第二个 EntityInteract 事件处理器（EventPriority.HIGH），拦截带命名牌的右键。
- 命名牌的名称存入实体持久数据的 CtfBaseName。
- 显示名重建为 `§o§b[昵称]§r [命名牌名称]`。
- GUI 内命名牌编辑框通过实时 C2S 数据包实现相同行为。

### GUI 内命名牌编辑框
- 在"命名:"标签下方新增文本编辑框，所有玩家（无论 OP 与否）均可编辑。
- 每次按键发送 C2SSetEntityBaseNamePacket 到服务器，实现即时持久化。
- 打开宠物界面时自动填入服务器端当前的 CtfBaseName。
- 不依赖 SNBT 宠物配置文件，独立运作。

### 权限控制（OP vs 非 OP）
- OP 玩家：所有编辑框可编辑，所有操作按钮可见（保存、导入、增删标签等）。
- 非 OP 玩家：头衔以彩色只读文本显示；配置字段无编辑框；保存/导入/添加标签按钮隐藏。
- 命名牌编辑框始终可编辑。
- "普通玩家：仅阅览"提示信息从两侧界面中完全移除。
- 非 OP 玩家在界面的标签体中不显示 "ID:" 行，内容自动上移。

### 网络包
- 新增 C2SSetEntityBaseNamePacket（idx=7）：客户端到服务器的实时命名同步。
- 新增 C2SImportGeneralToPetPacket（idx=6）：导入通用配置到宠物。
- S2COpenPetScreenPacket 扩展了 baseName 字段，确保命名牌编辑框始终显示正确值。
- C2SSavePetSettingPacket 扩展了 entityNetworkId 字段，保存后更新实体显示名。

### 其他改动
- 宠物昵称标签从"昵称"改为"头衔"，对所有玩家显示为天蓝色。
- 宠物头顶名称颜色从金色（§e）改为天蓝色（§b）以统一视觉。
- 标签栏移至两侧界面顶部，跨分栏线全宽显示。
- 两侧界面左右比例从 1:3 调整为 1:2。
- 增大命名牌编辑框与头衔编辑框之间的垂直间距，防止重叠。
- 启动流程自动化：每次启动前清理 CTF_Settings/ 和 saves/ 目录，确保纯净测试环境。
- PCL2 启动器不受进程清理影响。
- 版本号从 0.1.0-alpha 升至 0.1.1-alpha。

## 当前范围

这个版本将框架从概念验证转变为实用的日常工具。完整的 SNBT 配置系统、分标签页 GUI、实体命名显示、命名牌集成以及 OP/非 OP 权限分离均已就绪。GUI 结构已为后续添加技能和装备进化功能（作为额外标签页）做好了准备。

尚未覆盖：技能系统机制、装备进化玩法、向所有在线玩家广播配置变化（S2C 广播架构已就绪但未启用）。


---

# v0.1.0-alpha

## What this test version is for

v0.1.0-alpha is the first playable test version of Custom Taming Framework. Its purpose is to prove the core idea of the mod: suitable creatures can be turned into custom pets, keep their own progression data, receive simple upgrades, and react to configurable pet-related items.

This version is not a complete pet framework yet. It is an early foundation for testing the activation flow, pet progression data, basic food and special-item behavior, equipment-style enhancement slots, and the separation between normal pets and custom-enabled pets.

## What has been implemented

- Added the Taming Core item as the entry point for enabling the custom pet system on supported creatures.
- Added rules for two pet categories:
  - creatures that must already be tamed or owned before they can be enabled;
  - creatures and mounts that can be enabled directly.
- Added support for vanilla pets and mounts such as wolves, cats, parrots, horses, donkeys, mules, llamas, skeleton horses, zombie horses, pigs, striders, and camels.
- Added persistent pet progress data, including whether the creature has been enabled, who activated it, evolution points, and unlocked skill markers.
- Added normal pet food behavior based on the food value of edible items.
- Added special food entries for the built-in example special food, golden apples, enchanted golden apples, and golden carrots.
- Added a basic pet screen showing pet progress, unlocked skill markers, and equipment-slot summary information.
- Added five logical equipment slots for future pet enhancement routes: head, body, fore limb, hind limb, and tail.
- Added a test enhancement module that can be installed into the head slot of an enabled pet.
- Added server-side validation for pet activation and pet screen access.
- Added configuration entries for pet categories, food categories, special food entries, and Taming Core behavior.
- Added machine-readable self-test logging for the main alpha systems.

## Current scope

This alpha focuses on proving that the main systems can work together. It provides a small but functional loop: enable a supported creature, store its custom pet state, interact with it through food or test items, and inspect its current custom progress.

The version is meant for testing behavior and gathering feedback before expanding the framework into a more complete customization system.

---

# v0.1.0-alpha

## 这个测试版是做什么的

v0.1.0-alpha 是 Custom Taming Framework 的第一个可游玩测试版本。它的目的，是验证这个 Mod 的核心想法：合适的生物可以被启用为自定义宠物，保存自己的成长数据，获得简单强化，并响应可配置的宠物相关物品。

这个版本还不是完整的宠物框架。它是一个早期基础版本，用来测试启用流程、宠物成长数据、普通食物与特殊物品行为、装备式强化槽位，以及普通宠物和已启用自定义宠物之间的区别。

## 已经实现了什么

- 添加了“驯兽核心”物品，作为为支持的生物开启自定义宠物系统的入口。
- 添加了两类宠物规则：
  - 必须先被驯服或拥有，才能启用的生物；
  - 可以直接启用的生物和坐骑。
- 添加了对原版宠物和坐骑的支持，例如狼、猫、鹦鹉、马、驴、骡、羊驼、骷髅马、僵尸马、猪、炽足兽和骆驼。
- 添加了宠物进度持久化数据，包括是否已启用、由谁启用、进化点数和已解锁技能标记。
- 添加了基于食物数值的普通宠物食物行为。
- 添加了特殊食物入口，包括内置示例特殊食物、金苹果、附魔金苹果和金胡萝卜。
- 添加了基础宠物界面，用于查看宠物进度、已解锁技能标记和装备槽摘要信息。
- 添加了五个逻辑装备槽，供后续宠物强化路线使用：头部、躯干、前肢、后肢和尾部。
- 添加了一个测试强化模块，可以安装到已启用宠物的头部槽。
- 添加了服务端校验，用于验证宠物启用和宠物界面访问。
- 添加了宠物分类、食物分类、特殊食物入口和驯兽核心行为相关配置。
- 添加了机器可读的自测日志，用于验证主要 alpha 系统。

## 当前范围

这个 alpha 版本重点验证主要系统能否串联工作。它提供了一个小型但可运行的闭环：启用支持的生物，保存它的自定义宠物状态，通过食物或测试物品与它互动，并查看它当前的自定义成长进度。

这个版本主要用于测试行为和收集反馈，之后再把框架扩展为更完整的自定义系统。

## License

本项目采用 MIT License，详见 [LICENSE](./LICENSE)。

本仓库的 Gradle Wrapper 保留其随附的 Apache-2.0 许可，详见 [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md)。
