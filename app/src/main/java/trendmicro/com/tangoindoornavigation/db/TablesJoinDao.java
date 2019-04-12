package trendmicro.com.tangoindoornavigation.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import com.vividsolutions.jts.geom.Coordinate;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;

import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointWithADFInfo;

/**
 * Created by hugo on 29/08/2017.
 */

public class TablesJoinDao {

    private static final String TAG = TablesJoinDao.class.getSimpleName();

    private final BasicDbHelper mDbHelper;

    public TablesJoinDao(Context context) {
        mDbHelper = BasicDbHelper.getInstance(context);
    }

    public synchronized List<PointWithADFInfo> queryAllNamingPointsWithADFInfoByUUID(String uuid) {
        List<PointWithADFInfo> pointWithADFList = new ArrayList<PointWithADFInfo>();
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor = null;

        final String query = "SELECT p." + PointDao.Columns.ID + ", p." + PointDao.Columns.POINT_NAME + ", p." + PointDao.Columns.ADF_UUID + ", p." + PointDao.Columns.TYPE
                                + ", p." + PointDao.Columns.COORDINATE_X + ", p." + PointDao.Columns.COORDINATE_Y + ", p." + PointDao.Columns.COORDINATE_Z
                                + ", p." + PointDao.Columns.QUATERNION_W + ", p." + PointDao.Columns.QUATERNION_X + ", p." + PointDao.Columns.QUATERNION_Y
                                + ", p." + PointDao.Columns.QUATERNION_Z + ", p." + PointDao.Columns.GRID_VECTOR_X + ", p." + PointDao.Columns.GRID_VECTOR_Y
                                + ", a." + ADFDao.Columns.ID + ", a." + ADFDao.Columns.ADF_UUID + ", a." + ADFDao.Columns.BUILDING_ID + ", a." + ADFDao.Columns.ADF_NAME + ", a." + ADFDao.Columns.FLOOR
                                + " FROM " + PointDao.TABLE_NAME + " p LEFT JOIN " + ADFDao.TABLE_NAME + " a ON p." + PointDao.Columns.ADF_UUID + "=a." + ADFDao.Columns.ADF_UUID
                                + " WHERE p." + PointDao.Columns.POINT_NAME + "!=? AND p." + PointDao.Columns.ADF_UUID + "=?";

        String[] args = new String[] { String.valueOf(""), uuid };

        PointWithADFInfo pointWithADFInfo = null;
        ADFInfo adf = null;
        PointInfo point = null;
        Coordinate coordinate = null;
        Quaternion quaternion = null;
        Vector2 vector = null;

        try {
            cursor = database.rawQuery(query, args);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        pointWithADFInfo = new PointWithADFInfo();
                        point = new PointInfo();
                        adf = new ADFInfo();
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

                        adf.setId(cursor.getString(13));
                        adf.setUUID(cursor.getString(14));
                        adf.setBuildingId(cursor.getInt(15));
                        adf.setADFName(cursor.getString(16));
                        adf.setFloor(cursor.getInt(17));

                        pointWithADFInfo.setPointInfo(point);
                        pointWithADFInfo.setADFInfo(adf);
                        pointWithADFList.add(pointWithADFInfo);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "queryAllNamingPointsWithADFInfoByUUID fail, ex = " + ex.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return pointWithADFList;
    }
}
