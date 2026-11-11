package com.example.electricbillestimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

/** Small shared helpers: formatting, input checks, edge-to-edge insets and animations. */
public final class Ui {

    public static final String[] MONTHS = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    private Ui() {
    }

    // ---------- formatting ----------

    public static String money(double value) {
        return String.format(Locale.US, "RM %,.2f", value);
    }

    public static String money3(double value) {
        return String.format(Locale.US, "RM %,.3f", value);
    }

    // ---------- input checks ----------

    /** Returns 0 when the text is a valid unit count, otherwise the string resource describing the problem. */
    public static int unitsError(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return R.string.err_units_empty;
        if (s.startsWith("-")) return R.string.err_units_negative;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return R.string.err_units_invalid;
        }
        if (s.length() > 7) return R.string.err_units_too_high;
        if (Integer.parseInt(s) > BillCalculator.MAX_UNITS) return R.string.err_units_too_high;
        return 0;
    }

    // ---------- screen setup ----------

    /** Keeps content clear of the status bar, navigation bar, display cut-outs and the keyboard. */
    public static void applySystemBarInsets(@NonNull View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
    }

    /** Sets the title and a working back arrow on the screen's toolbar. */
    public static void setupToolbar(@NonNull AppCompatActivity activity, int titleRes) {
        MaterialToolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.setTitle(titleRes);
        toolbar.setNavigationOnClickListener(v -> activity.getOnBackPressedDispatcher().onBackPressed());
    }

    public static void hideKeyboard(@NonNull View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    // ---------- animations ----------

    /** Animates a money value from one number to another. */
    public static void countUp(@NonNull final TextView tv, double from, final double to) {
        if (tv.getTag() instanceof ValueAnimator) ((ValueAnimator) tv.getTag()).cancel();
        final double start = from;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(750);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            tv.setText(money(start + (to - start) * f));
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tv.setText(money(to));
            }
        });
        tv.setTag(animator);
        animator.start();
    }

    /** Fades and slides a view up into place. */
    public static void popIn(@NonNull View v, long delayMs) {
        v.setAlpha(0f);
        v.setTranslationY(48f);
        v.animate().alpha(1f).translationY(0f).setStartDelay(delayMs).setDuration(500)
                .setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    /** Quick horizontal shake, used when input is rejected. */
    public static void shake(@NonNull View v) {
        v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.shake));
    }

    /** Small "pulse" to draw attention to a view that just changed. */
    public static void pulse(@NonNull View v) {
        v.animate().cancel();
        v.setScaleX(1f);
        v.setScaleY(1f);
        v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(140)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(220).start()).start();
    }
}
