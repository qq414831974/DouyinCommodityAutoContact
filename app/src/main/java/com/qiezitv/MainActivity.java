package com.qiezitv;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.dy.fastframework.activity.BaseActivity;
import com.dy.fastframework.view.CommonMsgDialog;
import com.qiezitv.view.TsUtils;
import com.yw.game.floatmenu.FloatItem;
import com.yw.game.floatmenu.FloatLogoMenu;
import com.yw.game.floatmenu.FloatMenuView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import yin.deng.normalutils.utils.LogUtils;
import yin.deng.normalutils.utils.MyUtils;
import yin.deng.normalutils.utils.NoDoubleClickListener;

public class MainActivity extends BaseActivity implements ServiceConnection {
    private static final int REQUEST_OVERLAY = 3211;

    private Button btStart;
    private Switch switchIsOpen;
    private Switch switchIsAutoClose;
    private TextView tvResults;
    private CommonMsgDialog msgDialog;
    private List<FloatItem> itemList = new ArrayList<>();
    private FloatLogoMenu mFloatMenu;
    private Intent intent;
    private MyNotifyService.MyWorkService workService;


    @Override
    public int setLayout() {
        return R.layout.activity_main;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!isAccessibilitySettingsOn(this,
                AccessibilityAutoContactService.class.getName())) {// 判断服务是否开启
            if (msgDialog != null && msgDialog.isShowing()) {
                msgDialog.dismiss();
                msgDialog = null;
            }
            msgDialog = new CommonMsgDialog(this);
            msgDialog.setCancelable(false);
            msgDialog.getHolder().tvTitle.setText("系统提示");
            msgDialog.getHolder().tvSure.setText("去开启");
            msgDialog.getHolder().tvCancle.setVisibility(View.GONE);
            msgDialog.getHolder().tvMiddle.setVisibility(View.GONE);
            msgDialog.getHolder().tvContent.setText("使用此功能需要开启无障碍服务，请点击去开启手动打开！");
            msgDialog.getHolder().llProgress.setVisibility(View.GONE);
            msgDialog.getHolder().tvSure.setOnClickListener(new NoDoubleClickListener() {
                @Override
                protected void onNoDoubleClick(View v) {
                    msgDialog.dismiss();
                    jumpToSettingPage(MainActivity.this);// 跳转到开启页面
                }
            });
            msgDialog.show();
        } else {
            TsUtils.showTips("无障碍服务已开启，准备就绪");
            //do other things...
        }
        int nowCount = BaseApp.getSharedPreferenceUtil().getInt("count");
        tvResults.setText("上次发送条数：" + nowCount + "条");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        if (AccessibilityAutoContactService.isSwitchOpen) {
            switchIsOpen.setChecked(true);
        } else {
            switchIsOpen.setChecked(false);

        }
    }

    //判断自定义辅助功能服务是否开启
    public static boolean isAccessibilitySettingsOn(Context context, String className) {
        if (context == null) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            List<ActivityManager.RunningServiceInfo> runningServices =
                    activityManager.getRunningServices(200);// 获取正在运行的服务列表
            if (runningServices.size() < 0) {
                return false;
            }
            for (int i = 0; i < runningServices.size(); i++) {
                ComponentName service = runningServices.get(i).service;
                if (service.getClassName().equals(className)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    //跳转到设置页面无障碍服务开启自定义辅助功能服务
    public static void jumpToSettingPage(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void bindViewWithId() {
        btStart = findViewById(R.id.bt_start);
        switchIsOpen = findViewById(R.id.s_is_open);
        switchIsAutoClose = findViewById(R.id.s_is_auto_close);
        tvResults = findViewById(R.id.tv_results);
    }

    @Override
    public void initFirst() {
        intent = new Intent();
        intent.setClass(this, MyNotifyService.class);
        startService(intent);
        bindService(intent, this, BIND_AUTO_CREATE);
        try {
            OutputStream os = Runtime.getRuntime().exec("su").getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initStatus();
        switchIsOpen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AccessibilityAutoContactService.isSwitchOpen = switchIsOpen.isChecked();
                if (switchIsOpen.isChecked()) {
                    switchIsOpen.setText("任务开启");
                } else {
                    switchIsOpen.setText("任务关闭");
                }
            }
        });
        switchIsAutoClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AccessibilityAutoContactService.needAutoClose = switchIsAutoClose.isChecked();
                if (switchIsAutoClose.isChecked()) {
                    switchIsAutoClose.setText("凌晨3点自动关闭");
                } else {
                    switchIsAutoClose.setText("无限执行任务");
                }
            }
        });
        btStart.setOnClickListener(new NoDoubleClickListener() {
            @Override
            protected void onNoDoubleClick(View v) {
                initStatus();
                AccessibilityAutoContactService.refreshNowData();
                BaseApp.getSharedPreferenceUtil().saveInt("count", 0);
                RequestOverlayPermission();
            }
        });
        TsUtils.showTips("准备开始任务");
    }

    private void initStatus() {
    }

    @Override
    public void onDestroy() {
        unbindService(this);
        stopService(intent);
        super.onDestroy();
    }

    // 动态请求悬浮窗权限
    private void RequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                String ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION";
                Intent intent = new Intent(ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY);
            } else {
//                openFloatView();
                launchDouYin();
            }
        }
    }

    /**
     * Activity执行结果，回调函数
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Toast.makeText(activity, "onActivityResult设置权限！", Toast.LENGTH_SHORT).show();
        if (requestCode == REQUEST_OVERLAY)        // 从应用权限设置界面返回
        {
            if (resultCode == RESULT_OK) {
                LogUtils.i("悬浮窗的权限ok了");
                openFloatView();
            }
        }
    }


    /**
     * 开启悬浮框
     */
    private void openFloatView() {
        if (mFloatMenu == null) {
            initData();
            mFloatMenu = new FloatLogoMenu.Builder()
                    .withContext(workService.getService().getApplication())//这个在7.0（包括7.0）以上以及大部分7.0以下的国产手机上需要用户授权，需要搭配<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
                    .logo(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_main))
                    .drawCicleMenuBg(false)
                    .backMenuColor(0xffe4e3e1)
                    .setBgDrawable(new ColorDrawable(Color.parseColor("#ebebeb")))
                    //这个背景色需要和logo的背景色一致
                    .setFloatItems(itemList)
                    .defaultLocation(FloatLogoMenu.LEFT)
                    .drawRedPointNum(false)
                    .showWithListener(new FloatMenuView.OnMenuClickListener() {
                        @Override
                        public void onItemClick(int position, String title) {
                            if (position == 0) {
                                if (AccessibilityAutoContactService.isSwitchOpen) {
                                    switchIsOpen.setChecked(false);
                                    TsUtils.showTips("已停止所有任务");
                                } else {
                                    switchIsOpen.setChecked(true);
                                    TsUtils.showTips("任务开始");
                                }
                                AccessibilityAutoContactService.isSwitchOpen = switchIsOpen.isChecked();
                                itemList.get(0).setTitle("状态:" + (AccessibilityAutoContactService.isSwitchOpen ? "开" : "关"));
                                mFloatMenu.openMenu();
                            }
                        }

                        @Override
                        public void dismiss() {

                        }
                    });
            launchDouYin();
        } else {
            launchDouYin();
        }
    }

    private void initData() {
        itemList.clear();
        final boolean isSwitchOpen = AccessibilityAutoContactService.isSwitchOpen;
        FloatItem floatItem1 = new FloatItem("状态:" + (isSwitchOpen ? "开" : "关"), Color.WHITE, getResources().getColor(R.color.normal_gray), BitmapFactory.decodeResource(getResources(), R.mipmap.ic_is_open));
        itemList.add(floatItem1);
    }


    /**
     * 启动第三方apk
     * 直接打开  每次都会启动到启动界面，每次都会干掉之前的，从新启动
     * XXXXX ： 包名
     */
    public void launchDouYin() {
        PackageManager packageManager = getPackageManager();
        Intent it = packageManager.getLaunchIntentForPackage("com.ss.android.ugc.aweme");
        try {
            startActivity(it);
        } catch (Exception e) {
            e.printStackTrace();
            TsUtils.showTips("启动失败，请确认您安装了抖音");
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        LogUtils.i("服务已开启");
        TsUtils.showTips("后台服务已开启");
        workService = ((MyNotifyService.MyWorkService) service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}