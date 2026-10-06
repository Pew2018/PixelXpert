package dev.pew2018.pixelcloneprofilehelper;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.text.DateFormat;
import java.util.*;
public final class MainActivity extends AppCompatActivity {
    private static volatile MainActivity visible;
    private LinearLayout root; private TextView installed,framework,scope,hook,config,logText,device;
    private SwitchMaterial entry,third; private boolean expanded;
    @Override protected void onCreate(Bundle b){super.onCreate(b);AppConfig.init(this);visible=this;build();refresh();}
    @Override protected void onResume(){super.onResume();visible=this;refresh();}
    @Override protected void onDestroy(){if(visible==this)visible=null;super.onDestroy();}
    static void refreshVisible(){MainActivity a=visible;if(a!=null)a.runOnUiThread(a::refresh);}
    private void build(){
        root=new LinearLayout(this);root.setOrientation(1);
        Toolbar bar=new Toolbar(this);bar.setTitle(R.string.app_name);bar.setTitleTextColor(getColor(R.color.pcph_on_primary));bar.setBackgroundColor(getColor(R.color.pcph_primary));root.addView(bar,new LinearLayout.LayoutParams(-1,dp(56)));
        ScrollView scroll=new ScrollView(this);LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(16),dp(12),dp(16),dp(20));scroll.addView(page);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout s=card(page,"运行状态");installed=line(s);framework=line(s);scope=line(s);hook=line(s);button(s,"刷新状态",v->refresh());
        LinearLayout c=card(page,"克隆功能");entry=new SwitchMaterial(this);entry.setText("开放系统克隆应用入口");entry.setMinHeight(dp(52));c.addView(entry);
        third=new SwitchMaterial(this);third.setText("允许克隆第三方应用");third.setMinHeight(dp(52));c.addView(third);
        config=line(c);button(c,"打开系统克隆应用",v->openClonePage());
        LinearLayout l=card(page,"运行日志");logText=line(l);button(l,"展开更多",v->{expanded=!expanded;refresh();});button(l,"复制 / 导出",v->exportLogs());button(l,"清除",v->clearLogs());
        LinearLayout d=card(page,"设备与诊断");device=line(d);button(d,"复制诊断信息",v->copyDiagnostics());
        setContentView(root);
        entry.setChecked(AppConfig.prefs().getBoolean(HelperApplication.ENTRY,true));third.setChecked(AppConfig.prefs().getBoolean(HelperApplication.THIRD,true));
        entry.setOnCheckedChangeListener((b,v)->save(HelperApplication.ENTRY,v));third.setOnCheckedChangeListener((b,v)->save(HelperApplication.THIRD,v));
    }
    private LinearLayout card(LinearLayout parent,String title){
        MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(8));card.setCardElevation(dp(2));card.setUseCompatPadding(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);parent.addView(card,lp);
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),dp(12),dp(16),dp(12));card.addView(box);
        TextView h=new TextView(this);h.setText(title);h.setTextSize(18);h.setTypeface(null,android.graphics.Typeface.BOLD);box.addView(h);return box;
    }
    private TextView line(LinearLayout parent){TextView t=new TextView(this);t.setTextSize(14);t.setPadding(0,dp(7),0,dp(7));parent.addView(t);return t;}
    private void button(LinearLayout p,String label,View.OnClickListener listener){MaterialButton b=new MaterialButton(this);b.setText(label);b.setOnClickListener(listener);p.addView(b,new LinearLayout.LayoutParams(-1,-2));}
    private void refresh(){
        if(framework==null)return;installed.setText("模块 APK：已安装（v0.1）");framework.setText("Vector / LSPosed："+HelperApplication.framework);
        scope.setText("Settings 作用域："+(HelperApplication.service==null?"未知：服务未连接":HelperApplication.scoped?"已包含 com.android.settings":"未包含 com.android.settings"));
        List<String> rows=logs();String hookState="未知：尚未收到 Settings 进程回报";for(String row:rows){
            if(row.contains("SETTINGS_PROCESS_LOADED"))hookState="Settings 进程已加载本模块，等待 Hook 匹配结果";
            if(row.contains("HOOK_INSTALL_RESULT"))hookState="Hook 匹配检查完成："+row.substring(row.lastIndexOf(" | ")+3);
            if(row.contains("HOOK_FAILED")||row.contains("HOOK_PARTIAL"))hookState="Hook 发生匹配或运行问题；查看下方日志";
        }hook.setText("Settings Hook："+hookState);
        config.setText(HelperApplication.service==null?"配置：本机已保存，尚未确认送达":HelperApplication.sync()?"配置已写入 Remote Preferences；需 Hook 读取":"配置下发失败：未知");
        if(entry!=null){entry.setChecked(AppConfig.prefs().getBoolean(HelperApplication.ENTRY,true));third.setChecked(AppConfig.prefs().getBoolean(HelperApplication.THIRD,true));}
        StringBuilder out=new StringBuilder();int n=expanded?rows.size():Math.min(5,rows.size());for(int i=Math.max(0,rows.size()-n);i<rows.size();i++)out.append(rows.get(i)).append('\n');
        logText.setText(rows.isEmpty()?"暂无诊断记录":out.toString().trim());
        String lastReport=rows.isEmpty()?"未知（Settings 尚无回报）":rows.get(rows.size()-1).split(" \\| ",2)[0];
        String fingerprint=Build.FINGERPRINT; if(fingerprint.length()>64)fingerprint=fingerprint.substring(0,64)+"…";
        device.setText("Android "+Build.VERSION.RELEASE+"（SDK "+Build.VERSION.SDK_INT+"）\n设备："+Build.MANUFACTURER+" "+Build.MODEL+"\n构建："+Build.DISPLAY+"\n指纹片段："+fingerprint+"\n最近 Settings 回报："+lastReport);
    }
    private void save(String key,boolean value){AppConfig.prefs().edit().putBoolean(key,value).apply();boolean sent=HelperApplication.sync();config.setText(sent?"已下发；需 Settings Hook 读取":"本机已保存；框架服务未连接");Snackbar.make(root,sent?"配置已下发，重新打开克隆应用页面":"配置暂未送达",Snackbar.LENGTH_LONG).show();}
    private void openClonePage(){
        Intent i=new Intent("android.settings.MANAGE_CLONED_APPS_SETTINGS").setPackage("com.android.settings");
        if(i.resolveActivity(getPackageManager())!=null){try{startActivity(i);return;}catch(RuntimeException ignored){}}
        new MaterialAlertDialogBuilder(this).setTitle("原生克隆页面不可用").setMessage("系统未公开可解析的克隆页面，可改为打开应用设置。")
          .setNegativeButton("取消",null).setPositiveButton("打开应用设置",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS))).show();
    }
    private void exportLogs(){new MaterialAlertDialogBuilder(this).setTitle("导出本地诊断").setMessage("分享前请检查内容。日志只包含时间、Hook 事件与设备构建信息，不包含应用列表或账户数据。")
        .setNegativeButton("取消",null).setPositiveButton("继续",(d,w)->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,diagnostics());startActivity(Intent.createChooser(i,"分享诊断"));}).show();}
    private void clearLogs(){new MaterialAlertDialogBuilder(this).setTitle("清除日志").setMessage("清除本机保存的模块诊断记录？").setNegativeButton("取消",null).setPositiveButton("清除",(d,w)->{getSharedPreferences("diagnostics",0).edit().clear().apply();refresh();}).show();}
    private List<String> logs(){List<String> r=new ArrayList<>(getSharedPreferences("diagnostics",0).getStringSet("logs",Collections.emptySet()));Collections.sort(r);return r;}
    private String diagnostics(){return "Pixel Clone Profile Helper v0.1\n"+device.getText()+"\n"+framework.getText()+"\n"+scope.getText()+"\n"+hook.getText()+"\n"+config.getText()+"\n"+logText.getText();}
    private void copyDiagnostics(){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("diagnostics",diagnostics()));Snackbar.make(root,"诊断信息已复制",Snackbar.LENGTH_SHORT).show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
