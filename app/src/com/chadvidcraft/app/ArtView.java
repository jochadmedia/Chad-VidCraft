package com.chadvidcraft.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

public final class ArtView extends View {
    private final int colorA;
    private final int colorB;
    private final boolean showPlay;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private ValueAnimator animator;
    private float phase;
    private boolean playing = true;

    public ArtView(Context context, int colorA, int colorB, boolean showPlay) {
        super(context);
        this.colorA = colorA;
        this.colorB = colorB;
        this.showPlay = showPlay;
    }

    public void setPlaying(boolean value) {
        playing = value;
        invalidate();
    }

    public boolean isPlaying() {
        return playing;
    }

    private void start() {
        if (!showPlay) return;
        if (animator != null && animator.isRunning()) return;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(8000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, w, h, colorA, colorB, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        float drift = (float) Math.sin(phase * Math.PI * 2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 1.2f));
        paint.setColor(0x33FFFFFF);
        float baseR = Math.min(w, h) * 0.55f;
        for (int i = 0; i < 4; i++) {
            canvas.drawCircle(w * 0.82f + drift * 10f, h * 0.18f,
                    baseR * (0.45f + i * 0.22f), paint);
        }

        paint.setStyle(Paint.Style.FILL);
        int bars = 22;
        float bw = Math.max(Ui.dp(getContext(), 2), w / (bars * 2.6f));
        float gap = (w - bars * bw) / (bars + 1f);
        for (int i = 0; i < bars; i++) {
            float wave = (float) Math.sin(phase * Math.PI * 2f + i * 0.55f);
            float bh = h * (0.08f + 0.14f * Math.abs(wave));
            float x = gap + i * (bw + gap);
            paint.setColor(i % 3 == 0 ? 0x66FFFFFF : 0x33FFFFFF);
            canvas.drawRoundRect(x, h - Ui.dp(getContext(), 18) - bh, x + bw,
                    h - Ui.dp(getContext(), 18), bw / 2f, bw / 2f, paint);
        }

        paint.setShader(new LinearGradient(0, h * 0.55f, 0, h,
                Color.TRANSPARENT, 0x99000000, Shader.TileMode.CLAMP));
        canvas.drawRect(0, h * 0.55f, w, h, paint);
        paint.setShader(null);

        if (showPlay) {
            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) * 0.15f;
            paint.setColor(0x33000000);
            canvas.drawCircle(cx, cy, r, paint);
            paint.setColor(0xE6FFFFFF);
            canvas.drawCircle(cx, cy, r * 0.92f, paint);
            paint.setColor(Ui.INK);
            if (playing) {
                float bw2 = r * 0.22f;
                float bh2 = r * 0.62f;
                canvas.drawRoundRect(cx - bw2 * 1.7f, cy - bh2 / 2f, cx - bw2 * 0.2f,
                        cy + bh2 / 2f, bw2 / 2f, bw2 / 2f, paint);
                canvas.drawRoundRect(cx + bw2 * 0.2f, cy - bh2 / 2f, cx + bw2 * 1.7f,
                        cy + bh2 / 2f, bw2 / 2f, bw2 / 2f, paint);
            } else {
                path.reset();
                path.moveTo(cx - r * 0.24f, cy - r * 0.34f);
                path.lineTo(cx + r * 0.38f, cy);
                path.lineTo(cx - r * 0.24f, cy + r * 0.34f);
                path.close();
                canvas.drawPath(path, paint);
            }
        }
    }
}
