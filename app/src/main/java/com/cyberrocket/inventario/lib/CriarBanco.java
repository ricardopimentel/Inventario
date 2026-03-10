package com.cyberrocket.inventario.lib;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CriarBanco extends SQLiteOpenHelper {
    private static final String TABLE_NAME = "CONFIG";

    private static final String SQL_CREATE_CONFIG =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    "_ID INTEGER PRIMARY KEY," +
                    "URL TEXT, SESSION_TOKEN TEXT, USUARIO TEXT, SENHA TEXT )";

    private static final String SQL_DELETE_CONFIG =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "config.db";

    public CriarBanco(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_CONFIG);
        // Insere a linha inicial se não existir
        db.execSQL("INSERT OR IGNORE INTO " + TABLE_NAME + " (_ID, URL, SESSION_TOKEN) VALUES (1, '', '')");
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN USUARIO TEXT");
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN SENHA TEXT");
            } catch (Exception e) {
                // Se falhar (ex: colunas já existem), deleta e recria para garantir integridade
                db.execSQL(SQL_DELETE_CONFIG);
                onCreate(db);
            }
        }
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}