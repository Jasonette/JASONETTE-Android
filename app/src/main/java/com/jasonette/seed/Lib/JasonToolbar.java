package com.jasonette.seed.Lib;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.jasonette.seed.Component.JasonImageComponent;
import com.jasonette.seed.Helper.JasonHelper;

import org.json.JSONObject;

/**
 * Created by realitix on 27/07/17.
 */

public class JasonToolbar extends Toolbar {
    private TextView titleView;
    private ImageView logoView;
    private int alignment = -1;
    private int leftOffset;
    private int topOffset;
    private int imageWidth;
    private int imageHeight;

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
    public void setTitle(CharSequence title) {
        // remove image view before inserting title view
        if (logoView != null && logoView.getParent() == this) {
            removeView(logoView);
        }

        // remove title if empty
        if (title.length() <= 0) {
            if (titleView != null && titleView.getParent() == this) {
                removeView(titleView);
            }
            return;
        }

        // create title only on the first call
        if (titleView == null) {
            titleView = new TextView(getContext());
        }

        // insert into toolbar
        if (titleView.getParent() != this) {
            addView(titleView, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }

        // manage positioning
        Toolbar.LayoutParams params = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
        params.gravity = (alignment == -1) ? Gravity.LEFT : alignment;
        params.leftMargin = leftOffset;
        params.topMargin = topOffset;
        titleView.setLayoutParams(params);

        // set text
        titleView.setText(title);
    }

    public void setImage(JSONObject url) {
        // remove title view before inserting image view
        if (titleView != null && titleView.getParent() == this) {
            removeView(titleView);
        }

        // create the image view only on the first call
        if (logoView == null) {
            logoView = new ImageView(getContext());
        }

        // insert into toolbar
        if (logoView.getParent() != this) {
            addView(logoView);
        }

        // manage positioning
        Toolbar.LayoutParams params = new Toolbar.LayoutParams(imageWidth, imageHeight);
        params.gravity = (alignment == -1) ? Gravity.CENTER : alignment;
        params.leftMargin = leftOffset;
        params.topMargin = topOffset;
        logoView.setLayoutParams(params);

        // load image with glide
        Glide.with(getContext())
                .load(JasonImageComponent.resolve_url(url, getContext()))
                .into(logoView);
    }

    public void setTitleFont(JSONObject style) {
        JasonHelper.setTextViewFont(titleView, style, getContext());
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

    public void setTopOffset(int offset) {
        topOffset = offset;
    }

    public void setImageHeight(int height) {
        imageHeight = height;
    }

    public void setImageWidth(int width) {
        imageWidth = width;
    }
}
