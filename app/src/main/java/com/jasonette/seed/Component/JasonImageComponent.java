package com.jasonette.seed.Component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.jasonette.seed.Helper.JasonHelper;
import org.json.JSONObject;

public class JasonImageComponent {

    public static View build(View view, final JSONObject component, final JSONObject parent, final Context context) {
        if(view == null){
            try {
                ImageView imageview;
                imageview = new ImageView(context);
                imageview.setAdjustViewBounds(true);
                return imageview;
            } catch (Exception e) {
                Log.d("Error", e.toString());
                return new View(context);
            }
        } else {
            JasonComponent.build(view, component, parent, context);

            int width = 0;
            int height = 0;
            float corner_radius = 0;
            try {
                String type;
                final JSONObject style = JasonHelper.style(component, context);
                type = component.getString("type");
                String url = component.getString("url");

                if (style.has("corner_radius")) {
                    corner_radius = JasonHelper.pixels(context, style.getString("corner_radius"), "horizontal");
                }

                if (corner_radius == 0) {
                    try {
                        if (url.matches("\\.gif")) {
                            Glide
                                    .with(context)
                                    .load(url)
                                    .asGif()
                                    .into((ImageView)view);
                        } else {
                            if(style.has("color")){
                                Glide
                                        .with(context)
                                        .load(url)
                                        .asBitmap()
                                        .fitCenter()
                                        .into(new BitmapImageViewTarget((ImageView)view) {
                                            @Override
                                            protected void setResource(Bitmap res) {
                                                BitmapDrawable d = new BitmapDrawable(context.getResources(), res);
                                                try {
                                                    Drawable wrapper = DrawableCompat.wrap(d);
                                                    DrawableCompat.setTint(wrapper, JasonHelper.parse_color(style.getString("color")));
                                                    view.setImageDrawable(wrapper);
                                                } catch (Exception e) {
                                                    Log.d("Error", e.toString());
                                                    view.setImageDrawable(d);
                                                }
                                            }
                                        });
                            } else {

                                Glide
                                        .with(context)
                                        .load(url)
                                        .into((ImageView)view);
                            }

                        }
                    } catch (Exception e) {
                        Log.d("Error", e.toString());
                    }
                } else {
                    final float corner_radius_float = (float)corner_radius;
                    if (url.length() > 0) {
                        try {
                            Glide
                                    .with(context)
                                    .load(url)
                                    .asBitmap()
                                    .fitCenter()
                                    .into(new BitmapImageViewTarget((ImageView)view) {
                                        @Override
                                        protected void setResource(Bitmap res) {
                                            RoundedBitmapDrawable bitmapDrawable =
                                                    RoundedBitmapDrawableFactory.create(context.getResources(), res);
                                            bitmapDrawable.setCornerRadius(corner_radius_float);
                                            view.setImageDrawable(bitmapDrawable);
                                        }
                                    });
                        } catch (Exception e) {
                            Log.d("Error", e.toString());
                        }
                    }
                }


                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)view.getLayoutParams();
                if (!style.has("height")) {
                    params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                }
                if (!style.has("width")) {
                    params.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                }



                JasonComponent.addListener(view, context);
                view.requestLayout();
                return view;
            } catch (Exception e){
                Log.d("Error", e.toString());
            }
            return new View(context);
        }

    }
}
