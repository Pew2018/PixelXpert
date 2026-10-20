package dev.pew2018.pixelcloneprofilehelper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.core.view.ViewCompat;

/** Only this small block relayouts each frame. Reversal starts at the current rendered geometry. */
final class DisclosureMotion {
    private final View details;
    private final ImageView arrow;
    private ValueAnimator animator;
    private boolean expanded;
    private int targetHeight, lastWidth;
    private final View.OnLayoutChangeListener resize;

    DisclosureMotion(View details, ImageView arrow) {
        this.details = details; this.arrow = arrow;
        resize = (v, l, t, r, b, ol, ot, or, ob) -> {
            int width = r - l;
            if (width != lastWidth) { lastWidth = width; finish(); }
        };
        ((View) details.getParent()).addOnLayoutChangeListener(resize);
    }
    private int naturalHeight() {
        ViewGroup parent = (ViewGroup) details.getParent();
        int width = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        ViewGroup.LayoutParams params = details.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
            width -= margins.leftMargin + margins.rightMargin;
        }
        details.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, width), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return details.getMeasuredHeight();
    }
    void setExpanded(boolean value, boolean animate) {
        int startHeight = details.getVisibility() == View.GONE ? 0 : details.getHeight();
        float startAlpha = details.getVisibility() == View.GONE ? 0f : details.getAlpha();
        float startRotation = arrow.getRotation();
        cancel();
        expanded = value;
        ViewCompat.setStateDescription((View) arrow.getParent(), value ? "已展开" : "已收起");
        details.setImportantForAccessibility(value ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if (!value) details.clearFocus();
        if (!animate || !ClassicMotion.enabled(details.getContext()) || !ViewCompat.isLaidOut((View) details.getParent())) {
            finish(); return;
        }
        targetHeight = value ? naturalHeight() : 0;
        details.setVisibility(View.VISIBLE);
        setHeight(startHeight);
        details.setAlpha(startAlpha);
        ValueAnimator next = ValueAnimator.ofFloat(0f, 1f);
        animator = next;
        next.setDuration(ClassicMotion.duration(details.getContext(), R.integer.pcph_motion_duration));
        next.setInterpolator(ClassicMotion.EASING);
        next.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            setHeight(Math.round(startHeight + (targetHeight - startHeight) * f));
            details.setAlpha(startAlpha + ((value ? 1f : 0f) - startAlpha) * f);
            arrow.setRotation(startRotation + ((value ? 180f : 0f) - startRotation) * f);
        });
        next.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (animator == animation) { animator = null; finish(); }
            }
        });
        next.start();
    }
    void contentChanged() {
        if (animator != null && expanded && naturalHeight() != targetHeight) setExpanded(true, true);
    }
    private void setHeight(int height) {
        ViewGroup.LayoutParams params = details.getLayoutParams();
        params.height = height; details.setLayoutParams(params);
    }
    private void cancel() {
        if (animator == null) return;
        ValueAnimator previous = animator; animator = null;
        previous.removeAllListeners(); previous.removeAllUpdateListeners(); previous.cancel();
    }
    void finish() {
        cancel();
        details.setVisibility(expanded ? View.VISIBLE : View.GONE);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        details.setAlpha(1f); arrow.setRotation(expanded ? 180f : 0f);
    }
    void dispose() { finish(); ((View) details.getParent()).removeOnLayoutChangeListener(resize); }
}
