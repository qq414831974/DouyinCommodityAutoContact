package com.qiezitv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.dy.fastframework.view.CommonMsgDialog;
import com.qiezitv.callback.Callback;
import com.qiezitv.view.TsUtils;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import yin.deng.normalutils.utils.DownTimer;
import yin.deng.normalutils.utils.LogUtils;

public class AccessibilityAutoContactService extends AccessibilityService {
    public static final int DOING_TASK = -1;//准备开始
    public static final int NORMAL = 0;//准备开始
    public static final int CLICK_TAB = 1;//点击好物tab
    public static final int CLICK_SHOP_ITEM = 2;//点击抖音商城
    public static final int FIND_GOODS = 3;//寻找商品
    public static final int SCREEN_NEXT_PAGE = 4;//下滑下一页
    public static final int CLICK_GOODS = 5;//点击商品
    public static final int CLICK_CONTACT = 6;//点击客服
    public static final int COMMENTING = 7;//联系开始
    public static final int COMMENT_OVER = 8;//联系结束
    public int nowState = NORMAL;
    public boolean isPauseTimer = true;
    AccessibilityEvent mAccessibilityEvent;
    public static boolean isSwitchOpen = false;//是否开启辅助
    public static boolean isProcessClickTab = false;//是否正在点击tabbar
    public static boolean isProcessClickGoods = false;//是否正在点击tabbar
    public static boolean isProcessClickContact = false;//是否正在点击tabbar
    private DownTimer timer;
    /**************版本更新只需修改此部分的6个id即可***********************/
    //视频发布的左下角音乐封面id
    public String tabTag = "android:id/tabs";
    public String shopItemTag = "com.ss.android.ugc.live:id/bqb";
    public String activityCoverTag = "com.bytedance.android.shopping:id/iv_ec_activity_cover";
    public String tabbarClass = "com.bytedance.ies.xelement.viewpager.LynxTabBarView";
    public String tabClass = "com.bytedance.ies.xelement.viewpager.childitem.LynxTabbarItem";
    public String dollarClass = "com.lynx.tasm.behavior.ui.text.UIText";
    public String contactTag = "com.ss.android.ugc.aweme:id/v1";
    private String updateCancleBtId = "com.ss.android.ugc.aweme:id/ctq";//更新提示的取消按钮id
    /*************************基础配置*******************************************/
    private boolean isKillSelfDoing = false;
    public static boolean needAutoClose = false;//是否需要自动关闭所有程序

    /**
     * 重置当前所有标记类数据
     */
    public static void refreshNowData() {

    }

    long lastDoTime = 0;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (System.currentTimeMillis() - lastDoTime > 1000) {
                lastDoTime = System.currentTimeMillis();
                if (isSwitchOpen) {
                    isSwitchOpen = false;
                    showTs("暂停任务");
                } else {
                    isSwitchOpen = true;
                    showTs("开始任务");
                }
            }
        }
        return super.onKeyEvent(event);
    }

    SimpleDateFormat format = new SimpleDateFormat("HH");

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        mAccessibilityEvent = event;
        if (!isSwitchOpen) {
            LogUtils.v("已关闭辅助");
            return;
        }
        closeUpdateDialog();
        //开始干活
        switch (nowState) {
            case DOING_TASK:
                break;
            case NORMAL:
                LogUtils.d("开始干活了，当前类型：" + nowState);
                List<AccessibilityNodeInfo> tabNodes = findNodesById(tabTag);
                if (isOk(tabNodes)) {
                    AccessibilityNodeInfo meNodeParent = tabNodes.get(0);
                    meNodeParent.getChild(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    nowState = CLICK_TAB;
                } else {
                    LogUtils.e("暂未找到`我`标签");
                }
                break;
            case CLICK_TAB:
                List<AccessibilityNodeInfo> shopNodes = findNodesById(shopItemTag);
                if (isOk(shopNodes)) {
                    shopNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    nowState = CLICK_SHOP_ITEM;
                } else {
                    LogUtils.e("暂未找到`抖音商城`标签");
                }
                break;
            case CLICK_SHOP_ITEM:
                if (isProcessClickTab) {
                    break;
                }
                isProcessClickTab = true;
                List<AccessibilityNodeInfo> tabbarNodes = findNodesByClass(tabbarClass);
                if (tabbarNodes.size() <= 0) {
                    isProcessClickTab = false;
                    break;
                }
                AccessibilityNodeInfo tabbarNode = tabbarNodes.get(0);
                Rect tabbarRect = new Rect();
                tabbarNode.getBoundsInScreen(tabbarRect);
                int tabbarSlideY = (tabbarRect.top + tabbarRect.bottom) / 2;
                List<AccessibilityNodeInfo> tabNodes = findNodesByClass(tabClass);
                int tabIndex = 0;
                for (int i = 0; i < tabNodes.size(); i++) {
                    AccessibilityNodeInfo tabNode = tabNodes.get(i);
                    if (tabNode.getText().equals("运动")) {
                        tabIndex = i;
                    }
                }
                if (tabIndex > 4) {
                    moveTo(tabbarRect.right - 60, tabbarRect.left + 60, tabbarSlideY, tabbarSlideY, tabIndex / 4, new Callback() {
                        @Override
                        public void callback() {
                            clickYundong();
                        }
                    });
                } else {
                    clickYundong();
                }
                break;
            case FIND_GOODS:
                if (isProcessClickGoods) {
                    break;
                }
                isProcessClickGoods = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        clickGoods();
                    }
                }, 5000);
                break;
            case SCREEN_NEXT_PAGE:
                break;
            case CLICK_GOODS:
                List<AccessibilityNodeInfo> contactNodes = findNodesById(contactTag);
                if (isOk(contactNodes)) {
                    AccessibilityNodeInfo contactNodeParent = contactNodes.get(0).getParent();
                    contactNodeParent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            nowState = CLICK_CONTACT;
                        }
                    }, 2000);
                } else {
                    LogUtils.e("暂未找到`客服`标签");
                }
                break;
            case CLICK_CONTACT:
                if (isProcessClickContact) {
                    break;
                }
                isProcessClickContact = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        forceClick(480, 2200, new Callback() {
//                            @Override
//                            public void callback() {
//                                isProcessClickContact = false;
//                            }
//                        });
                    }
                }, 5000);
                break;
            case COMMENTING:
                break;
            case COMMENT_OVER:
                break;
        }
    }
//    com.bytedance.android.shopping:id/iv_ec_activity_cover
//    com.ss.android.ugc.live:id/a3h 闪购

    public void clickYundong() {
        List<AccessibilityNodeInfo> tabNodes = findNodesByClass(tabClass);
        for (int i = 0; i < tabNodes.size(); i++) {
            AccessibilityNodeInfo tabNode = tabNodes.get(i);
            if (tabNode.getText().equals("运动")) {
                Rect tabRect = new Rect();
                tabNode.getBoundsInScreen(tabRect);
                forceClick((tabRect.left + tabRect.right) / 2, (tabRect.top + tabRect.bottom) / 2, new Callback() {
                    @Override
                    public void callback() {
                        nowState = FIND_GOODS;
                        isProcessClickTab = false;
                    }
                });
                break;
            }
        }
    }

    private List<AccessibilityNodeInfo> clickedIndexs = new ArrayList<>();

    public void clickGoods() {
        List<AccessibilityNodeInfo> dollarNodes = findNodesByClassAndText(dollarClass, "￥");
        boolean hasGoods = false;
        for (int i = 0; i < dollarNodes.size(); i++) {
            final AccessibilityNodeInfo dollarNode = dollarNodes.get(i);
            if (dollarNode.getText().toString().equals("￥")) {
                Rect dollarRect = new Rect();
                dollarNode.getBoundsInScreen(dollarRect);
                if (dollarRect.bottom > 0 && dollarRect.top > 0 && dollarRect.left > 0 && dollarRect.right > 0 && !clickedIndexs.contains(dollarNode)) {
                    hasGoods = true;
                    forceClick(dollarRect.right, dollarRect.top, new Callback() {
                        @Override
                        public void callback() {
                            clickedIndexs.add(dollarNode);
                            nowState = CLICK_GOODS;
                            isProcessClickGoods = false;
                        }
                    });
                    break;
                }
            }
        }
        if (!hasGoods) {
            int moveLength = 809 * 2;
            moveTo(540, 540, 2250, 2250 - moveLength, 1, new Callback() {
                @Override
                public void callback() {
                    clickGoods();
                }
            });
        }
    }

    public void closeUpdateDialog() {
        List<AccessibilityNodeInfo> nodeCancleBt = findNodesById(updateCancleBtId);
        if (isOk(nodeCancleBt)) {
            nodeCancleBt.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
            LogUtils.i("关闭更新提示");
        }
    }

    private void moveTo(final int x1, final int x2, final int y1, final int y2, final int times, final Callback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                    (path, 20, 600)).build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    if (times > 1) {
                        moveTo(x1, x2, y1, y2, times - 1, callback);
                    } else {
                        if (callback != null) {
                            callback.callback();
                        }
                    }
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);

                }
            }, new Handler(getMainLooper()));
            //滑动完成
        }
    }

    /*
     * 杀死后台进程
     */
    public void killAll(boolean needReOpenPhone) {
        isSwitchOpen = false;
        showTs("任务已结束");
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            }
        }, 200);
    }


    private void reOpenPhone() {
        LogUtils.v("关机重启");
        String cmd = "reboot";
        doCmd(cmd);
    }

    private void forceBack() {
        LogUtils.v("点击返回键");
        String cmd = "input keyevent 4";
        doCmd(cmd);
    }

    private void showTs(final String msg) {
        LogUtils.i("当前操作：" + msg);
        TsUtils.showTips(msg);
    }

    private void showDialog(final String msg) {
        Handler handlerThree = new Handler(Looper.getMainLooper());
        handlerThree.post(new Runnable() {
            public void run() {
                CommonMsgDialog msgDialog = new CommonMsgDialog(getApplicationContext());
                msgDialog.getHolder().tvTitle.setText("系统提示");
                msgDialog.getHolder().tvSure.setText("确定");
                msgDialog.getHolder().tvMiddle.setVisibility(View.GONE);
                msgDialog.getHolder().tvCancle.setVisibility(View.GONE);
                msgDialog.getHolder().tvContent.setText(msg);
                msgDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                msgDialog.show();
            }
        });
    }


    /**
     * 返回该节点是否存在或有效
     *
     * @param nodes
     * @return
     */
    private boolean isOk(List<AccessibilityNodeInfo> nodes) {
        if (nodes != null && nodes.size() > 0 && nodes.get(0).isEnabled()) {
            return true;
        }
        return false;
    }

    long lastClickTime = 0;
    boolean isClickZan = false;

    private void forceClick(int x, int y, final Callback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path path = new Path();
            path.moveTo(x, y);
            path.lineTo(x, y);
            dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription
                    (path, 1, 1)).build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    if (callback != null) {
                        callback.callback();
                    }
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                }
            }, new Handler(getMainLooper()));
            //滑动完成
        }
    }

    public void doCmd(String cmd) {
        try {
            OutputStream os;
            os = Runtime.getRuntime().exec("su").getOutputStream();
            os.write(cmd.getBytes());
            os.flush();//清空缓存
            os.close();//停止流
            LogUtils.e("命令执行完成");
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
            LogUtils.v("命令出错了:" + e.getMessage());
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        LogUtils.d("qqqqqq无障碍服务已开启");
    }

    public void setNodeText(AccessibilityNodeInfo node, String text) {
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    @Override
    public void onInterrupt() {

    }

    private AccessibilityNodeInfo getRootNodeInfo() {
        AccessibilityEvent curEvent = mAccessibilityEvent;
        AccessibilityNodeInfo nodeInfo = null;
        if (Build.VERSION.SDK_INT >= 16) {
            nodeInfo = getRootInActiveWindow();
        } else {
            nodeInfo = curEvent.getSource();
        }
        return nodeInfo;
    }

    public List<AccessibilityNodeInfo> findNodesByText(String text) {
        AccessibilityNodeInfo nodeInfo = getRootNodeInfo();
        if (nodeInfo != null) {
            Log.i("accessibility", "getClassName：" + nodeInfo.getClassName());
            Log.i("accessibility", "getText：" + nodeInfo.getText());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                //需要在xml文件中声明权限android:accessibilityFlags="flagReportViewIds"
                // 并且版本大于4.3 才能获取到view 的 ID
                Log.i("accessibility", "getClassName：" + nodeInfo.getViewIdResourceName());
            }
            return nodeInfo.findAccessibilityNodeInfosByText(text);
        }
        return null;
    }

    private AccessibilityNodeInfo getChildNodeInfos(String id, int childIndex) {
        List<AccessibilityNodeInfo> listChatRecord = findNodesById(id);
        if (listChatRecord == null || listChatRecord.size() == 0) {
            return null;
        }
        AccessibilityNodeInfo parentNode = listChatRecord.get(0);//该节点
        int count = parentNode.getChildCount();
        Log.i("accessibility", "子节点个数 " + count);
        return childIndex < count ? parentNode.getChild(childIndex) : null;
    }


    public List<AccessibilityNodeInfo> findNodesById(String viewId) {
        AccessibilityNodeInfo nodeInfo = getRootNodeInfo();
        if (nodeInfo != null) {
            if (Build.VERSION.SDK_INT >= 18) {
                return nodeInfo.findAccessibilityNodeInfosByViewId(viewId);
            }
        }
        return null;
    }

    public List<AccessibilityNodeInfo> findNodesByClass(String viewClass) {
        AccessibilityNodeInfo rootInfo = getRootNodeInfo();
        AccessibilityNodeInfo shopRootInfo = rootInfo.findAccessibilityNodeInfosByViewId(shopRootViewTag).get(0);
        AccessibilityNodeInfo nodeInfo = shopRootInfo.getChild(0);
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        if (nodeInfo != null) {
            if (Build.VERSION.SDK_INT >= 18) {
                for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                    AccessibilityNodeInfo child = nodeInfo.getChild(i);
                    if (child.getClassName().toString().equals(viewClass)) {
                        nodes.add(child);
                    }
                }
            }
        }
        return nodes;
    }

    public List<AccessibilityNodeInfo> findNodesByClassAndText(String viewClass, String text) {
        AccessibilityNodeInfo rootInfo = getRootNodeInfo();
        AccessibilityNodeInfo shopRootInfo = rootInfo.findAccessibilityNodeInfosByViewId(shopRootViewTag).get(0);
        AccessibilityNodeInfo nodeInfo = shopRootInfo.getChild(0);
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        if (nodeInfo != null) {
            if (Build.VERSION.SDK_INT >= 18) {
                for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                    AccessibilityNodeInfo child = nodeInfo.getChild(i);
                    if (child.getClassName().toString().equals(viewClass) && child.getText().equals(text)) {
                        nodes.add(child);
                    }
                }
            }
        }
        return nodes;
    }
}
