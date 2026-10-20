package dev.pew2018.pixelcloneprofilehelper;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.animation.PathInterpolator;
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
    private TextView recentReport;
    private boolean binding;
    private boolean pageShown;
    private final List<ScrollView> pageScrolls = new ArrayList<>();
    private String rawDetails;
    private String rawEvents = "";
    private List<String> formattedRows;
    private String deviceSnapshot;
    private ObjectAnimator refreshAnimator;
    private View refreshAction;
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
        if (state != null) {
            for (int i = 0; i < pageScrolls.size(); i++) {
                ScrollView scroll = pageScrolls.get(i);
                int y = state.getInt("scroll_" + i, 0);
                scroll.post(() -> scroll.scrollTo(0, y));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle state) {
        for (int i = 0; i < pageScrolls.size(); i++) state.putInt("scroll_" + i, pageScrolls.get(i).getScrollY());
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
        if (refreshAnimator != null) refreshAnimator.cancel();
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
        toolbar.setContentInsetsRelative(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_space_4));
        toolbar.setContentInsetStartWithNavigation(dimension(R.dimen.pcph_subpage_title_start));
        toolbar.setTitleMarginStart(0);
        toolbar.setContentInsetEndWithActions(0);
        toolbar.setBackgroundColor(getColor(R.color.pcph_toolbar));
        toolbar.setElevation(dimension(R.dimen.pcph_toolbar_elevation));
        toolbar.setPopupTheme(R.style.ThemeOverlay_CloneHelper_Popup);
        toolbar.setNavigationOnClickListener(v -> navigateUp());
        toolbar.setOnMenuItemClickListener(this::onToolbarAction);
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, dimension(R.dimen.pcph_toolbar_height)));

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
        entry.setOnCheckedChangeListener((button, checked) -> {
            if (!binding) save(HelperApplication.ENTRY, checked);
        });
        third.setOnCheckedChangeListener((button, checked) -> {
            if (!binding) save(HelperApplication.THIRD, checked);
        });
    }

    private LinearLayout newPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setId(0x1000 + pageScrolls.size());
        pageScrolls.add(scroll);
        scroll.setScrollbarFadingEnabled(true);
        scroll.setClipToPadding(false);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, 0, 0, dimension(R.dimen.pcph_page_bottom));
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
        config.setPadding(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_space_8), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_space_8));
        divider(functions, dimension(R.dimen.pcph_page_horizontal));
        settingRow(functions, "系统克隆设置", "管理克隆应用", v -> openClonePage());

        LinearLayout status = section(page, "运行状态");
        SettingRow summary = settingRow(status, "系统设置连接", "", v -> {
            statusExpanded = !statusExpanded;
            updateStatusDisclosure(true);
        });
        connectionSummary = summary.summary;
        statusArrow = summary.arrow;
        statusArrow.setImageResource(R.drawable.pcph_ic_expand);
        statusDetails = new LinearLayout(this);
        statusDetails.setOrientation(LinearLayout.VERTICAL);
        status.addView(statusDetails, matchWrap());
        installed = infoRow(statusDetails, "模块");
        framework = infoRow(statusDetails, "运行框架连接");
        scope = infoRow(statusDetails, "系统设置作用范围");
        hook = infoRow(statusDetails, "克隆功能检查");
        recentReport = infoRow(statusDetails, "最近回报");
        updateStatusDisclosure(false);

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
        caption.setText(getString(R.string.app_name) + " · v" + appVersionName());
        caption.setPadding(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_page_horizontal));
        deviceDetails = bodyText(page, true);
        deviceDetails.setPadding(dimension(R.dimen.pcph_page_horizontal), 0, dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_page_horizontal));
        deviceDetails.setTextIsSelectable(true);
    }

    private void showPage(int page) {
        int next = page >= HOME && page <= RAW ? page : HOME;
        boolean changing = pageShown && next != currentPage;
        int direction = next > currentPage ? 1 : -1;
        if (changing && animationsEnabled()) {
            pages.setInAnimation(pageAnimation(true, direction));
            pages.setOutAnimation(pageAnimation(false, direction));
        } else {
            pages.setInAnimation(null);
            pages.setOutAnimation(null);
        }
        currentPage = next;
        pages.setDisplayedChild(currentPage);
        pageShown = true;
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
            MenuItem refreshItem = menu.findItem(REFRESH);
            androidx.appcompat.widget.AppCompatImageButton refreshButton =
                    new androidx.appcompat.widget.AppCompatImageButton(this, null, androidx.appcompat.R.attr.toolbarNavigationButtonStyle);
            refreshButton.setImageResource(R.drawable.pcph_ic_refresh);
            refreshButton.setContentDescription(getString(R.string.refresh_status));
            refreshButton.setPadding(dimension(R.dimen.pcph_space_12), dimension(R.dimen.pcph_space_12), dimension(R.dimen.pcph_space_12), dimension(R.dimen.pcph_space_12));
            refreshButton.setLayoutParams(new android.view.ViewGroup.LayoutParams(dimension(R.dimen.pcph_touch_target), dimension(R.dimen.pcph_touch_target)));
            refreshButton.setOnClickListener(v -> refreshWithFeedback());
            refreshItem.setActionView(refreshButton);
            refreshAction = refreshButton;
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
            case REFRESH: refreshWithFeedback(); return true;
            case EXPORT: exportLogs(); return true;
            case CLEAR: clearLogs(); return true;
            case COPY: copyDiagnostics(); return true;
            case ABOUT:
                androidx.appcompat.app.AlertDialog about = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_CloneHelper_Dialog)
                        .setBackgroundInsetStart(0).setBackgroundInsetEnd(0)
                        .setTitle("应用信息")
                        .setMessage(getString(R.string.app_name) + "\nv" + appVersionName())
                        .setPositiveButton("关闭", null).show();
                android.graphics.Rect available = new android.graphics.Rect();
                root.getWindowVisibleDisplayFrame(available);
                if (about.getWindow() != null) {
                    about.getWindow().setLayout(Math.min(available.width() - dimension(R.dimen.pcph_touch_target),
                            dimension(R.dimen.pcph_dialog_max_width)), -2);
                    about.getWindow().setDimAmount(.56f);
                }
                return true;
            default: return false;
        }
    }

    private boolean animationsEnabled() {
        return ValueAnimator.areAnimatorsEnabled()
                && Settings.Global.getFloat(getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) > 0f;
    }

    private int motionDuration() { return getResources().getInteger(R.integer.pcph_motion_duration); }

    private android.view.animation.Animation pageAnimation(boolean entering, int direction) {
        AnimationSet animation = new AnimationSet(true);
        animation.addAnimation(new AlphaAnimation(entering ? 0f : 1f, entering ? 1f : 0f));
        int distance = dimension(R.dimen.pcph_page_motion_distance) * direction;
        animation.addAnimation(new TranslateAnimation(entering ? distance : 0, entering ? 0 : -distance, 0, 0));
        animation.setDuration(Math.round(motionDuration() * Settings.Global.getFloat(
                getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)));
        animation.setInterpolator(new PathInterpolator(.4f, 0f, .2f, 1f));
        return animation;
    }

    private void refreshWithFeedback() {
        if (refreshAnimator != null && refreshAnimator.isRunning()) return;
        if (refreshAction != null && animationsEnabled()) {
            refreshAnimator = ObjectAnimator.ofFloat(refreshAction, View.ROTATION, 0f, 360f);
            refreshAnimator.setDuration(getResources().getInteger(R.integer.pcph_refresh_duration));
            refreshAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            refreshAnimator.start();
        }
        refresh();
        showFeedback(getString(R.string.status_refreshed), Snackbar.LENGTH_SHORT);
    }

    private void updateStatusDisclosure(boolean animate) {
        if (statusDetails == null) return;
        boolean motion = animate && animationsEnabled() && ViewCompat.isLaidOut(statusDetails.getRootView());
        // ChangeBounds animates the containing block and following rows together; Fade only targets details.
        TransitionManager.endTransitions(pageContents.get(HOME));
        if (motion) {
            TransitionSet transition = new TransitionSet().setOrdering(TransitionSet.ORDERING_TOGETHER);
            transition.addTransition(new ChangeBounds());
            transition.addTransition(new Fade().addTarget(statusDetails));
            transition.setDuration(motionDuration());
            transition.setInterpolator(new PathInterpolator(.4f, 0f, .2f, 1f));
            TransitionManager.beginDelayedTransition(pageContents.get(HOME), transition);
        }
        statusDetails.setVisibility(statusExpanded ? View.VISIBLE : View.GONE);
        statusArrow.animate().cancel();
        float rotation = statusExpanded ? 180f : 0f;
        if (motion) statusArrow.animate().rotation(rotation).setDuration(motionDuration())
                .setInterpolator(new PathInterpolator(.4f, 0f, .2f, 1f)).start();
        else statusArrow.setRotation(rotation);
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
                page.setPadding(safe.left, 0, safe.right, dimension(R.dimen.pcph_page_bottom) + safe.bottom);
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
        heading.setMinHeight(dimension(R.dimen.pcph_section_height));
        heading.setPadding(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_section_top), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_section_bottom));
        ViewCompat.setAccessibilityHeading(heading, true);
        box.addView(heading, matchWrap());
        return box;
    }

    private void divider(LinearLayout parent) { divider(parent, 0); }

    private void divider(LinearLayout parent, int inset) {
        View line = new View(this);
        line.setBackgroundColor(getColor(R.color.pcph_divider));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dimension(R.dimen.pcph_divider_height));
        params.setMarginStart(inset);
        parent.addView(line, params);
    }

    private final class SettingRow extends LinearLayout {
        final TextView title, summary;
        final ImageView arrow;
        SettingRow(String label, String detail, View.OnClickListener listener) {
            super(MainActivity.this);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setMinimumHeight(dimension(R.dimen.pcph_info_row_height));
            setPadding(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_row_vertical_padding), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_row_vertical_padding));
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
            detailParams.topMargin = dimension(R.dimen.pcph_summary_spacing);
            text.addView(summary, detailParams);
            addView(text, new LinearLayout.LayoutParams(0, -2, 1f));
            arrow = new ImageView(MainActivity.this);
            arrow.setImageResource(R.drawable.pcph_ic_chevron);
            arrow.setImageTintList(ColorStateList.valueOf(getColor(R.color.pcph_text_secondary)));
            arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dimension(R.dimen.pcph_icon_size), dimension(R.dimen.pcph_icon_size));
            arrowParams.setMarginStart(dimension(R.dimen.pcph_page_horizontal));
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
        SettingRow row = settingRow(parent, label, "", null);
        if (parent == statusDetails) {
            row.setPaddingRelative(dimension(R.dimen.pcph_detail_indent), dimension(R.dimen.pcph_space_8), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_space_8));
            row.setMinimumHeight(dimension(R.dimen.pcph_detail_row_height));
            row.title.setTextAppearance(R.style.TextAppearance_CloneHelper_DetailTitle);
        }
        return row.summary;
    }

    private TextView bodyText(LinearLayout parent, boolean diagnostic) {
        TextView text = new TextView(this);
        text.setTextAppearance(diagnostic ? R.style.TextAppearance_CloneHelper_Diagnostic
                : R.style.TextAppearance_CloneHelper_Body);
        text.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
        if (diagnostic) text.setLineSpacing(dimension(R.dimen.pcph_space_4), 1f);
        parent.addView(text, matchWrap());
        return text;
    }

    private SwitchMaterial switchRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dimension(R.dimen.pcph_switch_row_height));
        row.setPadding(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_space_4), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_space_4));
        row.setBackground(selectableItemBackground());
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextAppearance(R.style.TextAppearance_CloneHelper_Primary);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        SwitchMaterial control = new SwitchMaterial(this);
        control.setContentDescription(label);
        control.setMinHeight(dimension(R.dimen.pcph_touch_target));
        control.setMinWidth(dimension(R.dimen.pcph_touch_target));
        title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        control.setThumbTintList(switchThumbColors());
        control.setTrackTintList(switchTrackColors());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMarginStart(dimension(R.dimen.pcph_space_12));
        row.addView(control, params);
        row.setClickable(true);
        row.setFocusable(false);
        row.setOnClickListener(v -> { if (control.isEnabled()) control.toggle(); });
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
        if (!rows.equals(formattedRows)) {
            rawEvents = formatRawLogs(rows);
            formattedRows = new ArrayList<>(rows);
        }
        hook.setText(hookSummary(rows));
        boolean delivered = HelperApplication.service != null && HelperApplication.sync();
        config.setText(getString(delivered ? R.string.config_wait_settings : R.string.config_wait_connection));
        binding = true;
        android.content.SharedPreferences preferences = AppConfig.prefs();
        entry.setChecked(preferences.getBoolean(HelperApplication.ENTRY, true));
        third.setChecked(preferences.getBoolean(HelperApplication.THIRD, true));
        binding = false;

        String lastReport = rows.isEmpty() ? "尚未收到回报" : logFields(rows.get(rows.size() - 1))[0];
        // Reports are historical evidence; they do not prove a live connection or configuration parity.
        connectionSummary.setText(rows.isEmpty() ? "尚未收到系统设置进程回报"
                : "已收到系统回报");
        recentReport.setText(rows.isEmpty() ? lastReport : UiPresentation.reportTime(lastReport));
        String latest = rows.isEmpty() ? null : rows.get(rows.size() - 1);
        logsSummary.setText(latest == null ? "暂无运行日志" : "最近事件：" + eventSummary(latest));
        deviceSummary.setText(Build.MODEL + " · Android " + Build.VERSION.RELEASE);
        device.setText(Build.MANUFACTURER + " " + Build.MODEL);
        androidVersion.setText(Build.VERSION.RELEASE + " · SDK " + Build.VERSION.SDK_INT);
        deviceBuild.setText(Build.DISPLAY);
        frameworkVersion.setText(HelperApplication.framework == null || HelperApplication.framework.isEmpty()
                ? "尚未确认" : UiPresentation.framework(HelperApplication.framework));
        deviceReport.setText(rows.isEmpty() ? lastReport : UiPresentation.reportTime(lastReport));
        diagnosticHook.setText(hookDiagnosticStatus(rows));
        String details = "Build fingerprint\n" + Build.FINGERPRINT
                + "\n\nBuild ID\n" + Build.ID
                + "\n\n最近回报时间\n" + lastReport
                + "\n\n运行框架\n" + HelperApplication.framework
                + "\n\n系统设置作用范围\n" + (HelperApplication.scoped ? "com.android.settings：已启用"
                    : HelperApplication.service == null ? "尚未确认" : "com.android.settings：未启用")
                + "\n\nHook 状态\n" + hookDiagnosticStatus(rows)
                + "\n\n原始事件记录\n" + (rows.isEmpty() ? "暂无运行日志" : rawEvents);
        if (!details.equals(rawDetails)) {
            rawDetails = details;
            deviceDetails.setText(styleRawDetails(details));
        }
        deviceSnapshot = getString(R.string.app_name) + " v" + appVersionName()
                + "\n设备型号：" + device.getText() + "\nAndroid 版本：" + androidVersion.getText()
                + "\n构建版本：" + deviceBuild.getText() + "\n运行框架：" + frameworkVersion.getText()
                + "\n系统设置回报：" + deviceReport.getText() + "\nHook 状态：" + diagnosticHook.getText();
        renderLogs(rows);
    }

    private String eventSummary(String row) {
        String[] fields = logFields(row);
        return fields.length >= 3 ? logSummary(fields[2]) : "诊断记录";
    }

    private void renderLogs(List<String> rows) {
        if (rows.equals(renderedLogs)) return;
        renderedLogs = new ArrayList<>(rows);
        ScrollView scroll = pageScrolls.get(LOGS);
        int scrollY = scroll.getScrollY();
        logEntries.removeAllViews();
        scroll.post(() -> scroll.scrollTo(0, scrollY));
        if (rows.isEmpty()) {
            TextView empty = bodyText(logEntries, false);
            empty.setText("暂无运行日志");
            empty.setPadding(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_icon_size), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_icon_size));
            return;
        }
        String lastDate = "";
        for (int i = rows.size() - 1; i >= 0; i--) {
            String raw = rows.get(i);
            String timestamp = logFields(raw)[0];
            String date = UiPresentation.logDate(timestamp);
            if (!date.equals(lastDate)) {
                TextView heading = bodyText(logEntries, false);
                heading.setTextAppearance(R.style.TextAppearance_CloneHelper_Caption);
                heading.setText(date);
                heading.setMinHeight(dimension(R.dimen.pcph_log_date_height));
                heading.setGravity(Gravity.CENTER_VERTICAL);
                heading.setPadding(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_space_8), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_space_4));
                ViewCompat.setAccessibilityHeading(heading, true);
                lastDate = date;
            }
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.TOP);
            line.setBaselineAligned(true);
            line.setMinimumHeight(dimension(R.dimen.pcph_switch_row_height));
            line.setPadding(dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_log_row_padding), dimension(R.dimen.pcph_page_horizontal), dimension(R.dimen.pcph_log_row_padding));
            TextView time = new TextView(this);
            time.setTextAppearance(R.style.TextAppearance_CloneHelper_Time);
            time.setText(UiPresentation.logTime(timestamp));
            int timeWidth = Math.max(dimension(R.dimen.pcph_log_text_offset), (int) Math.ceil(time.getPaint().measureText(time.getText().toString())) + dimension(R.dimen.pcph_page_horizontal));
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
            divider(logEntries, dimension(R.dimen.pcph_page_horizontal) + timeWidth);
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
        String status = "尚未收到系统设置进程回报";
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

    private CharSequence styleRawDetails(String text) {
        SpannableStringBuilder styled = new SpannableStringBuilder(text);
        String[] labels = {"Build fingerprint", "Build ID", "最近回报时间", "运行框架", "系统设置作用范围", "Hook 状态", "原始事件记录"};
        int start = 0;
        for (String label : labels) {
            int position = text.indexOf(label + "\n", start);
            if (position >= 0) {
                styled.setSpan(new TextAppearanceSpan(this, R.style.TextAppearance_CloneHelper_RawLabel),
                        position, position + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = position + label.length();
            }
        }
        return styled;
    }

    private void save(String key, boolean value) {
        AppConfig.prefs().edit().putBoolean(key, value).apply();
        boolean sent = HelperApplication.sync();
        config.setText(getString(sent ? R.string.config_wait_settings : R.string.config_wait_connection));
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
                            ? getString(R.string.app_name) + " v" + appVersionName() + "\n原始运行日志\n" + formatRawLogs(logs())
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
        return getString(R.string.app_name) + " v" + appVersionName() + "\n"
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
        clipboard.setPrimaryClip(ClipData.newPlainText("diagnostics", currentPage == DEVICE ? deviceSnapshot : diagnostics()));
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

