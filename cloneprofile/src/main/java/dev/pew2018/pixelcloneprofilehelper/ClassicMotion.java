package dev.pew2018.pixelcloneprofilehelper;

import android.animation.ValueAnimator;
import android.content.Context;
import android.provider.Settings;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/** Shared motion policy; property animators themselves honor the system duration scale. */
final class ClassicMotion {
    static final Interpolator EASING = new PathInterpolator(.4f, 0f, .2f, 1f);
    private ClassicMotion() {}
    static boolean enabled(Context context) {
        return ValueAnimator.areAnimatorsEnabled() && Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) > 0f;
    }
    static int duration(Context context, int resource) { return context.getResources().getInteger(resource); }
}
