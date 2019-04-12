package trendmicro.com.tangoindoornavigation.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import trendmicro.com.tangoindoornavigation.db.dto.BuildingInfo;


/**
 * Created by hugo on 24/08/2017.
 */
public class BuildingDao {
    private static final String TAG = BuildingDao.class.getSimpleName();

    public static final String TABLE_NAME = "Buildings";


    public static class Columns {
        static final String ID = "id";
        public static final String BUILD_NAME = "name";
    }

    public static class Table implements BasicDbHelper.Table {

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Columns.BUILD_NAME + " TEXT NOT NULL );");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // sample code for db schema upgrade
            /*
            if (oldVersion <= BasicDbHelper.TABLE_VERSION_2) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN "
                        + Columns.IS_MALWARE + " INTEGER DEFAULT 0");
            }
            */
        }

    }

    private final BasicDbHelper mDbHelper;

    public BuildingDao(Context context) {
        mDbHelper = BasicDbHelper.getInstance(context);
    }

    public synchronized void addBuilding(BuildingInfo item) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
            db.insert(TABLE_NAME, null, combineInsertContent(item));
        } catch (Exception ex) {
            Log.e(TAG, "addBuilding fail. BuildingInfo = " + item.toString() + ", ex = " + ex.toString());
        }
    }

    public synchronized int updateBuildingByItem(BuildingInfo item) {
        int result = 0;
        if (null == item) {
            return result;
        }

        try {
            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            ContentValues contentValues = new ContentValues();
            contentValues.put(Columns.BUILD_NAME , item.getBuildingName());

            String whereClause = Columns.ID + "=?";
            String[] whereArgs = { String.valueOf(item.getId()) };

            result = database.update(TABLE_NAME, contentValues, whereClause, whereArgs);

        } catch (Exception ex) {
            Log.e(TAG, "updateBuildingByItem fail. ex = " + ex.toString());
        }
        return result;
    }

    public synchronized List<BuildingInfo> queryAllBuildings() {
        List<BuildingInfo> buildingList = new ArrayList<BuildingInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.BUILD_NAME };
        String selection = "";
        String[] args = new String[]{};

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        BuildingInfo building = new BuildingInfo();

                        building.setId(cursor.getString(0));
                        building.setBuildingName(cursor.getString(1));
                        buildingList.add(building);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryAllBuildings fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return buildingList;
    }


    public synchronized int removeAllBuildings() {
        int result = 0;
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        String selection = "";
        String[] args = new String[]{};

        try {
            result = database.delete(TABLE_NAME, selection, args);

        } catch (Exception ex) {
            Log.e(TAG, "removeAllBuildings fail, ex = " + ex.toString());
        }
        return result;
    }

    public synchronized int removeBuildingByID(int id) {
        int result = 0;
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        String selection = Columns.ID + "=?";
        String[] args = new String[]{String.valueOf(id) };

        try {
            result = database.delete(TABLE_NAME, selection, args);

        } catch (Exception ex) {
            Log.e(TAG, "removeBuildingByID fail, ex = " + ex.toString());
        }
        return result;
    }

    private ContentValues combineInsertContent(BuildingInfo item) {
        ContentValues contentValues = new ContentValues();

        if (item != null) {
            contentValues.put(Columns.BUILD_NAME, item.getBuildingName());
        }

        return contentValues;
    }

}
