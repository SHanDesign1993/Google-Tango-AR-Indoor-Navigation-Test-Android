package  trendmicro.com.tangoindoornavigation.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by hugo on 28/07/2017.
 */
public class BasicDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tango.db";
    private static final int DATABASE_VERSION = 1;

    public static final int TABLE_VERSION_1 = 1 ; //init

    public interface Table {
        void onCreate(SQLiteDatabase db);
        void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    }

    private static BasicDbHelper mInstance = null;

    public static synchronized BasicDbHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BasicDbHelper(context.getApplicationContext(), DATABASE_NAME,
                    null, DATABASE_VERSION);
        }
        return mInstance;
    }

    public BasicDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (Table table : TableRegistration.getTables()) {
            table.onCreate(db);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (Table table : TableRegistration.getTables())
            table.onUpgrade(db, oldVersion, newVersion);
    }

}

