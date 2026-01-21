package mcv.testfacepass.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "face.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_NAME = "face";
    public static final String FIELD_FACE_ID = "face_id";
    public static final String FIELD_NAME = "name";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME + "(" +
                FIELD_FACE_ID + " TEXT PRIMARY KEY, " +
                FIELD_NAME + " TEXT" +
                ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}