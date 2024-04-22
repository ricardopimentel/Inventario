package com.cyberrocket.inventario.lib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class Crud {

    public Crud(){

    }

    public String SelectItem(Context contexto, String tabela, int id, int item){
        String resposta = "";
        CriarBanco mDbHelper = new CriarBanco(contexto);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String select = "select * from "+tabela+" where _ID="+id;
        Cursor c = db.rawQuery(select, null);
        while(c.moveToNext())        {
            resposta = (c.getString(item));
        }

        return resposta;
    }

    public Boolean UpdateItem(Context contexto, String tabela, int id, ContentValues values){
        CriarBanco mDbHelper = new CriarBanco(contexto);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String selection =  "_ID = ?";
        String[] selectionArgs = { id+"" };
        try{
            int count = db.update(tabela, values, selection, selectionArgs);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public Boolean InsertItem(Context contexto, String tabela, ContentValues values){
        CriarBanco mDbHelper = new CriarBanco(contexto);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
            long newRowId = db.insert(tabela, null, values);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
