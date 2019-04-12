package trendmicro.com.tangoindoornavigation.rendering.animation;

import android.util.Log;

import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.math.vector.Vector3;

import trendmicro.com.tangoindoornavigation.rendering.NavigationPath;

/**
 * Created by hugo on 16/10/2017.
 */

public class NavigationPathAnimation3D extends Animation3D {

    private static final String TAG = NavigationPathAnimation3D.class.getSimpleName();
    protected NavigationPath mNavigationPath;
    private Vector3 mVectorStart;
    private Vector3 mVectorEnd;

    protected int mIndex = 1;
    protected int mTotal = 0;

    public NavigationPathAnimation3D() {
        super();
    }

    @Override
    public void eventStart() {
        if (isFirstStart()) {
            mNavigationPath = (NavigationPath) mTransformable3D;
            mVectorStart = mNavigationPath.getStartVector3();
            mVectorEnd = mNavigationPath.getEndVector3();
            //mTotal = mNavigationPath.getTotalCount();
            Log.i(TAG, "[eventStart] mTotal = " + mTotal);
        }


        super.eventStart();
    }


    @Override
    protected void applyTransformation() {

        //Log.i(TAG, "[applyTransformation] getInterpolatedTime() = " + getInterpolatedTime());
        double delta = 1.0 / (mTotal);
        if (getInterpolatedTime() >= (delta * mIndex)) {
            Log.i(TAG, "[applyTransformation] rebuildPoints() mIndex = " + mIndex);
            if (mIndex == mTotal) {
                //mNavigationPath.anmationVectorUpdate(-1);
            } else {
                //mNavigationPath.anmationVectorUpdate(mIndex);
            }
            mIndex++;
        }
    }
}
