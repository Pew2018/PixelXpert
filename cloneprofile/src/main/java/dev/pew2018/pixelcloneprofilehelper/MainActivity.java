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
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MainActivity extends AppCompatActivity {
    private static volatile MainActivity visible;

    private LinearLayout root;
    private View statusBarSpacer;
    private TextView installed;
    private TextView framework;
    private TextView scope;
    private TextView hook;
    private TextView config;
    private TextView logText;
    private TextView device;
    private TextView deviceBuild;
    private TextView deviceReport;
    private TextView deviceDetails;
    private MaterialButton expandLogsButton;
    private MaterialButton detailsButton;
    private SwitchMaterial entry;
    private SwitchMaterial third;
    private boolean expanded;
    private boolean detailsExpanded;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppConfig.init(this);
        visible = this;
        build();
        refresh();
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
        root.addView(statusBarSpacer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0));

        Toolbar bar = new Toolbar(this);
        bar.setTitle(R.string.app_name);
        bar.setTitleTextColor(getColor(R.color.pcph_text_on_dark));
        bar.setTitleTextAppearance(this, R.style.TextAppearance_CloneHelper_Toolbar);
        bar.setContentInsetsRelative(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_page_horizontal));
        bar.setBackgroundColor(getColor(R.color.pcph_toolbar));
        bar.setElevation(dimension(R.dimen.pcph_toolbar_elevation));
        root.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dimension(R.dimen.pcph_toolbar_height)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(getColor(R.color.pcph_background));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(
                dimension(R.dimen.pcph_page_horizontal),
                dimension(R.dimen.pcph_page_top),
                dimension(R.dimen.pcph_page_horizontal),
                dimension(R.dimen.pcph_page_bottom));
        scroll.addView(page, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout statusCard = card(page, "运行状态");
        installed = infoRow(statusCard, "模块 APK");
        framework = infoRow(statusCard, "运行框架");
        scope = infoRow(statusCard, "作用范围");
        hook = infoRow(statusCard, "系统设置");
        addActionRow(statusCard, button -> flatButton(button, "刷新状态", v -> refresh()));

        LinearLayout cloneCard = card(page, "克隆功能");
        entry = switchRow(cloneCard, "启用系统克隆入口");
        third = switchRow(cloneCard, "允许克隆第三方应用");
        addSectionCaption(cloneCard, "配置状态");
        config = bodyText(cloneCard, false);
        addActionRow(cloneCard, button -> raisedButton(button, "打开系统克隆应用", v -> openClonePage()));

        LinearLayout logsCard = card(page, "运行日志");
        logText = bodyText(logsCard, false);
        LinearLayout logActions = new LinearLayout(this);
        logActions.setOrientation(LinearLayout.HORIZONTAL);
        logActions.setGravity(Gravity.CENTER_VERTICAL);
        flatButton(logActions, "展开更多", v -> {
            expanded = !expanded;
            refresh();
        });
        expandLogsButton = (MaterialButton) logActions.getChildAt(0);
        flatButton(logActions, "导出诊断", v -> exportLogs());
        flatButton(logActions, "清除", v -> clearLogs());
        logsCard.addView(logActions, matchWrap());

        LinearLayout diagnosticsCard = card(page, "设备与诊断");
        device = bodyText(diagnosticsCard, false);
        device.setTextAppearance(R.style.TextAppearance_CloneHelper_Primary);
        deviceBuild = infoRow(diagnosticsCard, "构建");
        deviceReport = infoRow(diagnosticsCard, "Settings 回报");
        deviceDetails = bodyText(diagnosticsCard, true);
        deviceDetails.setTextIsSelectable(true);
        deviceDetails.setVisibility(View.GONE);
        LinearLayout diagnosticActions = new LinearLayout(this);
        diagnosticActions.setOrientation(LinearLayout.HORIZONTAL);
        diagnosticActions.setGravity(Gravity.CENTER_VERTICAL);
        flatButton(diagnosticActions, "查看详细信息", v -> {
            detailsExpanded = !detailsExpanded;
            deviceDetails.setVisibility(detailsExpanded ? View.VISIBLE : View.GONE);
            detailsButton.setText(detailsExpanded ? "收起" : "查看详细信息");
        });
        detailsButton = (MaterialButton) diagnosticActions.getChildAt(0);
        flatButton(diagnosticActions, "复制诊断信息", v -> copyDiagnostics());
        diagnosticsCard.addView(diagnosticActions, matchWrap());

        setContentView(root);
        applyWindowInsets(root, page);
        entry.setChecked(AppConfig.prefs().getBoolean(HelperApplication.ENTRY, true));
        third.setChecked(AppConfig.prefs().getBoolean(HelperApplication.THIRD, true));
        entry.setOnCheckedChangeListener((button, checked) -> save(HelperApplication.ENTRY, checked));
        third.setOnCheckedChangeListener((button, checked) -> save(HelperApplication.THIRD, checked));
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

    private void applyWindowInsets(View rootView, LinearLayout page) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            LinearLayout.LayoutParams spacerParams = (LinearLayout.LayoutParams) statusBarSpacer.getLayoutParams();
            if (spacerParams.height != safeInsets.top) {
                spacerParams.height = safeInsets.top;
                statusBarSpacer.setLayoutParams(spacerParams);
            }

            page.setPadding(
                    dimension(R.dimen.pcph_page_horizontal) + safeInsets.left,
                    dimension(R.dimen.pcph_page_top),
                    dimension(R.dimen.pcph_page_horizontal) + safeInsets.right,
                    dimension(R.dimen.pcph_page_bottom) + safeInsets.bottom);

            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), rootView);
            if (controller != null) {
                controller.setAppearanceLightStatusBars(false);
                controller.setAppearanceLightNavigationBars(!isNightMode());
            }
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    private boolean isNightMode() {
        int mode = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private LinearLayout card(LinearLayout parent, String title) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(R.color.pcph_surface));
        card.setRadius(dimension(R.dimen.pcph_card_radius));
        card.setCardElevation(dimension(R.dimen.pcph_card_elevation));
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(true);
        card.setClickable(false);
        card.setFocusable(false);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dimension(R.dimen.pcph_card_spacing);
        parent.addView(card, cardParams);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(
                dimension(R.dimen.pcph_card_padding),
                dimension(R.dimen.pcph_card_padding_vertical),
                dimension(R.dimen.pcph_card_padding),
                dimension(R.dimen.pcph_card_padding_vertical));
        card.addView(box, new MaterialCardView.LayoutParams(
                MaterialCardView.LayoutParams.MATCH_PARENT, MaterialCardView.LayoutParams.WRAP_CONTENT));

        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextAppearance(R.style.TextAppearance_CloneHelper_SectionTitle);
        box.addView(heading, matchWrap());
        LinearLayout.LayoutParams headingParams = (LinearLayout.LayoutParams) heading.getLayoutParams();
        headingParams.bottomMargin = dimension(R.dimen.pcph_section_spacing);
        heading.setLayoutParams(headingParams);
        return box;
    }

    private TextView infoRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dimension(R.dimen.pcph_row_vertical_padding), 0,
                dimension(R.dimen.pcph_row_vertical_padding));

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextAppearance(R.style.TextAppearance_CloneHelper_Primary);
        row.addView(title, matchWrap());

        TextView value = new TextView(this);
        value.setTextAppearance(R.style.TextAppearance_CloneHelper_Secondary);
        value.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
        row.addView(value, matchWrap());

        parent.addView(row, matchWrap());
        return value;
    }

    private void addSectionCaption(LinearLayout parent, String label) {
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextAppearance(R.style.TextAppearance_CloneHelper_Caption);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dimension(R.dimen.pcph_config_caption_top);
        parent.addView(caption, params);
    }

    private TextView bodyText(LinearLayout parent, boolean diagnostic) {
        TextView text = new TextView(this);
        text.setTextAppearance(diagnostic
                ? R.style.TextAppearance_CloneHelper_Diagnostic
                : R.style.TextAppearance_CloneHelper_Secondary);
        text.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
        if (diagnostic) {
            text.setLineSpacing(dimension(R.dimen.pcph_log_line_spacing), 1f);
        }
        parent.addView(text, matchWrap());
        return text;
    }

    private SwitchMaterial switchRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dimension(R.dimen.pcph_switch_row_height));
        row.setBackground(selectableItemBackground());

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextAppearance(R.style.TextAppearance_CloneHelper_Primary);
        row.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        SwitchMaterial control = new SwitchMaterial(this);
        control.setContentDescription(label);
        control.setThumbTintList(switchThumbColors());
        control.setTrackTintList(switchTrackColors());
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        switchParams.leftMargin = dimension(R.dimen.pcph_switch_label_spacing);
        row.addView(control, switchParams);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> control.toggle());
        parent.addView(row, matchWrap());
        return control;
    }

    private ColorStateList switchThumbColors() {
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{
                getColor(R.color.pcph_switch_disabled_thumb),
                getColor(R.color.pcph_accent),
                getColor(R.color.pcph_switch_off_thumb)
        };
        return new ColorStateList(states, colors);
    }

    private ColorStateList switchTrackColors() {
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{
                getColor(R.color.pcph_switch_disabled_track),
                getColor(R.color.pcph_switch_on_track),
                getColor(R.color.pcph_switch_off_track)
        };
        return new ColorStateList(states, colors);
    }

    private android.graphics.drawable.Drawable selectableItemBackground() {
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true)) {
            return getDrawable(value.resourceId);
        }
        return null;
    }

    private interface ButtonFactory {
        void create(LinearLayout parent);
    }

    private void addActionRow(LinearLayout parent, ButtonFactory factory) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = dimension(R.dimen.pcph_action_spacing);
        parent.addView(row, params);
        factory.create(row);
    }

    private void flatButton(LinearLayout parent, String label, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextAppearance(R.style.TextAppearance_CloneHelper_Button);
        button.setTextColor(flatButtonTextColors());
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        button.setRippleColor(ColorStateList.valueOf(getColor(R.color.pcph_ripple)));
        button.setCornerRadius(dimension(R.dimen.pcph_button_radius));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(dimension(R.dimen.pcph_touch_target));
        button.setMinimumHeight(dimension(R.dimen.pcph_touch_target));
        button.setMinWidth(0);
        button.setPadding(dimension(R.dimen.pcph_button_horizontal_padding), 0,
                dimension(R.dimen.pcph_button_horizontal_padding), 0);
        button.setElevation(0f);
        button.setStateListAnimator(null);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams buttonParams = wrapWrap();
        if (parent.getChildCount() > 0) {
            buttonParams.leftMargin = dimension(R.dimen.pcph_section_spacing);
        }
        parent.addView(button, buttonParams);
    }

    private ColorStateList flatButtonTextColors() {
        int[][] states = new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{}
        };
        int[] colors = new int[]{
                getColor(R.color.pcph_text_disabled),
                getColor(R.color.pcph_accent)
        };
        return new ColorStateList(states, colors);
    }

    private void raisedButton(LinearLayout parent, String label, View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextAppearance(R.style.TextAppearance_CloneHelper_Button);
        button.setTextColor(getColor(R.color.pcph_text_on_accent));
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.pcph_accent)));
        button.setRippleColor(ColorStateList.valueOf(getColor(R.color.pcph_ripple_on_accent)));
        button.setCornerRadius(dimension(R.dimen.pcph_button_radius));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(dimension(R.dimen.pcph_touch_target));
        button.setMinimumHeight(dimension(R.dimen.pcph_touch_target));
        button.setMinWidth(dimension(R.dimen.pcph_button_min_width));
        button.setElevation(dimension(R.dimen.pcph_button_elevation));
        button.setOnClickListener(listener);
        parent.addView(button, wrapWrap());
    }

    private void refresh() {
        if (framework == null) return;

        installed.setText("已安装 · v0.1");
        framework.setText(HelperApplication.service == null ? "等待连接" : "已连接");
        scope.setText(HelperApplication.service == null
                ? "尚未确认"
                : HelperApplication.scoped ? "已启用" : "未启用");

        List<String> rows = logs();
        String hookDiagnostic = hookDiagnosticStatus(rows);
        hook.setText(hookSummary(rows));

        boolean delivered = HelperApplication.service != null && HelperApplication.sync();
        config.setText(delivered
                ? "配置已保存，等待系统设置生效"
                : "配置已保存，等待系统连接");

        if (entry != null) {
            entry.setChecked(AppConfig.prefs().getBoolean(HelperApplication.ENTRY, true));
            third.setChecked(AppConfig.prefs().getBoolean(HelperApplication.THIRD, true));
        }

        if (expanded) {
            logText.setText(formatRawLogs(rows));
            logText.setTextAppearance(R.style.TextAppearance_CloneHelper_Diagnostic);
            logText.setTextIsSelectable(true);
        } else {
            logText.setText(recentLogSummaries(rows));
            logText.setTextAppearance(R.style.TextAppearance_CloneHelper_Secondary);
            logText.setTextIsSelectable(false);
        }
        expandLogsButton.setText(expanded ? "收起" : "展开更多");
        expandLogsButton.setEnabled(!rows.isEmpty());

        String lastReport = rows.isEmpty()
                ? "尚无回报"
                : rows.get(rows.size() - 1).split(" \\| ", 2)[0];
        device.setText("Android " + Build.VERSION.RELEASE + " · SDK " + Build.VERSION.SDK_INT
                + "\n" + Build.MANUFACTURER + " " + Build.MODEL);
        deviceBuild.setText(Build.DISPLAY);
        deviceReport.setText(rows.isEmpty() ? "尚无回报" : shortTime(lastReport));
        deviceDetails.setText("Build fingerprint\n" + Build.FINGERPRINT
                + "\n\nBuild ID\n" + Build.ID
                + "\n\n最近 Settings 回报\n" + lastReport
                + "\n\n运行框架\n" + HelperApplication.framework
                + "\n\nSettings 作用域\n" + (HelperApplication.scoped
                    ? "com.android.settings：已启用"
                    : HelperApplication.service == null
                        ? "尚未确认"
                        : "com.android.settings：未启用")
                + "\n\nHook 状态\n" + hookDiagnostic);
    }

    private String hookDiagnosticStatus(List<String> rows) {
        String status = "尚未收到 Settings 进程回报";
        for (String row : rows) {
            String[] fields = logFields(row);
            if (fields.length < 3) continue;
            String event = fields[2];
            String detail = fields.length > 3 ? fields[3] : "";
            if ("SETTINGS_PROCESS_LOADED".equals(event)) {
                status = "Settings 进程已加载本模块，等待 Hook 匹配结果";
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

    private String recentLogSummaries(List<String> rows) {
        List<String> summaries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = rows.size() - 1; i >= 0 && summaries.size() < 3; i--) {
            String[] fields = logFields(rows.get(i));
            if (fields.length < 3) continue;
            String summary = logSummary(fields[2]);
            if (summary == null || !seen.add(summary)) continue;
            summaries.add(shortTime(fields[0]) + "   " + summary);
        }
        Collections.reverse(summaries);
        if (summaries.isEmpty()) return "暂无诊断记录";
        StringBuilder output = new StringBuilder();
        for (String line : summaries) {
            if (output.length() > 0) output.append('\n');
            output.append(line);
        }
        return output.toString();
    }

    private String logSummary(String event) {
        switch (event) {
            case "REPORT_CHANNEL_CONNECTED":
                return "系统设置已连接";
            case "REPORT_CHANNEL_REJECTED":
                return "系统设置连接失败";
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
                ? "配置已保存，等待系统设置生效"
                : "配置已保存，等待系统连接");
        Snackbar.make(root, "配置已保存", Snackbar.LENGTH_LONG).show();
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
        new MaterialAlertDialogBuilder(this)
                .setTitle("原生克隆页面不可用")
                .setMessage("系统未公开可解析的克隆页面，可改为打开应用设置。")
                .setNegativeButton("取消", null)
                .setPositiveButton("打开应用设置",
                        (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS)))
                .show();
    }

    private void exportLogs() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("导出本地诊断")
                .setMessage("分享前请检查内容。日志只包含时间、Hook 事件与设备构建信息，不包含应用列表或账户数据。")
                .setNegativeButton("取消", null)
                .setPositiveButton("继续", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, diagnostics());
                    startActivity(Intent.createChooser(intent, "分享诊断"));
                })
                .show();
    }

    private void clearLogs() {
        new MaterialAlertDialogBuilder(this)
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
        return "Pixel Clone Profile Helper v0.1\n"
                + device.getText() + "\n构建："
                + deviceBuild.getText() + "\n" + deviceDetails.getText()
                + "\n运行框架：" + HelperApplication.framework
                + "\nSettings 作用域：" + scope.getText()
                + "\n克隆功能状态：" + hook.getText()
                + "\n配置状态：" + config.getText()
                + "\n原始运行日志\n" + formatRawLogs(logs());
    }

    private void copyDiagnostics() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("diagnostics", diagnostics()));
        Snackbar.make(root, "诊断信息已复制", Snackbar.LENGTH_SHORT).show();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dimension(int resource) {
        return getResources().getDimensionPixelSize(resource);
    }
}
