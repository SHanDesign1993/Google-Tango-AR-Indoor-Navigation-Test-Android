package trendmicro.com.tangoindoornavigation.preference;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import trendmicro.com.tangoindoornavigation.db.ADFDao;
import trendmicro.com.tangoindoornavigation.db.PointDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointWithADFInfo;

/**
 * Created by hugo on 29/08/2017.
 */

public class SharedPreferenceHelper {

    private static final String TAG = SharedPreferenceHelper.class.getSimpleName();

    public static void setEndPointInfo(Context context, PointWithADFInfo pointWithADFInfo) {
        if (pointWithADFInfo != null) {
            Log.i(TAG, "[setEndPointInfo]pointWithADFInfo = " + pointWithADFInfo.toString());

            ADFInfo adf = pointWithADFInfo.getADFInfo();
            PointInfo point = pointWithADFInfo.getPointInfo();

            String endPointInfoStr = adf.getUUID() + ";" + point.getId();
            NavigationSharedPreference.setEndPointInfoSet(context, endPointInfoStr);
        } else {
            Log.i(TAG, "[setEndPointInfo]pointWithADFInfo = null");
            NavigationSharedPreference.setEndPointInfoSet(context, "");
        }
    }

    public static PointWithADFInfo getEndPointInfo(Context context) {

        String endPointInfoStr = NavigationSharedPreference.getEndPointInfoSet(context);
        if (!endPointInfoStr.equals("")) {
            Log.i(TAG, "[getEndPointInfo]endPointInfoSet is not null");
            PointWithADFInfo pointWithADFInfo = new PointWithADFInfo();

            String[] endPointInfo = endPointInfoStr.split(";");
            ADFDao adfDao = new ADFDao(context);
            ADFInfo adf = adfDao.queryADFByUUID(endPointInfo[0]);
            PointDao pointDao = new PointDao(context);
            PointInfo point = pointDao.queryPointByID(endPointInfo[1]);
            pointWithADFInfo.setADFInfo(adf);
            pointWithADFInfo.setPointInfo(point);
            return pointWithADFInfo;
        } else {
            Log.i(TAG, "[getEndPointInfo]endPointInfoSet is empty");
            return null;
        }
    }
}
