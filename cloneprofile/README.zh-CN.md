# Pixel Clone Profile Helper v0.1

独立的 Vector / libxposed API 102 LSPosed 模块 APK。模块只 Hook Android Settings 的克隆应用入口与候选列表；克隆档案创建、应用安装、卸载及档案删除仍由 Android 原生 Settings 执行。

## 功能与安装

- 开放系统原生克隆应用入口；保留系统白名单中仍已安装的应用，并扩展加入普通第三方应用。
- 默认不加入白名单外的系统应用（含更新系统应用）；避免在克隆档案已有的系统应用中重复显示。
- 安装 APK 后，在兼容 API 102 的 Vector/LSPosed 管理器中启用模块，作用域只选择 com.android.settings，再重启 Settings 进程。
- 本项目不包含 KernelSU/Magisk 刷入包。设备需另行安装兼容 Vector/LSPosed 与 Zygisk 环境。
- 关闭入口开关后 Hook 恢复系统可用性结果；关闭第三方开关后保留系统白名单行为。候选列表在原生页面重新打开时刷新。
- 克隆页面 Intent 不可用时，App 提供跳转 Android 应用设置的降级入口。

## 状态与已知限制

- 框架服务、Settings 作用域、Hook 匹配、配置送达分别显示。API 服务未连接或诊断回传不可用时显示未知。
- Hook 目标源自 PixelXpert canary 研究：ClonedAppsPreferenceController.getAvailabilityStatus()、AppStateClonedAppsBridge.mAllowedApps。Settings 私有实现会随 Android 版本变化。
- 尚未在 Pixel 8 Pro Android 16 实机验证。Actions 编译通过不代表设备兼容；不声明 Android 14/15 或其他 ROM 兼容。
- 克隆档案包列表查询失败时，跳过候选范围扩展并记录诊断。v0.1 不额外开放隐藏的删除全部档案菜单。
- 配置通过框架 Remote Preferences 下发；列表设置需重新打开克隆页，个别系统版本可能需重启 Settings 进程。

## 隐私、日志与许可

最多保留 100 条本地记录；只写时间、Settings Hook 事件、精简错误类型和构建信息，不记录应用列表、账户、电话号码、IMSI、ICCID、克隆数据或使用内容。导出前显示确认；支持清除。
Hook 源码依据研究文档独立重写，未复制 PixelXpert 代码。若后续直接移植 PixelXpert GPL-3.0 代码，须保留版权与许可证并遵守 GPL-3.0。

GitHub Actions 在 main 只运行该独立子项目的 lint、单元测试、Debug/Release APK 构建；不构建 PixelXpert APK，不生成刷入包或正式 Release。
