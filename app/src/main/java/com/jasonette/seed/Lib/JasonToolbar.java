package com.jasonette.seed.Lib;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

/**
 * Created by realitix on 27/07/17.
 */

public class JasonToolbar extends Toolbar {
    private TextView titleView;
    private int alignment = Gravity.LEFT;
    private int leftOffset = 0;
    private int rightOffset = 0;
    private int topOffset = 0;

    public JasonToolbar(Context context) {
        super(context);
    }

    public JasonToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JasonToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (titleView != null) {
            int offset = 50;

            if (alignment == Gravity.CENTER) {
                titleView.setX((getWidth() - titleView.getWidth()) / 2);
            }
            else if (alignment == Gravity.RIGHT) {

                if (getMenu().size() != 0) {
                    offset = 150;
                }

                titleView.setX((getWidth() - titleView.getWidth()) - offset - rightOffset);
            }
            else {
                titleView.setX(offset + leftOffset); // LEFT
            }

            titleView.setTranslationY(topOffset);
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

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public void setLeftOffset(int offset) {
        leftOffset = offset;
    }

    public void setRightOffset(int offset) {
        this.rightOffset = offset;
    }

    public void setTopOffset(int offset) {
        this.topOffset = offset;
    }
}
