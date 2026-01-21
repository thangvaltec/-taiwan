package mcv.testfacepass.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseHelper {
    private DBHelper dbHelper;

    public DatabaseHelper(Context context) {
        dbHelper = new DBHelper(context);
    }

    /**
     * 添加记录
     */
    public boolean add(String faceId, String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.FIELD_FACE_ID, faceId);
        values.put(DBHelper.FIELD_NAME, name);
        long result = db.insert(DBHelper.TABLE_NAME, null, values);
        db.close();
        return result != -1;
    }

    /**
     * 删除记录
     */
    public boolean delete(String faceId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete(DBHelper.TABLE_NAME,
                DBHelper.FIELD_FACE_ID + "=?", new String[]{faceId});
        db.close();
        return result > 0;
    }

    /**
     * 根据faceId查找name
     */
    public String findName(String faceId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_NAME,
                new String[]{DBHelper.FIELD_NAME},
                DBHelper.FIELD_FACE_ID + "=?",
                new String[]{faceId}, null, null, null);

        String name = null;
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(0);
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return name;
    }
}