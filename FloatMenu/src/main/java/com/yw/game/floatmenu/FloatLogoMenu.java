/*
 * Copyright (c) 2016, Shanghai YUEWEN Information Technology Co., Ltd.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  Neither the name of Shanghai YUEWEN Information Technology Co., Ltd. nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY SHANGHAI YUEWEN INFORMATION TECHNOLOGY CO., LTD. AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.yw.game.floatmenu;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by wengyiming on 2017/7/20.
 */
public class FloatLogoMenu {
    /**
     * ?????? logo ????????????????????????????????????
     */
    private static final String LOCATION_X = "hintLocation";
    private static final String LOCATION_Y = "locationY";

    /**
     * ????????? ?????? ??? ??? ??????
     */
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    /**
     * ??????????????????????????????
     */
    private int mStatusBarHeight;
    /**
     * ???????????????????????????????????????????????????
     */
    private float mXInScreen;

    /**
     * ???????????????????????????????????????????????????
     */
    private float mYInScreen;

    /**
     * ???????????????????????????????????????????????????
     */
    private float mXDownInScreen;

    /**
     * ???????????????????????????????????????????????????
     */
    private float mYDownInScreen;

    /**
     * ???????????????????????????????????????View?????????????????????
     */
    private float mXInView;

    /**
     * ???????????????????????????????????????View?????????????????????
     */
    private float mYinView;

    /**
     * ?????????????????????
     */
    private int mScreenWidth;

    /**
     * ?????? activity ??? wManager
     */
    private WindowManager wManager;


    /**
     * ??? wManager ?????? LayoutParams
     */
    private WindowManager.LayoutParams wmParams;

    /**
     * ????????????????????????????????????????????????
     */
    private DotImageView mFloatLogo;


    /**
     * ?????? ?????? ?????? logo????????????
     */
    private CountDownTimer mHideTimer;


    /**
     * float menu?????????
     */
    private Handler mHandler = new Handler(Looper.getMainLooper());


    /**
     * ???????????????????????????????????? ????????? ?????????
     */
    private Interpolator mLinearInterpolator = new LinearInterpolator();

    /**
     * ????????????????????????????????????????????????????????????
     */
    private static double DOUBLE_CLICK_TIME = 0L;

    /**
     * ?????????????????????
     */
    private boolean isDrag = false;

    /**
     * ????????????????????????location??????????????????
     */
    private int mResetLocationValue;

    /**
     * ????????????????????? ???????????? ???????????? logo ???????????????
     */
    private Runnable updatePositionRunnable = new Runnable() {
        @Override
        public void run() {
            isDrag = true;
            checkPosition();
        }
    };

    /**
     * ???????????????????????????????????????return false??? onclick ????????????
     */
    private OnTouchListener mDefaultOnTouchListerner = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            isDrag = false;
            return false;
        }
    };

    /**
     * ?????????????????????????????????????????????????????????????????????return true????????????onclick????????????
     */
    private OnTouchListener touchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    floatEventDown(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    floatEventMove(event);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    floatEventUp();
                    break;
            }
            return true;
        }
    };


    /**
     * ??????????????????
     */
    private int mBackMenuColor = 0xffe4e3e1;

    /**
     * ????????????????????????
     */
    private boolean mDrawRedPointNum;


    /**
     * ??????????????????????????????false????????????
     */
    private boolean mCircleMenuBg;


    /**
     * R.drawable.yw_game_logo
     *
     * @param floatItems
     */
    private Bitmap mLogoRes;

    /**
     * ??????????????? mActivity ?????? mActivity
     */
    private Context mActivity;

    /**
     * ?????? ??????????????? ??????
     */
    private FloatMenuView.OnMenuClickListener mOnMenuClickListener;


    /**
     * ??????????????????
     */
    private int mDefaultLocation = RIGHT;


    /**
     * ????????? ?????? ??????
     */
    private int mHintLocation = mDefaultLocation;


    /**
     * ???????????????????????????
     */
    private List<FloatItem> mFloatItems = new ArrayList<>();

    private LinearLayout rootViewRight;

    private LinearLayout rootView;

    private ValueAnimator valueAnimator;

    private boolean isExpanded = false;

    private Drawable mBackground;


    private FloatLogoMenu(Builder builder) {
        mBackMenuColor = builder.mBackMenuColor;
        mDrawRedPointNum = builder.mDrawRedPointNum;
        mCircleMenuBg = builder.mCircleMenuBg;
        mLogoRes = builder.mLogoRes;
        mActivity = builder.mActivity;
        mOnMenuClickListener = builder.mOnMenuClickListener;
        mDefaultLocation = builder.mDefaultLocation;
        mFloatItems = builder.mFloatItems;
        mBackground = builder.mDrawable;

//        if (mActivity == null || mActivity.isFinishing() || mActivity.getWindowManager() == null) {
//            throw new IllegalArgumentException("Activity = null, or Activity is isFinishing ,or this Activity`s  token is bad");
//        }

        if (mLogoRes == null) {
            throw new IllegalArgumentException("No logo found,you can setLogo/showWithLogo to set a FloatLogo ");
        }

        if (mFloatItems.isEmpty()) {
            throw new IllegalArgumentException("At least one menu item!");
        }

        initFloatWindow();
        initTimer();
        initFloat();

    }

    public void setFloatItemList(List<FloatItem> floatItems) {
        this.mFloatItems = floatItems;
        calculateDotNum();
    }

    /**
     * ?????????????????? window
     */
    private void initFloatWindow() {
        wmParams = new WindowManager.LayoutParams();
        if (mActivity instanceof Activity) {
            Activity activity = (Activity) mActivity;
            wManager = activity.getWindowManager();
            //??????dialog????????????activity???windows???,activity???????????????????????????float
            wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        } else {
            wManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //???android7.1????????????????????????TYPE_PHONE?????? ?????????????????????
                wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                //???android7.1????????????????????????TYPE_PHONE?????? ?????????????????????
                wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                wmParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            }
        }
        mScreenWidth = wManager.getDefaultDisplay().getWidth();
        int screenHeight = wManager.getDefaultDisplay().getHeight();

        //??????????????????????????? ??????????????????statusBarHeight???0
        mStatusBarHeight = dp2Px(25, mActivity);

        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mHintLocation = getSetting(LOCATION_X, mDefaultLocation);
        int defaultY = ((screenHeight - mStatusBarHeight) / 2) / 3;
        int y = getSetting(LOCATION_Y, defaultY);
        if (mHintLocation == LEFT) {
            wmParams.x = 0;
        } else {
            wmParams.x = mScreenWidth;
        }

        if (y != 0 && y != defaultY) {
            wmParams.y = y;
        } else {
            wmParams.y = defaultY;
        }
        wmParams.alpha = 1;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
    }


    /**
     * ??????????????????
     */
    private void initFloat() {
        generateLeftLineLayout();
        generateRightLineLayout();
        mFloatLogo = new DotImageView(mActivity, mLogoRes);
        mFloatLogo.setLayoutParams(new WindowManager.LayoutParams(dp2Px(50, mActivity), dp2Px(50, mActivity)));
        mFloatLogo.setDrawNum(mDrawRedPointNum);
        mFloatLogo.setBgColor(mBackMenuColor);
        mFloatLogo.setDrawDarkBg(true);
        calculateDotNum();
        floatBtnEvent();
        try {
            wManager.addView(mFloatLogo, wmParams);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void generateLeftLineLayout() {
        DotImageView floatLogo = new DotImageView(mActivity, mLogoRes);
        floatLogo.setLayoutParams(new WindowManager.LayoutParams(dp2Px(60, mActivity), dp2Px(60, mActivity)));
        floatLogo.setDrawNum(mDrawRedPointNum);
        floatLogo.setDrawDarkBg(false);

        rootView = new LinearLayout(mActivity);
        rootView.setOrientation(LinearLayout.HORIZONTAL);
        rootView.setGravity(Gravity.CENTER);
        rootView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp2Px(60, mActivity)));

        rootView.setBackgroundDrawable(mBackground);


        FloatMenuView mFloatMenuView = new FloatMenuView.Builder(mActivity)
                .setFloatItems(mFloatItems)
                .setBackgroundColor(Color.TRANSPARENT)
                .setCicleBg(mCircleMenuBg)
                .setStatus(FloatMenuView.STATUS_LEFT)
                .setMenuBackgroundColor(Color.TRANSPARENT)
                .drawNum(mDrawRedPointNum)
                .create();
        setMenuClickListener(mFloatMenuView);

        rootView.addView(floatLogo);
        rootView.addView(mFloatMenuView);


        floatLogo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExpanded) {
                    try {
                        wManager.removeViewImmediate(rootView);
                        wManager.addView(FloatLogoMenu.this.mFloatLogo, wmParams);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isExpanded = false;
                }
            }
        });
    }

    private void generateRightLineLayout() {
        final DotImageView floatLogo = new DotImageView(mActivity, mLogoRes);
        floatLogo.setLayoutParams(new WindowManager.LayoutParams(dp2Px(70, mActivity), dp2Px(70, mActivity)));
        floatLogo.setDrawNum(mDrawRedPointNum);
        floatLogo.setDrawDarkBg(false);

        floatLogo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isExpanded) {
                    try {
                        wManager.removeViewImmediate(rootViewRight);
                        wManager.addView(FloatLogoMenu.this.mFloatLogo, wmParams);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isExpanded = false;
                }
            }
        });

        rootViewRight = new LinearLayout(mActivity);
        rootViewRight.setOrientation(LinearLayout.HORIZONTAL);
        rootViewRight.setGravity(Gravity.CENTER);
        rootViewRight.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp2Px(100, mActivity)));


        rootViewRight.setBackgroundDrawable(mBackground);


        FloatMenuView mFloatMenuView = new FloatMenuView.Builder(mActivity)
                .setFloatItems(mFloatItems)
                .setBackgroundColor(Color.TRANSPARENT)
                .setCicleBg(mCircleMenuBg)
                .setStatus(FloatMenuView.STATUS_RIGHT)
                .setMenuBackgroundColor(Color.TRANSPARENT)
                .drawNum(mDrawRedPointNum)
                .create();
        setMenuClickListener(mFloatMenuView);

        rootViewRight.addView(mFloatMenuView);
        rootViewRight.addView(floatLogo);


    }

    /**
     * ????????? ???????????????????????????
     */
    private void initTimer() {
        mHideTimer = new CountDownTimer(2000, 10) {        //???????????????5????????????????????????????????????
            @Override
            public void onTick(long millisUntilFinished) {
                if (isExpanded) {
                    mHideTimer.cancel();
                }
            }

            @Override
            public void onFinish() {
                if (isExpanded) {
                    mHideTimer.cancel();
                    return;
                }
                if (!isDrag) {
                    if (mHintLocation == LEFT) {
                        mFloatLogo.setStatus(DotImageView.HIDE_LEFT);
                        mFloatLogo.setDrawDarkBg(true);
                    } else {
                        mFloatLogo.setStatus(DotImageView.HIDE_RIGHT);
                        mFloatLogo.setDrawDarkBg(true);
                    }
//                    mFloatLogo.setOnTouchListener(mDefaultOnTouchListerner);//???onClick???????????????????????????onclick??????
                }
            }
        };
    }


    /**
     * ?????? ?????? ???????????? ?????????????????????????????? ???????????????
     *
     * @param mFloatMenuView
     */
    private void setMenuClickListener(FloatMenuView mFloatMenuView) {
        mFloatMenuView.setOnMenuClickListener(new FloatMenuView.OnMenuClickListener() {
            @Override
            public void onItemClick(int position, String title) {
                mOnMenuClickListener.onItemClick(position, title);
            }

            @Override
            public void dismiss() {
                mFloatLogo.setDrawDarkBg(true);
                mOnMenuClickListener.dismiss();
                mHideTimer.start();
            }
        });

    }


    /**
     * ???????????????????????????touch???????????????
     */
    private void floatBtnEvent() {
        //?????????onClick?????? touchListener = mDefaultOnTouchListener ????????????
//        mFloatLogo.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!isDrag) {
//                    if (mFloatLogo.getStatus() != DotImageView.NORMAL) {
//                        mFloatLogo.setBitmap(mLogoRes);
//                        mFloatLogo.setStatus(DotImageView.NORMAL);
//                        if (!mFloatLogo.mDrawDarkBg) {
//                            mFloatLogo.setDrawDarkBg(true);
//                        }
//                    }
//                    mFloatLogo.setOnTouchListener(touchListener);
//                    mHideTimer.start();
//                }
//            }
//        });

        mFloatLogo.setOnTouchListener(touchListener);//??????touch??????
    }

    /**
     * ?????????touch????????? down ??????
     */
    private void floatEventDown(MotionEvent event) {
        isDrag = false;
        mHideTimer.cancel();
        if (mFloatLogo.getStatus() != DotImageView.NORMAL) {
            mFloatLogo.setStatus(DotImageView.NORMAL);
        }
        if (!mFloatLogo.mDrawDarkBg) {
            mFloatLogo.setDrawDarkBg(true);
        }
        if (mFloatLogo.getStatus() != DotImageView.NORMAL) {
            mFloatLogo.setStatus(DotImageView.NORMAL);
        }
        mXInView = event.getX();
        mYinView = event.getY();
        mXDownInScreen = event.getRawX();
        mYDownInScreen = event.getRawY();
        mXInScreen = event.getRawX();
        mYInScreen = event.getRawY();


    }

    /**
     * ?????????touch????????? move ??????
     */
    private void floatEventMove(MotionEvent event) {
        mXInScreen = event.getRawX();
        mYInScreen = event.getRawY();


        //???????????????????????????3???????????????????????????
        if (Math.abs(mXInScreen - mXDownInScreen) > mFloatLogo.getWidth() / 4 || Math.abs(mYInScreen - mYDownInScreen) > mFloatLogo.getWidth() / 4) {
            wmParams.x = (int) (mXInScreen - mXInView);
            wmParams.y = (int) (mYInScreen - mYinView) - mFloatLogo.getHeight() / 2;
            updateViewPosition(); // ????????????????????????????????????????????????
            double a = mScreenWidth / 2;
            float offset = (float) ((a - (Math.abs(wmParams.x - a))) / a);
            mFloatLogo.setDrag(isDrag, offset, false);
        } else {
            isDrag = false;
            mFloatLogo.setDrag(false, 0, true);
        }
    }

    /**
     * ?????????touch????????? up ??????
     */
    private void floatEventUp() {
        if (mXInScreen < mScreenWidth / 2) {   //?????????
            mHintLocation = LEFT;
        } else {                   //?????????
            mHintLocation = RIGHT;
        }
        if (valueAnimator == null) {
            valueAnimator = ValueAnimator.ofInt(64);
            valueAnimator.setInterpolator(mLinearInterpolator);
            valueAnimator.setDuration(1000);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mResetLocationValue = (int) animation.getAnimatedValue();
                    mHandler.post(updatePositionRunnable);
                }
            });

            valueAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (Math.abs(wmParams.x) < 0) {
                        wmParams.x = 0;
                    } else if (Math.abs(wmParams.x) > mScreenWidth) {
                        wmParams.x = mScreenWidth;
                    }
                    updateViewPosition();
                    isDrag = false;
                    mFloatLogo.setDrag(false, 0, true);
                    mHideTimer.start();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (Math.abs(wmParams.x) < 0) {
                        wmParams.x = 0;
                    } else if (Math.abs(wmParams.x) > mScreenWidth) {
                        wmParams.x = mScreenWidth;
                    }

                    updateViewPosition();
                    isDrag = false;
                    mFloatLogo.setDrag(false, 0, true);
                    mHideTimer.start();

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        if (!valueAnimator.isRunning()) {
            valueAnimator.start();
        }

//        //???????????????????????????????????????????????????logo??????????????????????????????????????????,
        if (Math.abs(mXInScreen - mXDownInScreen) > mFloatLogo.getWidth() / 5 || Math.abs(mYInScreen - mYDownInScreen) > mFloatLogo.getHeight() / 5) {
            isDrag = false;
        } else {
            openMenu();
        }

    }


    /**
     * ???????????????????????????????????????
     */
    private void checkPosition() {
        if (wmParams.x > 0 && wmParams.x < mScreenWidth) {
            if (mHintLocation == LEFT) {
                wmParams.x = wmParams.x - mResetLocationValue;
            } else {
                wmParams.x = wmParams.x + mResetLocationValue;
            }
            updateViewPosition();
            double a = mScreenWidth / 2;
            float offset = (float) ((a - (Math.abs(wmParams.x - a))) / a);
            mFloatLogo.setDrag(isDrag, offset, true);
            return;
        }


        if (Math.abs(wmParams.x) < 0) {
            wmParams.x = 0;
        } else if (Math.abs(wmParams.x) > mScreenWidth) {
            wmParams.x = mScreenWidth;
        }
        if (valueAnimator.isRunning()) {
            valueAnimator.cancel();
        }


        updateViewPosition();
        isDrag = false;


    }


    /**
     * ????????????
     */
    public void openMenu() {
        if (isDrag) return;

        if (!isExpanded) {
            mFloatLogo.setDrawDarkBg(false);
            try {
                wManager.removeViewImmediate(mFloatLogo);
                if (mHintLocation == RIGHT) {
                    wManager.addView(rootViewRight, wmParams);
                } else {
                    wManager.addView(rootView, wmParams);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            isExpanded = true;
            mHideTimer.cancel();
        } else {
            mFloatLogo.setDrawDarkBg(true);
            if (isExpanded) {
                try {
                    wManager.removeViewImmediate(mHintLocation == LEFT ? rootView : rootViewRight);
                    wManager.addView(mFloatLogo, wmParams);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                isExpanded = false;
            }
            mHideTimer.start();
        }

    }


    /**
     * ???????????????????????????????????????
     */
    private void updateViewPosition() {
        isDrag = true;
        try {
            if (!isExpanded) {
                if (wmParams.y - mFloatLogo.getHeight() / 2 <= 0) {
                    wmParams.y = mStatusBarHeight;
                    isDrag = true;
                }
                wManager.updateViewLayout(mFloatLogo, wmParams);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void show() {
        try {
            if (wManager != null && wmParams != null && mFloatLogo != null) {
                wManager.addView(mFloatLogo, wmParams);
            }
            if (mHideTimer != null) {
                mHideTimer.start();
            } else {
                initTimer();
                mHideTimer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     */
    public void hide() {
        destroyFloat();
    }


    /**
     * ????????????????????? ????????????
     */
    public void destroyFloat() {
        //?????????????????????logo????????????????????????????????????
        saveSetting(LOCATION_X, mHintLocation);
        saveSetting(LOCATION_Y, wmParams.y);
        mFloatLogo.clearAnimation();
        try {
            mHideTimer.cancel();
            if (isExpanded) {
                wManager.removeViewImmediate(mHintLocation == LEFT ? rootView : rootViewRight);
            } else {
                wManager.removeViewImmediate(mFloatLogo);
            }
            isExpanded = false;
            isDrag = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????
     */
    private void calculateDotNum() {
        int dotNum = 0;
        for (FloatItem floatItem : mFloatItems) {
            if (!TextUtils.isEmpty(floatItem.getDotNum())) {
                int num = Integer.parseInt(floatItem.getDotNum());
                dotNum = dotNum + num;
            }
        }
        mFloatLogo.setDrawNum(mDrawRedPointNum);
        setDotNum(dotNum);
    }

    /**
     * ????????????????????????
     *
     * @param dotNum d
     */
    private void setDotNum(int dotNum) {
        mFloatLogo.setDotNum(dotNum, new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isDrag) {
                    mHideTimer.start();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param menuLabel string
     * @return boolean
     */
    public boolean hasMenu(String menuLabel) {
        for (FloatItem menuItem : mFloatItems) {
            if (TextUtils.equals(menuItem.getTitle(), menuLabel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ????????????????????????????????????
     *
     * @param key          String
     * @param defaultValue int
     * @return int
     */
    private int getSetting(String key, int defaultValue) {
        try {
            SharedPreferences sharedata = mActivity.getSharedPreferences("floatLogo", 0);
            return sharedata.getInt(key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    /**
     * ????????????????????????????????????
     *
     * @param key   String
     * @param value int
     */
    public void saveSetting(String key, int value) {
        try {
            SharedPreferences.Editor sharedata = mActivity.getSharedPreferences("floatLogo", 0).edit();
            sharedata.putInt(key, value);
            sharedata.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int dp2Px(float dp, Context mContext) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.getResources().getDisplayMetrics());
    }


    public interface OnMenuClickListener {
        void onMenuExpended(boolean isExpened);
    }


    public void setValueAnimator() {

    }

    public static final class Builder {
        private int mBackMenuColor;
        private boolean mDrawRedPointNum;
        private boolean mCircleMenuBg;
        private Bitmap mLogoRes;
        private int mDefaultLocation;
        private List<FloatItem> mFloatItems = new ArrayList<>();
        private Context mActivity;
        private FloatMenuView.OnMenuClickListener mOnMenuClickListener;
        private Drawable mDrawable;


        public Builder setBgDrawable(Drawable drawable) {
            mDrawable = drawable;
            return this;
        }

        public Builder() {
        }

        public Builder setFloatItems(List<FloatItem> mFloatItems) {
            this.mFloatItems = mFloatItems;
            return this;
        }

        public Builder addFloatItem(FloatItem floatItem) {
            this.mFloatItems.add(floatItem);
            return this;
        }

        public Builder backMenuColor(int val) {
            mBackMenuColor = val;
            return this;
        }

        public Builder drawRedPointNum(boolean val) {
            mDrawRedPointNum = val;
            return this;
        }

        public Builder drawCicleMenuBg(boolean val) {
            mCircleMenuBg = val;
            return this;
        }

        public Builder logo(Bitmap val) {
            mLogoRes = val;
            return this;
        }

        public Builder withActivity(Activity val) {
            mActivity = val;
            return this;
        }

        public Builder withContext(Context val) {
            mActivity = val;
            return this;
        }

        public Builder setOnMenuItemClickListener(FloatMenuView.OnMenuClickListener val) {
            mOnMenuClickListener = val;
            return this;
        }

        public Builder defaultLocation(int val) {
            mDefaultLocation = val;
            return this;
        }

        public FloatLogoMenu showWithListener(FloatMenuView.OnMenuClickListener val) {
            mOnMenuClickListener = val;
            return new FloatLogoMenu(this);
        }

        public FloatLogoMenu showWithLogo(Bitmap val) {
            mLogoRes = val;
            return new FloatLogoMenu(this);
        }

        public FloatLogoMenu show() {
            return new FloatLogoMenu(this);
        }
    }


}
