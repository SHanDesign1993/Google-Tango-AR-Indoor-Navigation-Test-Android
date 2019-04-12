package trendmicro.com.tangoindoornavigation.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;

/**
 * Created by hugo on 24/08/2017.
 */
public class ADFDao {

    private static final String TAG = ADFDao.class.getSimpleName();

    public static final String TABLE_NAME = "ADFs";


    public static class Columns {
        static final String ID = "id";
        public static final String ADF_UUID = "adf_uuid";
        public static final String BUILDING_ID = "building_id";
        public static final String ADF_NAME = "name";
        public static final String FLOOR = "floor";
    }

    public static class Table implements BasicDbHelper.Table {

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Columns.ADF_UUID + " TEXT NOT NULL, "
                    + Columns.BUILDING_ID + " INTEGER NOT NULL, "
                    + Columns.ADF_NAME + " TEXT NOT NULL, "
                    + Columns.FLOOR + " INTEGER NOT NULL );");
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

    public ADFDao(Context context) {
        mDbHelper = BasicDbHelper.getInstance(context);
    }

    public synchronized void addADF(ADFInfo item) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
            db.insert(TABLE_NAME, null, combineInsertContent(item));
        } catch (Exception ex) {
            Log.e(TAG, "addADF fail. ADFInfo = " + item.toString() + ", ex = " + ex.toString());
        }
    }

    public synchronized int updateADFByItem(ADFInfo item) {
        int result = 0;
        if (null == item) {
            return result;
        }

        try {
            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            ContentValues contentValues = new ContentValues();
            contentValues.put(Columns.ADF_UUID , item.getUUID());
            contentValues.put(Columns.BUILDING_ID, item.getBuildingId());
            contentValues.put(Columns.ADF_NAME, item.getADFName());
            contentValues.put(Columns.FLOOR, item.getFloor());

            String whereClause = Columns.ID + "=?";
            String[] whereArgs = { String.valueOf(item.getId()) };

            result = database.update(TABLE_NAME, contentValues, whereClause, whereArgs);

        } catch (Exception ex) {
            Log.e(TAG, "updateADFByItem fail. ex = " + ex.toString());
        }
        return result;
    }

    public synchronized List<ADFInfo> queryAllADFs() {
        List<ADFInfo> adfList = new ArrayList<ADFInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.ADF_UUID, Columns.BUILDING_ID, Columns.ADF_NAME, Columns.FLOOR };
        String selection = "";
        String[] args = new String[]{};

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        ADFInfo adf = new ADFInfo();

                        adf.setId(cursor.getString(0));
                        adf.setUUID(cursor.getString(1));
                        adf.setBuildingId(cursor.getInt(2));
                        adf.setADFName(cursor.getString(3));
                        adf.setFloor(cursor.getInt(4));
                        adfList.add(adf);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryAllADFs fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return adfList;
    }

    public synchronized List<ADFInfo> queryADFsByBuildingId(int buildingId) {
        List<ADFInfo> adfList = new ArrayList<ADFInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.ADF_UUID, Columns.BUILDING_ID, Columns.ADF_NAME, Columns.FLOOR };
        String selection = Columns.BUILDING_ID + "=?";
        String[] args = new String[] { String.valueOf(buildingId) };

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        ADFInfo adf = new ADFInfo();

                        adf.setId(cursor.getString(0));
                        adf.setUUID(cursor.getString(1));
                        adf.setBuildingId(cursor.getInt(2));
                        adf.setADFName(cursor.getString(3));
                        adf.setFloor(cursor.getInt(4));
                        adfList.add(adf);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryADFsByBuildingId fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return adfList;
    }

    public synchronized ADFInfo queryADFByUUID(String uuid) {
        if (uuid == null)
            return null;

        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.ADF_UUID, Columns.BUILDING_ID, Columns.ADF_NAME, Columns.FLOOR };
        String selection = Columns.ADF_UUID + "=?";
        String[] args = new String[] { uuid };
        ADFInfo adf = null;
        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null,
                    null, null);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    adf = new ADFInfo();

                    adf.setId(cursor.getString(0));
                    adf.setUUID(cursor.getString(1));
                    adf.setBuildingId(cursor.getInt(2));
                    adf.setADFName(cursor.getString(3));
                    adf.setFloor(cursor.getInt(4));
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryADFByUUID fail. uuid = " + uuid + ", ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return adf;
    }


    public synchronized int removeAllADFs() {
        int result = 0;
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        String selection = "";
        String[] args = new String[]{};

        try {
            result = database.delete(TABLE_NAME, selection, args);

        } catch (Exception ex) {
            Log.e(TAG, "removeAllADFs fail, ex = " + ex.toString());
        }
        return result;
    }

    public synchronized int removeADFByUUID(String uuid) {
        int result = 0;
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        String selection = Columns.ADF_UUID + "=?";
        String[] args = new String[]{uuid };

        try {
            result = database.delete(TABLE_NAME, selection, args);

        } catch (Exception ex) {
            Log.e(TAG, "removeADFByUUID fail, ex = " + ex.toString());
        }
        return result;
    }

    public synchronized int removeADFByBuildingId(int buildingId) {
        int result = 0;
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        String selection = Columns.BUILDING_ID + "=?";
        String[] args = new String[] { String.valueOf(buildingId) };

        try {
            result = database.delete(TABLE_NAME, selection, args);

        } catch (Exception ex) {
            Log.e(TAG, "removeADFByBuildingId fail, ex = " + ex.toString());
        }
        return result;
    }

    private ContentValues combineInsertContent(ADFInfo item) {
        ContentValues contentValues = new ContentValues();

        if (item != null) {
            contentValues.put(Columns.ADF_UUID, item.getUUID());
            contentValues.put(Columns.BUILDING_ID, item.getBuildingId());
            contentValues.put(Columns.ADF_NAME, item.getADFName());
            contentValues.put(Columns.FLOOR, item.getFloor());
        }

        return contentValues;
    }
}
