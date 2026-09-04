package com.cyberrocket.inventario.lib;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class StatusBarHelper {

    public static void setupStatusBar(Activity activity, View rootContainer, View statusBarBg) {
        if (activity == null) return;

        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false); // Bright white icons/text on purple status bar
        }

        if (statusBarBg != null && rootContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
                Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                ViewGroup.LayoutParams lp = statusBarBg.getLayoutParams();
                if (lp != null && lp.height != statusBars.top) {
                    lp.height = statusBars.top;
                    statusBarBg.setLayoutParams(lp);
                }
                return insets;
            });
        }
    }
}
