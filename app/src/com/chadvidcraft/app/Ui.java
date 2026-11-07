package com.chadvidcraft.app;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BG = 0xFF0A0B14;
    static final int SURFACE = 0xFF141622;
    static final int SURFACE_2 = 0xFF1E2130;
    static final int STROKE = 0xFF2A2E42;
    static final int TEXT = 0xFFF2F4FA;
    static final int MUTED = 0xFF9AA1B8;
    static final int CYAN = 0xFF64E6FF;
    static final int INDIGO = 0xFF7C5CFF;
    static final int MAGENTA = 0xFFD946EF;
    static final int AMBER = 0xFFFBBF24;
    static final int EMERALD = 0xFF34D399;
    static final int ROSE = 0xFFFB7185;
    static final int INK = 0xFF0A0B14;

    private Ui() {
    }

    static int dp(Context c, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                c.getResources().getDisplayMetrics()));
    }

    static TextView text(Context c, String value, float sizeSp, int color, boolean bold) {
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        v.setTextColor(color);
        v.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        v.setLineSpacing(0f, 1.12f);
        return v;
    }

    static TextView caps(Context c, String value, int color) {
        TextView v = text(c, value.toUpperCase(), 10f, color, true);
        v.setLetterSpacing(0.14f);
        return v;
    }

    static LinearLayout column(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    static LinearLayout row(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    static GradientDrawable shape(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp);
        return d;
    }

    static GradientDrawable gradient(int start, int end, float radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{start, end});
        d.setCornerRadius(radiusDp);
        return d;
    }

    static LinearLayout card(Context c) {
        LinearLayout v = column(c);
        v.setBackground(shape(SURFACE, dp(c, 22)));
        v.setClipToOutline(true);
        v.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        ripple(v);
        return v;
    }

    static void pad(View v, int l, int t, int r, int b) {
        v.setPadding(dp(v.getContext(), l), dp(v.getContext(), t),
                dp(v.getContext(), r), dp(v.getContext(), b));
    }

    static LinearLayout.LayoutParams lparams(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    static LinearLayout.LayoutParams lparams(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    static void margin(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).setMargins(
                    dp(v.getContext(), l), dp(v.getContext(), t),
                    dp(v.getContext(), r), dp(v.getContext(), b));
        }
    }

    static TextView pill(Context c, String value, int bg, int fg) {
        TextView v = text(c, value, 10.5f, fg, true);
        v.setPadding(dp(c, 10), dp(c, 6), dp(c, 10), dp(c, 6));
        v.setBackground(shape(bg, 100));
        v.setGravity(Gravity.CENTER);
        return v;
    }

    static TextView button(Context c, String value, int bg, int fg) {
        TextView v = text(c, value, 14f, fg, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 18), dp(c, 13), dp(c, 18), dp(c, 13));
        v.setBackground(shape(bg, 16));
        v.setClickable(true);
        v.setFocusable(true);
        ripple(v);
        return v;
    }

    static TextView outlineButton(Context c, String value, int fg) {
        TextView v = text(c, value, 13f, fg, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.TRANSPARENT);
        d.setCornerRadius(dp(c, 16));
        d.setStroke(dp(c, 1), STROKE);
        v.setBackground(d);
        v.setClickable(true);
        ripple(v);
        return v;
    }

    static ImageView icon(Context c, int res, int tint, int sizeDp) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.setImageTintList(ColorStateList.valueOf(tint));
        int s = dp(c, sizeDp);
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }

    static ImageView iconTile(Context c, int res, int tint, int bg, int sizeDp) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.setImageTintList(ColorStateList.valueOf(tint));
        v.setBackground(shape(bg, dp(c, 13)));
        int s = dp(c, sizeDp);
        v.setPadding(dp(c, sizeDp * 0.26f), dp(c, sizeDp * 0.26f),
                dp(c, sizeDp * 0.26f), dp(c, sizeDp * 0.26f));
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }

    static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0x1AFFFFFF);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1)));
        return v;
    }

    static void ripple(View v) {
        if (!v.isClickable()) v.setClickable(true);
        android.graphics.drawable.Drawable bg = v.getBackground();
        v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), bg, null));
    }

    static void styleChip(TextView v, boolean selected, int accent) {
        if (selected) {
            v.setBackground(shape(accent, 100));
            v.setTextColor(INK);
        } else {
            v.setBackground(shape(SURFACE_2, 100));
            v.setTextColor(MUTED);
        }
    }

    static TextView chip(Context c, String value) {
        TextView v = text(c, value, 12f, MUTED, true);
        v.setPadding(dp(c, 14), dp(c, 9), dp(c, 14), dp(c, 9));
        v.setBackground(shape(SURFACE_2, 100));
        v.setGravity(Gravity.CENTER);
        v.setClickable(true);
        ripple(v);
        return v;
    }

    static void sectionHeader(LinearLayout parent, Context c, String title, String action,
                              Runnable onAction) {
        LinearLayout header = row(c);
        TextView t = text(c, title, 19f, TEXT, true);
        header.addView(t, lparams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (action != null) {
            TextView a = text(c, action, 12f, CYAN, true);
            a.setPadding(dp(c, 8), dp(c, 6), 0, dp(c, 6));
            a.setClickable(true);
            a.setOnClickListener(v -> onAction.run());
            header.addView(a);
        }
        parent.addView(header);
    }

    static int statusColor(String status) {
        if ("Rendering".equals(status) || "In progress".equals(status)) return AMBER;
        if ("Failed".equals(status) || "Cancelled".equals(status)) return ROSE;
        return EMERALD;
    }

    static int withAlpha(int base, int overlay) {
        int a = (overlay >>> 24) & 0xFF;
        int r = ((base >>> 16) & 0xFF) * a / 255 + ((overlay >>> 16) & 0xFF) * (255 - a) / 255;
        int g = ((base >>> 8) & 0xFF) * a / 255 + ((overlay >>> 8) & 0xFF) * (255 - a) / 255;
        int b = (base & 0xFF) * a / 255 + (overlay & 0xFF) * (255 - a) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
