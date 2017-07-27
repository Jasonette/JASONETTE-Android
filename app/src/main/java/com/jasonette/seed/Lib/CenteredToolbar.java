package com.jasonette.seed.Lib;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by realitix on 27/07/17.
 */

public class CenteredToolbar extends Toolbar {
    private TextView titleView;

    public CenteredToolbar(Context context) {
        super(context);
    }

    public CenteredToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CenteredToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (titleView != null) {
            titleView.setX((getWidth() - titleView.getWidth()) / 2);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (title.length() <= 0) {
            if (titleView != null && titleView.getParent() == this) {
                removeView(titleView);
            }
            return;
        }

        if (titleView == null) {
            titleView = new TextView(getContext());
        }

        if ( titleView.getParent() != this ) {
            addView(titleView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }

        titleView.setText(title);
    }

    @Override
    public void setTitleTextColor(int color) {
        titleView.setTextColor(color);
    }

    public void setTitleSize(float size) {
        titleView.setTextSize(size);
    }

    public void setTitleTypeface(Typeface font) {
        titleView.setTypeface(font);
    }
}
