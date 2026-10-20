package dev.pew2018.pixelcloneprofilehelper;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MainActivity extends AppCompatActivity {
    private static final int HOME = 0, LOGS = 1, DEVICE = 2, RAW = 3;
    private static final int REFRESH = 1, EXPORT = 2, CLEAR = 3, COPY = 4, ABOUT = 5;
    private static volatile MainActivity visible;
    private LinearLayout root;
    private View statusBarSpacer;
    private Toolbar toolbar;
    private ViewFlipper pages;
    private final List<LinearLayout> pageContents = new ArrayList<>();
    private OnBackPressedCallback pageBack;
    private int currentPage;
    private boolean statusExpanded;
    private LinearLayout statusDetails;
    private LinearLayout logEntries;
    private ImageView statusArrow;
    private TextView installed, framework, scope, hook, config;
    private TextView device, deviceBuild, deviceReport, deviceDetails;
    private TextView androidVersion, frameworkVersion, diagnosticHook;
    private TextView connectionSummary, logsSummary, deviceSummary;
    private SwitchMaterial entry, third;
    private List<String> renderedLogs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppConfig.init(this);
        if (state != null) {
            currentPage = state.getInt("page", HOME);
            statusExpanded = state.getBoolean("statusExpanded", false);
        }
        visible = this;
        build();
        refresh();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putInt("page", currentPage);
        state.putBoolean("statusExpanded", statusExpanded);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = this;
        refresh();
    }

    @Override
    protected void onDestroy() {
        if (visible == this) visible = null;
        super.onDestroy();
    }

    static void refreshVisible() {
        MainActivity activity = visible;
        if (activity != null) activity.runOnUiThread(activity::refresh);
    }

    private void build() {
        configureEdgeToEdge();
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.pcph_background));
        statusBarSpacer = new View(this);
        statusBarSpacer.setBackgroundColor(getColor(R.color.pcph_status_bar));
        root.addView(statusBarSpacer, new LinearLayout.LayoutParams(-1, 0));

        toolbar = new Toolbar(this);
        toolbar.setTitleTextColor(getColor(R.color.pcph_text_on_dark));
        toolbar.setTitleTextAppearance(this, R.style.TextAppearance_CloneHelper_Toolbar);
        toolbar.setContentInsetsRelative(dp(16), dp(16));
        toolbar.setBackgroundColor(getColor(R.color.pcph_toolbar));
        toolbar.setElevation(dimension(R.dimen.pcph_toolbar_elevation));
        toolbar.setPopupTheme(R.style.ThemeOverlay_CloneHelper_Popup);
        toolbar.setNavigationOnClickListener(v -> navigateUp());
        toolbar.setOnMenuItemClickListener(this::onToolbarAction);
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(56)));

        pages = new ViewFlipper(this);
        root.addView(pages, new LinearLayout.LayoutParams(-1, 0, 1f));
        buildHome(newPage());
        buildLogs(newPage());
        buildDevice(newPage());
        buildRaw(newPage());
        pageBack = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { navigateUp(); }
        };
        getOnBackPressedDispatcher().addCallback(this, pageBack);
        setContentView(root);
        applyWindowInsets();
        showPage(currentPage);

        entry.setChecked(AppConfig.prefs().getBoolean(HelperApplication.ENTRY, true));
        third.setChecked(AppConfig.prefs().getBoolean(HelperApplication.THIRD, true));
        entry.setOnCheckedChangeListener((button, checked) -> save(HelperApplication.ENTRY, checked));
        third.setOnCheckedChangeListener((button, checked) -> save(HelperApplication.THIRD, checked));
    }

    private LinearLayout newPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, 0, 0, dp(24));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        pages.addView(scroll, new ViewFlipper.LayoutParams(-1, -1));
        pageContents.add(page);
        return page;
    }

    private void buildHome(LinearLayout page) {
        LinearLayout functions = section(page, "克隆功能");
        entry = switchRow(functions, "启用系统克隆入口");
        third = switchRow(functions, "允许克隆第三方应用");
        config = bodyText(functions, false);
        config.setTextAppearance(R.style.TextAppearance_CloneHelper_Secondary);
        config.setPadding(dp(16), dp(8), dp(16), dp(12));
        settingRow(functions, "系统克隆设置", "管理克隆应用", v -> openClonePage());

        LinearLayout status = section(page, "运行状态");
        SettingRow summary = settingRow(status, "系统设置连接", "", v -> {
            statusExpanded = !statusExpanded;
            updateStatusDisclosure();
        });
        connectionSummary = summary.summary;
        statusArrow = summary.arrow;
        statusDetails = new LinearLayout(this);
        statusDetails.setOrientation(LinearLayout.VERTICAL);
        status.addView(statusDetails, matchWrap());
        installed = infoRow(statusDetails, "模块");
        framework = infoRow(statusDetails, "运行框架连接");
        scope = infoRow(statusDetails, "系统设置作用范围");
        hook = infoRow(statusDetails, "克隆功能检查");
        updateStatusDisclosure();

        LinearLayout diagnostics = section(page, "诊断");
        logsSummary = settingRow(diagnostics, "运行日志", "暂无运行日志", v -> showPage(LOGS)).summary;
        deviceSummary = settingRow(diagnostics, "设备与诊断", "", v -> showPage(DEVICE)).summary;
    }

    private void buildLogs(LinearLayout page) {
        logEntries = new LinearLayout(this);
        logEntries.setOrientation(LinearLayout.VERTICAL);
        page.addView(logEntries, matchWrap());
    }

    private void buildDevice(LinearLayout page) {
        LinearLayout hardware = section(page, "设备信息");
        device = infoRow(hardware, "设备型号");
        androidVersion = infoRow(hardware, "Android 版本");
        deviceBuild = infoRow(hardware, "构建版本");
        LinearLayout runtime = section(page, "模块与框架");
        frameworkVersion = infoRow(runtime, "运行框架");
        deviceReport = infoRow(runtime, "系统设置回报");
        diagnosticHook = infoRow(runtime, "Hook 状态");
        LinearLayout diagnostics = section(page, "诊断");
        settingRow(diagnostics, "原始诊断信息", "构建指纹、事件记录与框架详情", v -> showPage(RAW));
    }

    private void buildRaw(LinearLayout page) {
        TextView caption = bodyText(page, false);
        caption.setText("Pixel Clone Profile Helper · v" + appVersionName());
        caption.setPadding(dp(16), dp(24), dp(16), dp(16));
        deviceDetails = bodyText(page, true);
        deviceDetails.setPadding(dp(16), 0, dp(16), dp(16));
        deviceDetails.setTextIsSelectable(true);
    }

    private void showPage(int page) {
        currentPage = page >= HOME && page <= RAW ? page : HOME;
        pages.setDisplayedChild(currentPage);
        toolbar.setTitle(currentPage == HOME ? getString(R.string.helper_title)
                : currentPage == LOGS ? "运行日志" : currentPage == DEVICE ? "设备与诊断" : "原始诊断信息");
        toolbar.setNavigationIcon(currentPage == HOME ? null : getDrawable(R.drawable.pcph_ic_back));
        toolbar.setNavigationContentDescription(currentPage == HOME ? null : "返回");
        pageBack.setEnabled(currentPage != HOME);
        Menu menu = toolbar.getMenu();
        menu.clear();
        if (currentPage == HOME) {
            menu.add(Menu.NONE, REFRESH, Menu.NONE, "刷新状态")
                    .setIcon(R.drawable.pcph_ic_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(Menu.NONE, ABOUT, Menu.NONE, "应用信息");
        } else if (currentPage == LOGS) {
            menu.add(Menu.NONE, EXPORT, Menu.NONE, "导出日志");
            menu.add(Menu.NONE, CLEAR, Menu.NONE, "清除日志");
        } else {
            menu.add(Menu.NONE, COPY, Menu.NONE, "复制诊断信息")
                    .setIcon(R.drawable.pcph_ic_copy).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add(Menu.NONE, EXPORT, Menu.NONE, "导出完整诊断");
        }
        if (toolbar.getOverflowIcon() != null) toolbar.getOverflowIcon().setTint(Color.WHITE);
        toolbar.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    private void navigateUp() { showPage(currentPage == RAW ? DEVICE : HOME); }

    private boolean onToolbarAction(MenuItem item) {
        switch (item.getItemId()) {
            case REFRESH: refresh(); showFeedback("状态已刷新", Snackbar.LENGTH_SHORT); return true;
            case EXPORT: exportLogs(); return true;
            case CLEAR: clearLogs(); return true;
            case COPY: copyDiagnostics(); return true;
            case ABOUT:
                new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CloneHelper_Dialog)
                        .setTitle("应用信息")
                        .setMessage("Pixel Clone Profile Helper\nv" + appVersionName())
                        .setPositiveButton("关闭", null).show();
                return true;
            default: return false;
        }
    }

    private void updateStatusDisclosure() {
        if (statusDetails == null) return;
        statusDetails.setVisibility(statusExpanded ? View.VISIBLE : View.GONE);
        statusArrow.setRotation(statusExpanded ? 270f : 90f);
        ViewCompat.setStateDescription((View) statusArrow.getParent(), statusExpanded ? "已展开" : "已收起");
    }

    private void configureEdgeToEdge() {
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            LinearLayout.LayoutParams spacer = (LinearLayout.LayoutParams) statusBarSpacer.getLayoutParams();
            if (spacer.height != safe.top) {
                spacer.height = safe.top;
                statusBarSpacer.setLayoutParams(spacer);
            }
            toolbar.setPadding(safe.left, 0, safe.right, 0);
            for (LinearLayout page : pageContents) {
                page.setPadding(safe.left, 0, safe.right, dp(24) + safe.bottom);
            }
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
            controller.setAppearanceLightStatusBars(false);
            int mode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            controller.setAppearanceLightNavigationBars(mode != android.content.res.Configuration.UI_MODE_NIGHT_YES);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private LinearLayout section(LinearLayout parent, String title) {
        if (parent.getChildCount() > 0) divider(parent);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        parent.addView(box, matchWrap());
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextAppearance(R.style.TextAppearance_CloneHelper_SectionTitle);
        heading.setGravity(Gravity.BOTTOM);
        heading.setMinHeight(dp(48));
        heading.setPadding(dp(16), dp(20), dp(16), dp(12));
        ViewCompat.setAccessibilityHeading(heading, true);
        box.addView(heading, matchWrap());
        return box;
    }

    private void divider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(getColor(R.color.pcph_divider));
        parent.addView(line, new LinearLayout.LayoutParams(-1, dimension(R.dimen.pcph_divider_height)));
    }

    private final class SettingRow extends LinearLayout {
        final TextView title, summary;
        final ImageView arrow;
        SettingRow(String label, String detail, View.OnClickListener listener) {
            super(MainActivity.this);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setMinimumHeight(dp(72));
            setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout text = new LinearLayout(MainActivity.this);
            text.setOrientation(VERTICAL);
            title = new TextView(MainActivity.this);
            title.setTextAppearance(R.style.TextAppearance_CloneHelper_Primary);
            title.setText(label);
            text.addView(title, matchWrap());
            summary = new TextView(MainActivity.this);
            summary.setTextAppearance(R.style.TextAppearance_CloneHelper_Secondary);
            summary.setText(detail);
            LinearLayout.LayoutParams detailParams = matchWrap();
            detailParams.topMargin = dp(3);
            text.addView(summary, detailParams);
            addView(text, new LinearLayout.LayoutParams(0, -2, 1f));
            arrow = new ImageView(MainActivity.this);
            arrow.setImageResource(R.drawable.pcph_ic_chevron);
            arrow.setImageTintList(ColorStateList.valueOf(getColor(R.color.pcph_text_secondary)));
            arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(24), dp(24));
            arrowParams.setMarginStart(dp(16));
            addView(arrow, arrowParams);
            if (listener != null) {
                setBackground(selectableItemBackground());
                setClickable(true);
                setFocusable(true);
                setOnClickListener(listener);
            } else {
                arrow.setVisibility(GONE);
            }
        }
    }

    private SettingRow settingRow(LinearLayout parent, String title, String detail, View.OnClickListener listener) {
        SettingRow row = new SettingRow(title, detail, listener);
        parent.addView(row, matchWrap());
        return row;
    }

    private TextView infoRow(LinearLayout parent, String label) {
        if (parent.getChildCount() > 0
                && parent.getChildAt(parent.getChildCount() - 1) instanceof SettingRow) divider(parent);
        return settingRow(parent, label, "", null).summary;
    }

    private TextView bodyText(LinearLayout parent, boolean diagnostic) {
        TextView text = new TextView(this);
        text.setTextAppearance(diagnostic ? R.style.TextAppearance_CloneHelper_Diagnostic
                : R.style.TextAppearance_CloneHelper_Body);
        text.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
        if (diagnostic) text.setLineSpacing(dp(2), 1f);
        parent.addView(text, matchWrap());
        return text;
    }

    private SwitchMaterial switchRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(56));
        row.setPadding(dp(16), dp(4), dp(16), dp(4));
        row.setBackground(selectableItemBackground());
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextAppearance(R.style.TextAppearance_CloneHelper_Primary);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        SwitchMaterial control = new SwitchMaterial(this);
        control.setContentDescription(label);
        control.setThumbTintList(switchThumbColors());
        control.setTrackTintList(switchTrackColors());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMarginStart(dp(12));
        row.addView(control, params);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> control.toggle());
        parent.addView(row, matchWrap());
        return control;
    }

    private ColorStateList switchThumbColors() {
        return new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {android.R.attr.state_checked}, {}},
                new int[]{getColor(R.color.pcph_switch_disabled_thumb), getColor(R.color.pcph_accent),
                        getColor(R.color.pcph_switch_off_thumb)});
    }

    private ColorStateList switchTrackColors() {
        return new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {android.R.attr.state_checked}, {}},
                new int[]{getColor(R.color.pcph_switch_disabled_track), getColor(R.color.pcph_switch_on_track),
                        getColor(R.color.pcph_switch_off_track)});
    }

    private android.graphics.drawable.Drawable selectableItemBackground() {
        TypedValue value = new TypedValue();
        return getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true)
                ? getDrawable(value.resourceId) : null;
    }

    private void refresh() {
        if (framework == null) return;
        installed.setText("已安装 · v" + appVersionName());
        framework.setText(HelperApplication.service == null ? "等待连接" : "已连接");
        scope.setText(HelperApplication.service == null ? "尚未确认" : HelperApplication.scoped ? "已启用" : "未启用");
        List<String> rows = logs();
        hook.setText(hookSummary(rows));
        boolean delivered = HelperApplication.service != null && HelperApplication.sync();
        config.setText(delivered ? "已保存，等待系统设置生效" : "已保存，等待系统连接");
        entry.setChecked(AppConfig.prefs().getBoolean(HelperApplication.ENTRY, true));
        third.setChecked(AppConfig.prefs().getBoolean(HelperApplication.THIRD, true));

        String lastReport = rows.isEmpty() ? "尚未收到回报" : logFields(rows.get(rows.size() - 1))[0];
        // Reports are historical evidence; they do not prove a live connection or configuration parity.
        connectionSummary.setText(rows.isEmpty() ? "尚未收到系统设置进程回报"
                : "最近回报：" + lastReport + "\n" + hookSummary(rows));
        String latest = rows.isEmpty() ? null : rows.get(rows.size() - 1);
        logsSummary.setText(latest == null ? "暂无运行日志" : "最近事件：" + eventSummary(latest));
        deviceSummary.setText(Build.MODEL + " · Android " + Build.VERSION.RELEASE);
        device.setText(Build.MANUFACTURER + " " + Build.MODEL);
        androidVersion.setText(Build.VERSION.RELEASE + " · SDK " + Build.VERSION.SDK_INT);
        deviceBuild.setText(Build.DISPLAY);
        frameworkVersion.setText(HelperApplication.framework == null || HelperApplication.framework.isEmpty()
                ? "尚未确认" : HelperApplication.framework);
        deviceReport.setText(rows.isEmpty() ? lastReport : "最近回报：" + lastReport);
        diagnosticHook.setText(hookDiagnosticStatus(rows));
        deviceDetails.setText("Build fingerprint\n" + Build.FINGERPRINT
                + "\n\nBuild ID\n" + Build.ID
                + "\n\n最近回报时间\n" + lastReport
                + "\n\n运行框架\n" + HelperApplication.framework
                + "\n\n系统设置作用范围\n" + (HelperApplication.scoped ? "com.android.settings：已启用"
                    : HelperApplication.service == null ? "尚未确认" : "com.android.settings：未启用")
                + "\n\nHook 状态\n" + hookDiagnosticStatus(rows)
                + "\n\n原始事件记录\n" + (rows.isEmpty() ? "暂无运行日志" : formatRawLogs(rows)));
        renderLogs(rows);
    }

    private String eventSummary(String row) {
        String[] fields = logFields(row);
        return fields.length >= 3 ? logSummary(fields[2]) : "诊断记录";
    }

    private void renderLogs(List<String> rows) {
        if (rows.equals(renderedLogs)) return;
        renderedLogs = new ArrayList<>(rows);
        logEntries.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = bodyText(logEntries, false);
            empty.setText("暂无运行日志");
            empty.setPadding(dp(16), dp(24), dp(16), dp(24));
            return;
        }
        String lastDate = "";
        for (int i = rows.size() - 1; i >= 0; i--) {
            String raw = rows.get(i);
            String timestamp = logFields(raw)[0];
            String date = timestamp.length() >= 10 ? timestamp.substring(0, 10) : "时间未知";
            if (!date.equals(lastDate)) {
                TextView heading = bodyText(logEntries, false);
                heading.setTextAppearance(R.style.TextAppearance_CloneHelper_Caption);
                heading.setText(date);
                heading.setPadding(dp(16), dp(24), dp(16), dp(8));
                ViewCompat.setAccessibilityHeading(heading, true);
                lastDate = date;
            }
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.TOP);
            line.setMinimumHeight(dp(56));
            line.setPadding(dp(16), dp(16), dp(16), dp(16));
            TextView time = new TextView(this);
            time.setTextAppearance(R.style.TextAppearance_CloneHelper_Time);
            time.setText(shortTime(timestamp));
            int timeWidth = Math.max(dp(64), (int) Math.ceil(time.getPaint().measureText("00:00")) + dp(16));
            line.addView(time, new LinearLayout.LayoutParams(timeWidth, -2));
            TextView message = new TextView(this);
            message.setTextAppearance(R.style.TextAppearance_CloneHelper_Primary);
            message.setText(eventSummary(raw));
            line.addView(message, new LinearLayout.LayoutParams(0, -2, 1f));
            line.setBackground(selectableItemBackground());
            line.setClickable(true);
            line.setFocusable(true);
            line.setOnClickListener(v -> showLogDetail(raw));
            logEntries.addView(line, matchWrap());
            divider(logEntries);
        }
    }

    private void showLogDetail(String row) {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CloneHelper_Dialog)
                .setTitle(eventSummary(row)).setMessage(formatRawLogs(Collections.singletonList(row)))
                .setPositiveButton("关闭", null).show();
        TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) {
            message.setTextAppearance(R.style.TextAppearance_CloneHelper_Diagnostic);
            message.setTextIsSelectable(true);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String appVersionName() {
        try {
            android.content.pm.PackageInfo info =
                    getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName == null ? "未知" : info.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "未知";
        }
    }

    private String hookDiagnosticStatus(List<String> rows) {
        String status = "尚未收到 系统设置进程回报";
        for (String row : rows) {
            String[] fields = logFields(row);
            if (fields.length < 3) continue;
            String event = fields[2];
            String detail = fields.length > 3 ? fields[3] : "";
            if ("SETTINGS_PROCESS_LOADED".equals(event)) {
                status = "系统设置进程已加载本模块，等待 Hook 匹配结果";
            } else if ("HOOK_MATCHED".equals(event)) {
                status = "已匹配：" + detail;
            } else if ("HOOK_INSTALL_RESULT".equals(event)) {
                status = "Hook 匹配检查完成：" + detail;
            } else if ("HOOK_FAILED".equals(event) || "HOOK_PARTIAL".equals(event)) {
                status = "Hook 发生匹配或运行问题；查看原始日志";
            }
        }
        return status;
    }

    private String hookSummary(List<String> rows) {
        String summary = "等待系统响应";
        for (String row : rows) {
            String[] fields = logFields(row);
            if (fields.length < 3) continue;
            String event = fields[2];
            String detail = fields.length > 3 ? fields[3] : "";
            if ("HOOK_INSTALL_RESULT".equals(event)) {
                if (detail.contains("matched targets=2/2")) {
                    summary = "克隆功能可用";
                } else if (detail.contains("matched targets=0/2")) {
                    summary = "克隆功能不可用";
                } else {
                    summary = "部分功能不可用";
                }
            } else if ("HOOK_FAILED".equals(event) || "HOOK_PARTIAL".equals(event)) {
                summary = "部分功能不可用";
            } else if ("SETTINGS_PROCESS_LOADED".equals(event) || "HOOK_MATCHED".equals(event)) {
                summary = "正在检查";
            }
        }
        return summary;
    }

    private String logSummary(String event) {
        switch (event) {
            case "REPORT_CHANNEL_CONNECTED":
                return "系统设置回报通道已连接";
            case "REPORT_CHANNEL_REJECTED":
                return "系统设置回报通道连接失败";
            case "SETTINGS_PROCESS_LOADED":
                return "系统设置已启动";
            case "HOOK_MATCHED":
                return "系统设置检查通过";
            case "HOOK_INSTALL_RESULT":
                return "系统设置检查已完成";
            case "HOOK_FAILED":
                return "克隆功能不可用";
            case "HOOK_PARTIAL":
                return "部分克隆功能不可用";
            case "CANDIDATE_LIST_UPDATED":
                return "克隆应用列表已更新";
            case "CONFIG_READ":
                return "克隆功能设置已读取";
            case "CLONE_PROFILE_QUERY_FAILED":
                return "读取克隆档案失败";
            default:
                return "诊断状态已更新";
        }
    }

    private String formatRawLogs(List<String> rows) {
        StringBuilder output = new StringBuilder();
        for (String row : rows) {
            String[] fields = logFields(row);
            if (output.length() > 0) output.append("\n\n");
            if (fields.length < 3) {
                output.append(row);
                continue;
            }
            output.append(fields[0]).append('\n').append(fields[1]).append('\n').append(fields[2]);
            if (fields.length > 3 && !fields[3].isEmpty()) {
                output.append('\n').append(fields[3]);
            }
        }
        return output.toString();
    }

    private String[] logFields(String row) {
        return row.split(" \\| ", 4);
    }

    private String shortTime(String timestamp) {
        return timestamp.length() >= 16 && timestamp.charAt(10) == ' '
                ? timestamp.substring(11, 16)
                : timestamp;
    }

    private void save(String key, boolean value) {
        AppConfig.prefs().edit().putBoolean(key, value).apply();
        boolean sent = HelperApplication.sync();
        config.setText(sent
                ? "已保存，等待系统设置生效"
                : "已保存，等待系统连接");
        showFeedback("配置已保存", Snackbar.LENGTH_LONG);
    }

    private void openClonePage() {
        Intent intent = new Intent("android.settings.MANAGE_CLONED_APPS_SETTINGS")
                .setPackage("com.android.settings");
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                startActivity(intent);
                return;
            } catch (RuntimeException ignored) {
                // Fall through to the documented Settings fallback.
            }
        }
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CloneHelper_Dialog)
                .setTitle("原生克隆页面不可用")
                .setMessage("系统未公开可解析的克隆页面，可改为打开应用设置。")
                .setNegativeButton("取消", null)
                .setPositiveButton("打开应用设置",
                        (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS)))
                .show();
    }

    private void exportLogs() {
        final boolean logsOnly = currentPage == LOGS;
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CloneHelper_Dialog)
                .setTitle(logsOnly ? "导出运行日志" : "导出完整诊断")
                .setMessage("分享前请检查内容。日志只包含时间、Hook 事件与设备构建信息，不包含应用列表或账户数据。")
                .setNegativeButton("取消", null)
                .setPositiveButton("继续", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, logsOnly
                            ? "Pixel Clone Profile Helper v" + appVersionName() + "\n原始运行日志\n" + formatRawLogs(logs())
                            : diagnostics());
                    startActivity(Intent.createChooser(intent, logsOnly ? "分享运行日志" : "分享诊断"));
                })
                .show();
    }

    private void clearLogs() {
        new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CloneHelper_Dialog)
                .setTitle("清除日志")
                .setMessage("清除所有本地诊断记录？")
                .setNegativeButton("取消", null)
                .setPositiveButton("清除", (dialog, which) -> {
                    getSharedPreferences("diagnostics", 0).edit().clear().apply();
                    refresh();
                })
                .show();
    }

    private List<String> logs() {
        List<String> result = new ArrayList<>(getSharedPreferences("diagnostics", 0)
                .getStringSet("logs", Collections.emptySet()));
        Collections.sort(result);
        return result;
    }

    private String diagnostics() {
        return "Pixel Clone Profile Helper v" + appVersionName() + "\n"
                + device.getText() + "\nAndroid 版本：" + androidVersion.getText() + "\n构建版本："
                + deviceBuild.getText() + "\n" + deviceDetails.getText()
                + "\n运行框架：" + HelperApplication.framework
                + "\n系统设置作用范围：" + scope.getText()
                + "\n克隆功能状态：" + hook.getText()
                + "\n配置状态：" + config.getText();
    }

    private void showFeedback(String message, int duration) {
        Snackbar feedback = Snackbar.make(root, message, duration);
        feedback.setBackgroundTint(getColor(R.color.pcph_snackbar_background));
        feedback.setTextColor(Color.WHITE);
        feedback.show();
    }

    private void copyDiagnostics() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("diagnostics", diagnostics()));
        showFeedback("诊断信息已复制", Snackbar.LENGTH_SHORT);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dimension(int resource) {
        return getResources().getDimensionPixelSize(resource);
    }
}

