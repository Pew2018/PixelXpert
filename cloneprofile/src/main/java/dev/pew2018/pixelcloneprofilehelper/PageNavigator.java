package dev.pew2018.pixelcloneprofilehelper;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.view.ViewCompat;
import java.util.List;
import java.util.function.IntConsumer;

/** Two real, measured page views. No bitmap overlays or ViewFlipper visibility side effects. */
final class PageNavigator {
    static final class Host extends FrameLayout {
        boolean blocked;
        Host(Context context) { super(context); setClipChildren(true); }
        @Override public boolean dispatchTouchEvent(MotionEvent event) {
            return blocked || super.dispatchTouchEvent(event);
        }
        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            if (blocked && event.getKeyCode() != KeyEvent.KEYCODE_BACK) return true;
            return super.dispatchKeyEvent(event);
        }
    }
    private final Host host;
    private final List<? extends View> pages;
    private final IntConsumer changed;
    private ValueAnimator animator;
    private int current, from, to;
    private float direction, progress;
    private boolean seeking, initialized, disposed;

    PageNavigator(Host host, List<? extends View> pages, IntConsumer changed) {
        this.host = host; this.pages = pages; this.changed = changed;
    }
    static int parent(int page) { return page == 3 ? 2 : 0; }
    boolean busy() { return seeking || animator != null; }
    boolean seeking() { return seeking; }

    void show(int page, boolean animate) {
        if (disposed) return;
        settle();
        int next = page >= 0 && page < pages.size() ? page : 0;
        if (!initialized || next == current || !animate || !ClassicMotion.enabled(host.getContext())) {
            current = next; initialized = true; normalize(); changed.accept(current); return;
        }
        prepare(current, next, (next > current ? 1f : -1f) * layoutDirection());
        current = next; // A rapid toolbar/system back returns from this committed destination.
        changed.accept(current);
        animateTo(1f, false);
    }
    private float layoutDirection() {
        return ViewCompat.getLayoutDirection(host) == ViewCompat.LAYOUT_DIRECTION_RTL ? -1f : 1f;
    }
    private void prepare(int source, int destination, float sign) {
        from = source; to = destination; direction = sign; progress = 0f;
        host.blocked = true;
        for (View page : pages) {
            page.clearFocus();
            page.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        pages.get(from).setVisibility(View.VISIBLE);
        pages.get(to).setVisibility(View.VISIBLE);
        // Incoming above outgoing; complementary alpha reveals outgoing through the new background.
        pages.get(to).bringToFront();
        apply(0f);
    }
    private void apply(float fraction) {
        progress = Math.max(0f, Math.min(1f, fraction));
        float distance = host.getResources().getDimension(R.dimen.pcph_page_motion_distance) * direction;
        pages.get(from).setTranslationX(-distance * progress);
        pages.get(from).setAlpha(1f - progress);
        pages.get(to).setTranslationX(distance * (1f - progress));
        pages.get(to).setAlpha(progress);
    }
    boolean beginBack(boolean fromLeftEdge) {
        if (disposed) return false;
        settle();
        if (current == 0) return false;
        seeking = true;
        // Follow the actual swipe edge; ordinary button navigation uses logical RTL direction.
        prepare(current, parent(current), fromLeftEdge ? -1f : 1f);
        return true;
    }
    void progressBack(float fraction) {
        if (seeking) apply(ClassicMotion.enabled(host.getContext()) ? fraction : 0f);
    }
    void cancelBack() {
        if (!seeking) return;
        animateTo(0f, true);
    }
    boolean completeBack() {
        if (!seeking) return false;
        current = to;
        seeking = false;
        changed.accept(current);
        // Settle only the remaining distance; do not replay a second full navigation transition.
        animateTo(1f, false);
        return true;
    }
    private void animateTo(float target, boolean cancelGesture) {
        cancelAnimator();
        float start = progress;
        if (!ClassicMotion.enabled(host.getContext()) || Math.abs(target - start) < .001f) {
            if (cancelGesture) seeking = false;
            normalize(); return;
        }
        ValueAnimator next = ValueAnimator.ofFloat(start, target);
        animator = next;
        next.setDuration(Math.max(1, Math.round(ClassicMotion.duration(host.getContext(),
                R.integer.pcph_navigation_duration) * Math.abs(target - start))));
        next.setInterpolator(ClassicMotion.EASING);
        next.addUpdateListener(a -> apply((float) a.getAnimatedValue()));
        next.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (animator != animation) return;
                animator = null;
                if (cancelGesture) seeking = false;
                normalize();
            }
        });
        next.start();
    }
    private void cancelAnimator() {
        if (animator == null) return;
        ValueAnimator previous = animator; animator = null;
        previous.removeAllUpdateListeners(); previous.removeAllListeners(); previous.cancel();
    }
    private void normalize() {
        for (int i = 0; i < pages.size(); i++) {
            View page = pages.get(i);
            page.setAlpha(1f); page.setTranslationX(0f);
            // INVISIBLE keeps all pages measured, including the previous page used by a back gesture.
            page.setVisibility(i == current ? View.VISIBLE : View.INVISIBLE);
            page.setImportantForAccessibility(i == current ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                    : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        pages.get(current).bringToFront();
        host.blocked = false;
    }
    void settle() {
        cancelAnimator(); seeking = false;
        if (initialized) normalize();
    }
    void dispose() { settle(); disposed = true; }
}
