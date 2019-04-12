package  trendmicro.com.tangoindoornavigation.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.vividsolutions.jts.geom.Coordinate;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;

import java.util.ArrayList;
import java.util.List;

import  trendmicro.com.tangoindoornavigation.db.dto.PointInfo;

/**
 * Created by hugo on 28/07/2017.
 */
public class PointDao {
    private static final String TAG = PointDao.class.getSimpleName();

    public static final String TABLE_NAME = "Points";


    public static class Columns {
        static final String ID = "id";
        public static final String POINT_NAME = "name";
        public static final String ADF_UUID = "adf_uuid";
        public static final String TYPE = "type";
        public static final String COORDINATE_X = "coordinate_x";
        public static final String COORDINATE_Y = "coordinate_y";
        public static final String COORDINATE_Z = "coordinate_z";
        public static final String QUATERNION_W = "quaternion_w";
        public static final String QUATERNION_X = "quaternion_x";
        public static final String QUATERNION_Y = "quaternion_y";
        public static final String QUATERNION_Z = "quaternion_z";
        public static final String GRID_VECTOR_X = "grid_vector_x";
        public static final String GRID_VECTOR_Y = "grid_vector_y";
    }

    public static class Table implements BasicDbHelper.Table {

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                    + Columns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + Columns.POINT_NAME + " TEXT NOT NULL, "
                    + Columns.ADF_UUID + " TEXT NOT NULL, "
                    + Columns.TYPE + " INTEGER NOT NULL, " // 0 : general, 1 : naming, 2 : stairs, 3 : elevator
                    + Columns.COORDINATE_X + " REAL NOT NULL, "
                    + Columns.COORDINATE_Y + " REAL NOT NULL, "
                    + Columns.COORDINATE_Z + " REAL NOT NULL, "
                    + Columns.QUATERNION_W + " REAL NOT NULL, "
                    + Columns.QUATERNION_X + " REAL NOT NULL, "
                    + Columns.QUATERNION_Y + " REAL NOT NULL, "
                    + Columns.QUATERNION_Z + " REAL NOT NULL, "
                    + Columns.GRID_VECTOR_X + " REAL NOT NULL, "
                    + Columns.GRID_VECTOR_Y + " REAL NOT NULL );");
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

    public PointDao(Context context) {
        mDbHelper = BasicDbHelper.getInstance(context);
    }

    public synchronized void addPoint(PointInfo item) {

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        try {
            db.insert(TABLE_NAME, null, combineInsertContent(item));
        } catch (Exception ex) {
            Log.e(TAG, "addPoint fail. PointInfo = " + item.toString() + ", ex = " + ex.toString());
        }
    }

    public synchronized void updatePointByItem(PointInfo item) {

        if (null == item) {
            return;
        }

        try {
            SQLiteDatabase database = mDbHelper.getWritableDatabase();

            ContentValues contentValues = new ContentValues();
            contentValues.put(Columns.POINT_NAME , item.getPointName());
            contentValues.put(Columns.ADF_UUID, item.getUUID());
            contentValues.put(Columns.TYPE, item.getType().ordinal());
            contentValues.put(Columns.COORDINATE_X, item.getCoordinate().x);
            contentValues.put(Columns.COORDINATE_Y, item.getCoordinate().y);
            contentValues.put(Columns.COORDINATE_Z, item.getCoordinate().z);
            contentValues.put(Columns.QUATERNION_W, item.getQuaternion().w);
            contentValues.put(Columns.QUATERNION_X, item.getQuaternion().x);
            contentValues.put(Columns.QUATERNION_Y, item.getQuaternion().y);
            contentValues.put(Columns.QUATERNION_Z, item.getQuaternion().z);
            contentValues.put(Columns.GRID_VECTOR_X, item.getGridVector().getX());
            contentValues.put(Columns.GRID_VECTOR_Y, item.getGridVector().getY());

            String whereClause = Columns.ID + "=?";
            String[] whereArgs = { String.valueOf(item.getId()) };

            int result = database.update(TABLE_NAME, contentValues, whereClause, whereArgs);
            Log.e(TAG, "result = " + result);
        } catch (Exception ex) {
            Log.e(TAG, "updatePointByItem fail. ex = " + ex.toString());
        }
    }

    public synchronized List<PointInfo> queryAllPoints() {
        List<PointInfo> pointList = new ArrayList<PointInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                            Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };
        String selection = "";
        String[] args = new String[]{};
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        point = new PointInfo();
                        coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                        quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                        vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));

                        point.setId(cursor.getString(0));
                        point.setPointName(cursor.getString(1));
                        point.setUUID(cursor.getString(2));
                        point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                        point.setCoordinate(coordinate);
                        point.setQuaternion(quaternion);
                        point.setGridVector(vector);
                        pointList.add(point);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryAllPoints fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return pointList;
    }

    public synchronized List<PointInfo> queryAllPointsByUUID(String uuid) {
        List<PointInfo> pointList = new ArrayList<PointInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };
        String selection = Columns.ADF_UUID + "=?";
        String[] args = new String[] { uuid };
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        point = new PointInfo();
                        coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                        quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                        vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));

                        point.setId(cursor.getString(0));
                        point.setPointName(cursor.getString(1));
                        point.setUUID(cursor.getString(2));
                        point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                        point.setCoordinate(coordinate);
                        point.setQuaternion(quaternion);
                        point.setGridVector(vector);
                        pointList.add(point);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryAllPointsByUUID fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return pointList;
    }

    public synchronized List<PointInfo> queryAllNamingPoints() {
        List<PointInfo> pointList = new ArrayList<PointInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };
        String selection = Columns.POINT_NAME + "!=?";
        String[] args = new String[] { String.valueOf("") };
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        point = new PointInfo();
                        coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                        quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                        vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));

                        point.setId(cursor.getString(0));
                        point.setPointName(cursor.getString(1));
                        point.setUUID(cursor.getString(2));
                        point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                        point.setCoordinate(coordinate);
                        point.setQuaternion(quaternion);
                        point.setGridVector(vector);
                        pointList.add(point);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryAllNamingPoints fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return pointList;
    }

    public synchronized List<PointInfo> queryAllNamingPointsByUUID(String uuid) {
        List<PointInfo> pointList = new ArrayList<PointInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };
        String selection = Columns.POINT_NAME + "!=? AND " + Columns.ADF_UUID + "=?";
        String[] args = new String[] { String.valueOf(""), uuid };
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        point = new PointInfo();
                        coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                        quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                        vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));

                        point.setId(cursor.getString(0));
                        point.setPointName(cursor.getString(1));
                        point.setUUID(cursor.getString(2));
                        point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                        point.setCoordinate(coordinate);
                        point.setQuaternion(quaternion);
                        point.setGridVector(vector);
                        pointList.add(point);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryAllNamingPointsByUUID fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return pointList;
    }

    public synchronized List<PointInfo> queryAllStairAndElevatorPointsByUUID(String uuid) {
        List<PointInfo> pointList = new ArrayList<PointInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };
        String selection =  Columns.ADF_UUID + "=? AND (" + Columns.TYPE + "=? OR " + Columns.TYPE + "=?)";
        String[] args = new String[] { uuid, String.valueOf(PointInfo.PointType.STAIRS.ordinal()), String.valueOf(PointInfo.PointType.ELEVATOR.ordinal()) };
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        point = new PointInfo();
                        coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                        quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                        vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));

                        point.setId(cursor.getString(0));
                        point.setPointName(cursor.getString(1));
                        point.setUUID(cursor.getString(2));
                        point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                        point.setCoordinate(coordinate);
                        point.setQuaternion(quaternion);
                        point.setGridVector(vector);
                        pointList.add(point);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryAllStairAndElevatorPointsByUUID fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return pointList;
    }

    public synchronized int removeAllPoints() {
        int result = 0;
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        String selection = "";
        String[] args = new String[]{};

        try {
            result = database.delete(TABLE_NAME, selection, args);

        } catch (Exception ex) {
            Log.e(TAG, "removeAllPoints fail, ex = " + ex.toString());
        }
        return result;
    }

    public synchronized int removeAllPointsByUUID(String uuid) {
        int result = 0;
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        String selection = Columns.ADF_UUID + "=?";
        String[] args = new String[]{uuid};

        try {
            result = database.delete(TABLE_NAME, selection, args);

        } catch (Exception ex) {
            Log.e(TAG, "removeAllPoints fail, ex = " + ex.toString());
        }
        return result;
    }

    public synchronized int removePointByGridVectorAndUUID(Vector2 vector, String uuid) {
        int result = 0;
        if (vector != null) {
            SQLiteDatabase database = mDbHelper.getReadableDatabase();

            String selection = Columns.ADF_UUID + "=? AND " + Columns.GRID_VECTOR_X + "=? AND " + Columns.GRID_VECTOR_Y + "=?";
            String[] args = new String[]{uuid, String.valueOf(vector.getX()), String.valueOf(vector.getY())};

            try {
                result = database.delete(TABLE_NAME, selection, args);

            } catch (Exception ex) {
                Log.e(TAG, "removeAllPoints fail, ex = " + ex.toString());
            }
        }
        return result;
    }

    public synchronized PointInfo queryPointByID(String id) {
        if (id == null)
            return null;

        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };
        String selection = Columns.ID + "=?";
        String[] args = new String[] { id };
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null,
                    null, null);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    point = new PointInfo();
                    coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                    quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                    vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));

                    point.setId(cursor.getString(0));
                    point.setPointName(cursor.getString(1));
                    point.setUUID(cursor.getString(2));
                    point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                    point.setCoordinate(coordinate);
                    point.setQuaternion(quaternion);
                    point.setGridVector(vector);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryPointByID fail. id = " + id + ", ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return point;
    }


    public synchronized PointInfo queryPointByNameAndUUID(String pointName, String uuid) {
        if (pointName == null)
            return null;

        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };
        String selection = Columns.POINT_NAME + "=? AND " + Columns.ADF_UUID + "=?";
        String[] args = new String[] { pointName, uuid };
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null,
                    null, null);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    point = new PointInfo();
                    coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                    quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                    vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));

                    point.setId(cursor.getString(0));
                    point.setPointName(cursor.getString(1));
                    point.setUUID(cursor.getString(2));
                    point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                    point.setCoordinate(coordinate);
                    point.setQuaternion(quaternion);
                    point.setGridVector(vector);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryPointByNameAndUUID fail. pointName = " + pointName + ", ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return point;
    }

    public synchronized PointInfo queryPointByGridVectorAndUUID(Vector2 gridVector, String uuid) {
        if (gridVector == null)
            return null;

        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };

        String selection = Columns.ADF_UUID + "=? AND " + Columns.GRID_VECTOR_X + "=? AND " + Columns.GRID_VECTOR_Y + "=?";
        String[] args = new String[]{uuid, String.valueOf(gridVector.getX()), String.valueOf(gridVector.getY())};
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null,
                    null, null);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    point = new PointInfo();
                    coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                    quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                    vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));

                    point.setId(cursor.getString(0));
                    point.setPointName(cursor.getString(1));
                    point.setUUID(cursor.getString(2));
                    point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                    point.setCoordinate(coordinate);
                    point.setQuaternion(quaternion);
                    point.setGridVector(vector);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryPointByGridVectorAndUUID fail. gridVector = " + gridVector.getX() + ", " + gridVector.getY() + ", ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return point;
    }

    public synchronized PointInfo queryPointByCoordinateAndUUID(Coordinate condictionCoordinate, String uuid) {
        if (condictionCoordinate == null)
            return null;

        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        String[] columns = { Columns.ID, Columns.POINT_NAME, Columns.ADF_UUID,  Columns.TYPE, Columns.COORDINATE_X, Columns.COORDINATE_Y, Columns.COORDINATE_Z,
                Columns.QUATERNION_W, Columns.QUATERNION_X, Columns.QUATERNION_Y, Columns.QUATERNION_Z, Columns.GRID_VECTOR_X, Columns.GRID_VECTOR_Y };
        String selection = Columns.COORDINATE_X + "=? AND " + Columns.COORDINATE_Y + "=? AND " + Columns.COORDINATE_Z + "=? AND "+ Columns.ADF_UUID + "=?";
        String[] args = new String[] { String.valueOf(condictionCoordinate.x), String.valueOf(condictionCoordinate.y), String.valueOf(condictionCoordinate.z), uuid };
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.query(TABLE_NAME, columns, selection, args, null,
                    null, null);
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    point = new PointInfo();
                    coordinate = new Coordinate(cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6));
                    quaternion = new Quaternion(cursor.getDouble(7), cursor.getDouble(8), cursor.getDouble(9), cursor.getDouble(10));
                    vector = new Vector2(cursor.getDouble(11), cursor.getDouble(12));;

                    point.setId(cursor.getString(0));
                    point.setPointName(cursor.getString(1));
                    point.setUUID(cursor.getString(2));
                    point.setType(PointInfo.PointType.values()[cursor.getInt(3)]);
                    point.setCoordinate(coordinate);
                    point.setQuaternion(quaternion);
                    point.setGridVector(vector);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryPointByNameAndUUID fail. condictionCoordinate = " + condictionCoordinate.toString() + ", ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return point;
    }

    private ContentValues combineInsertContent(PointInfo item) {
        ContentValues contentValues = new ContentValues();

        if (item != null) {
            contentValues.put(Columns.POINT_NAME, item.getPointName());
            contentValues.put(Columns.ADF_UUID, item.getUUID());
            contentValues.put(Columns.TYPE, item.getType().ordinal());
            contentValues.put(Columns.COORDINATE_X, item.getCoordinate().x);
            contentValues.put(Columns.COORDINATE_Y, item.getCoordinate().y);
            contentValues.put(Columns.COORDINATE_Z, item.getCoordinate().z);
            contentValues.put(Columns.QUATERNION_W, item.getQuaternion().w);
            contentValues.put(Columns.QUATERNION_X, item.getQuaternion().x);
            contentValues.put(Columns.QUATERNION_Y, item.getQuaternion().y);
            contentValues.put(Columns.QUATERNION_Z, item.getQuaternion().z);
            contentValues.put(Columns.GRID_VECTOR_X, item.getGridVector().getX());
            contentValues.put(Columns.GRID_VECTOR_Y, item.getGridVector().getY());
        }

        return contentValues;
    }

}

