package com.example.mobileappdev.util;

import android.content.Context;
import android.content.res.Resources;
import android.widget.TextView;

import com.example.mobileappdev.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public final class CapacityFormatter {
    private CapacityFormatter() { }

    /** Fills the ratio text, the bar and the pill. Same wording on every screen. */
    public static void bind(Context ctx, int occupied, int capacity,
                            TextView ratio, TextView pill, LinearProgressIndicator bar) {
        int left = Math.max(0, capacity - occupied);
        Resources res = ctx.getResources();

        if (ratio != null) {
            ratio.setText(ctx.getString(R.string.capacity_format, occupied, capacity));
        }
        if (bar != null) {
            bar.setMax(Math.max(1, capacity));
            bar.setProgress(Math.min(occupied, capacity));
        }
        if (pill != null) {
            if (left == 0) {
                pill.setText(R.string.capacity_full);
            } else if (left <= 2) {
                pill.setText(res.getQuantityString(R.plurals.capacity_near_full, left, left));
            } else {
                pill.setText(res.getQuantityString(R.plurals.places_remaining, left, left));
            }
        }
    }
}
