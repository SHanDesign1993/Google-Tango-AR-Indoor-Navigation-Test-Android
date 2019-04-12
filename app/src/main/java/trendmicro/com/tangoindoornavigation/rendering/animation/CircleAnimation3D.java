package trendmicro.com.tangoindoornavigation.rendering.animation;

import android.util.Log;

import org.rajawali3d.animation.Animation3D;

import trendmicro.com.tangoindoornavigation.rendering.Circle;

/**
 * Created by hugo on 13/10/2017.
 */

public class CircleAnimation3D extends Animation3D {

    private static final String TAG = CircleAnimation3D.class.getSimpleName();
    protected Circle mCircle;

    protected int mIndex = 3;

    public CircleAnimation3D() {
        super();
    }

    @Override
    public void eventStart() {
        if (isFirstStart())
            mCircle = (Circle) mTransformable3D;

        mIndex = 3;
        super.eventStart();
    }


    @Override
    protected void applyTransformation() {

        Log.i(TAG, "[applyTransformation] getInterpolatedTime() = " + getInterpolatedTime());
        double delta = 1.0 / 39;
        if (getInterpolatedTime() >= (delta * (mIndex - 2))) {
            Log.i(TAG, "[applyTransformation] rebuildPoints() mIndex = " + mIndex);
            mCircle.rebuildPoints(mIndex);
            mIndex++;
        }
    }

}
