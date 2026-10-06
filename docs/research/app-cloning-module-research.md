# PixelXpert 应用克隆功能研究记录

> 状态：研究记录，不是实施方案或已完成的移植  
> 日期：2026-10-07  
> 研究仓库：[`Pew2018/PixelXpert`](https://github.com/Pew2018/PixelXpert)  
> 研究基线：`canary`，提交 `354cb462fc527d7e3204047b62aa31b616c5ac21`  
> 研究分支：`research/app-cloning-module`

## 1. 研究目标

研究 PixelXpert 中为 Pixel Settings 解锁“克隆应用”功能的实现，判断如何将其整理为独立模块。当前工作仅限代码研究与文档归档，不在本分支实现克隆功能、不改动 `canary` 或正式分支。

## 2. 结论摘要

PixelXpert 没有自行实现应用虚拟化，也没有复制 APK。Android 平台已有 Clone Profile（克隆用户档案）机制：Settings 创建一个克隆档案，并将已有应用安装到该档案。PixelXpert 主要通过 Xposed Hook 修改系统 Settings，让克隆入口可见、扩展可克隆应用列表，并显示删除克隆档案的菜单。

因此，独立版的核心应是一个针对 `com.android.settings` 的精简 LSPosed/Xposed 模块。KernelSU 可以承担模块分发和安装包装，但单靠启动脚本不足以替代对 Settings 内部逻辑的运行时 Hook。实际创建档案和安装应用仍由 Android 系统后端执行。

## 3. PixelXpert 中的代码证据

核心实现位于：

- [`AppCloneEnabler.java`](https://github.com/Pew2018/PixelXpert/blob/canary/app/src/main/java/sh/siava/pixelxpert/xposed/modpacks/settings/AppCloneEnabler.java)
- [Xposed 默认作用域](https://github.com/Pew2018/PixelXpert/blob/canary/app/src/main/resources/META-INF/xposed/scope.list)
- [模块构建配置](https://github.com/Pew2018/PixelXpert/blob/canary/app/build.gradle.kts)

当前 `AppCloneEnabler` 的行为：

1. Hook `ClonedAppsPreferenceController.getAvailabilityStatus()`，返回 `AVAILABLE`，使 Settings 的克隆应用入口通过可用性检查。
2. Hook `ClonedAppsPreferenceController.updateSummary()`，替换入口摘要。
3. Hook `AppStateClonedAppsBridge` 构造完成后的状态，把 `mAllowedApps` 替换成当前已安装包列表。
4. 读取克隆档案中的已安装包；对已经自动进入克隆档案的系统应用，不在候选列表中重复显示。代码仍会把“尚未自动克隆的系统应用”加入候选列表。
5. Hook `ManageApplications.updateOptionsMenu()`；当当前列表是克隆应用列表且克隆用户存在时，显示 `delete_all_app_clones` 菜单项。
6. 通过 Settings 内部 `Utils.getCloneUserId()` 判断克隆档案是否存在。

源码中有一段针对 `android.os.Flags.allowPrivateProfile()` 的 Private Space Hook，但它被注释掉了。不能据此把 Private Space 与 Clone Profile 视为同一功能，也不能认为当前克隆功能依赖这段 Hook。

默认 LSPosed 作用域文件包含 `com.android.settings`。这与核心 Hook 目标吻合。

## 4. Android 后端如何工作

AOSP Settings 的 `CloneBackend` 负责实际生命周期：

1. 若克隆档案不存在，调用 `UserManager.createProfile(..., USER_TYPE_PROFILE_CLONE, ...)` 创建档案。
2. 首次创建后启动该档案。
3. 调用包管理服务的 `installExistingPackageAsUser`，把现有应用安装到克隆档案。
4. 删除全部克隆应用时，Settings 移除克隆用户档案；这会删除其中的应用数据。

Android 对 Clone Profile 的定义是用于运行同一应用的第二实例；系统限制每个父用户至多一个克隆档案，且只能由主用户创建。它本质上是 Android 多用户/工作档案设施，不是容器引擎。

## 5. 独立模块的合理边界

建议后续独立模块只负责 Settings 暴露层：

- 针对目标 Android 版本检测 Settings 内部类是否存在。
- 显示克隆应用设置入口。
- 扩展可克隆应用列表，并明确系统应用的筛选策略。
- 保留删除克隆档案的途径，并在界面提示这会清除档案内数据。
- 对 Hook 缺失或失败进行安全降级，不影响 Settings 正常启动。

不应把系统的档案创建、应用安装逻辑重新实现一遍；优先调用原生 Settings 后端。快速切换个人/工作/克隆档案的 QS 功能属于相邻功能，可另立研究项，不是克隆应用闭环的必要组成。

## 6. 兼容性与风险

### 已确认

- 当前 `canary` 源码仍包含 `AppCloneEnabler`。
- 该 Hook 针对 Settings 的内部类、字段和资源 ID，包含反射访问和硬编码列表类型常量 `17`。
- 当前 PixelXpert app 的 `minSdk` 与 `compileSdk` 均为 36；这描述的是当前整项目构建配置，不代表最小独立 Hook 必须限制到 API 36。
- Android 平台保留 Clone Profile 后端不等于每个 Pixel 固件都允许其创建；必须在目标固件验证端到端行为。

### 待验证

- Pixel 8 Pro（`husky`）当前 Android 16 构建中，Settings 目标类、方法、字段和克隆菜单资源是否保持兼容。
- Hook 启用后是否能创建档案、安装应用、启动克隆应用、卸载单个克隆应用，以及删除整个克隆档案。
- 是否需按 Android 14/15/16 或 QPR 分拆 Hook 适配。
- 候选列表中系统应用的筛选范围；“可见于列表”不代表所有系统应用都适合克隆。
- 无效目标版本或系统更新后 Hook 失败时，Settings 是否保持正常启动。

### 主要工程风险

最脆弱的部分是 Settings 私有实现细节，而不是 Clone Profile 概念本身。Android 更新后，类名、字段名、方法签名、菜单 ID 或内部列表常量变化，都可能让 Hook 失效。实现时应逐项探测目标类和成员、记录 Hook 成功状态，并在找不到目标时跳过功能，而不是让 Settings 崩溃。

克隆档案删除操作具有破坏性，会清除其中所有应用数据。独立模块应保留明确告知用户的删除确认界面。

## 7. 研究阶段建议的验收清单

在开始独立模块开发前，先在目标设备上逐项确认：

- [ ] Android 16 当前构建中，Xposed 能作用于 `com.android.settings`。
- [ ] 克隆应用入口出现，且 Settings 主页面其他功能正常。
- [ ] 第三方应用出现在列表中。
- [ ] 克隆应用成功安装，且原应用与克隆应用的数据相互独立。
- [ ] 重启后档案和克隆应用仍存在。
- [ ] 单独移除克隆应用不会卸载主用户中的原应用。
- [ ] 删除克隆档案会清理档案数据，并且随后可重新创建。
- [ ] 禁用 Hook 或遇到未匹配的系统类时，Settings 不崩溃。

## 8. 许可与来源

PixelXpert 仓库使用 GPL-3.0。若直接移植或修改其代码并分发，应保留相应版权和许可证声明，并遵守 GPL-3.0 对衍生作品分发的要求。见 [仓库 LICENSE](https://github.com/Pew2018/PixelXpert/blob/canary/LICENSE)。

主要技术来源：

- [PixelXpert `AppCloneEnabler.java`](https://github.com/Pew2018/PixelXpert/blob/canary/app/src/main/java/sh/siava/pixelxpert/xposed/modpacks/settings/AppCloneEnabler.java)
- [PixelXpert LSPosed scope](https://github.com/Pew2018/PixelXpert/blob/canary/app/src/main/resources/META-INF/xposed/scope.list)
- [AOSP Settings `AppStateClonedAppsBridge`](https://android.googlesource.com/platform/packages/apps/Settings/+/main/src/com/android/settings/applications/AppStateClonedAppsBridge.java)
- [AOSP Settings `CloneBackend`](https://android.googlesource.com/platform/packages/apps/Settings/+/main/src/com/android/settings/applications/manageapplications/CloneBackend.java)
- [AOSP `UserManager.USER_TYPE_PROFILE_CLONE`](https://android.googlesource.com/platform/frameworks/base/+/61f01fe56bd8464acf3141212371a9176f3d6c9b/core/java/android/os/UserManager.java)
