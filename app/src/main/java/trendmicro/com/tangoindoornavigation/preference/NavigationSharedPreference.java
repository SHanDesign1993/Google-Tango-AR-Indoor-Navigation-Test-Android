package trendmicro.com.tangoindoornavigation.preference;

import android.content.Context;
import android.content.SharedPreferences;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointWithADFInfo;

import static android.R.id.list;

/**
 * Created by hugo on 29/08/2017.
 */

public class NavigationSharedPreference {
    private static SharedPreferences mPrefs;
    private static final String SHARED_PREFERENCES_NAME = "navigation";

    private static final String KEY_END_POINT_INFO = "end_point_info";
    private static final String KEY_ENABLE_GRID = "enable_grid";
    private static final String KEY_QUAD_TREE_START = "quad_tree_start";
    private static final String KEY_QUAD_TREE_RANGE = "quad_tree_range";
    private static final String KEY_DETECT_DISTANCE = "detect_distance";

    private static synchronized SharedPreferences getSharedPreference(
            Context context) {
        if (mPrefs == null) {
            mPrefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                    Context.MODE_PRIVATE);
        }
        return mPrefs;
    }

    public static void setEndPointInfoSet(Context context, String endPointInfoString) {
        getSharedPreference(context).edit().putString(KEY_END_POINT_INFO, endPointInfoString).commit();
    }

    public static String getEndPointInfoSet(Context context) {
        return getSharedPreference(context).getString(KEY_END_POINT_INFO, "");
    }

    public static void setEnableGrid(Context context, boolean enableGrid) {
        getSharedPreference(context).edit().putBoolean(KEY_ENABLE_GRID, enableGrid).commit();
    }

    public static boolean getEnableGrid(Context context) {
        return getSharedPreference(context).getBoolean(KEY_ENABLE_GRID, false);
    }

    public static void setQuadTreeStart(Context context, int quadTreeStart) {
        getSharedPreference(context).edit().putInt(KEY_QUAD_TREE_START, quadTreeStart).commit();
    }

    public static int getQuadTreeStart(Context context) {
        return getSharedPreference(context).getInt(KEY_QUAD_TREE_START, -120);
    }

    public static void setQuadTreeRange(Context context, int quadTreeRange) {
        getSharedPreference(context).edit().putInt(KEY_QUAD_TREE_RANGE, quadTreeRange).commit();
    }

    public static int getQuadTreeRange(Context context) {
        return getSharedPreference(context).getInt(KEY_QUAD_TREE_RANGE, 240);
    }

    public static void setDetectDistance(Context context, double detectDistance) {
        getSharedPreference(context).edit().putFloat(KEY_DETECT_DISTANCE, (float)detectDistance).commit();
    }

    public static double getDetectDistance(Context context) {
        DecimalFormat df = new DecimalFormat("##.0");
        return Double.parseDouble(df.format(getSharedPreference(context).getFloat(KEY_DETECT_DISTANCE, 1.0f)));
    }
}
