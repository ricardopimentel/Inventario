package com.cyberrocket.inventario;

import android.app.Application;
import com.cyberrocket.inventario.lib.ThemeUtils;

public class InventarioApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Applies the theme globally as soon as the app starts
        ThemeUtils.applyTheme(this);
    }
}
