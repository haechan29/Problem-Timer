package kr.co.problemtimertest.impl;

import android.view.View;
import android.view.animation.Animation;
import android.widget.LinearLayout;

public class AnimationListenerImpl implements Animation.AnimationListener {

    boolean isPageOpen;
    LinearLayout recordTable;

    public AnimationListenerImpl(boolean isPageOpen, LinearLayout recordTable) {
        this.isPageOpen = isPageOpen;
        this.recordTable = recordTable;
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        // 페이지가 열려 있는 경우
        if (isPageOpen) {
            recordTable.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {}

    @Override
    public void onAnimationRepeat(Animation animation) {}
}
