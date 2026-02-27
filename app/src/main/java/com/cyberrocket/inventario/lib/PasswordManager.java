package com.cyberrocket.inventario.lib;

import com.cyberrocket.inventario.models.SenhaItem;
import java.util.ArrayList;

public class PasswordManager {

    // Legacy JSON parser/builder for Notepad
    // Replaced by Knowledge Base implementation directly in ScannerActivity
    
    public static ArrayList<SenhaItem> parseNotepadJson(String content) {
        return new ArrayList<>();
    }

    public static String buildNotepadJson(ArrayList<SenhaItem> lista) {
        return "{}";
    }
}
