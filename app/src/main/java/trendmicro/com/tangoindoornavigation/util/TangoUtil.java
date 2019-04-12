package trendmicro.com.tangoindoornavigation.util;

import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.rajawali.DeviceExtrinsics;
import com.projecttango.rajawali.Pose;
import com.vividsolutions.jts.geom.Coordinate;

import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import trendmicro.com.tangoindoornavigation.data.PathVector;

/**
 * Created by hugo on 24/07/2017.
 */
public class TangoUtil {

    private static final String TAG = TangoUtil.class.getSimpleName();

    public enum VectorDirection {
        LEFT,
        RIGHT,
        FORWARD,
        BACKWARD,
        UNKNOW
    }

    public static Coordinate coordinate(TangoPoseData tangoPoseData) {
        float[] translation = tangoPoseData.getTranslationAsFloats();
        return new Coordinate(translation[TangoPoseData.INDEX_TRANSLATION_X],
                translation[TangoPoseData.INDEX_TRANSLATION_Y],
                translation[TangoPoseData.INDEX_TRANSLATION_Z]);
    }

    public static Coordinate coordinate(Pose pose) {
        Vector3 v = pose.getPosition();
        return new Coordinate(v.x, v.y, v.z);
    }

    public static Coordinate coordinate(Vector2 v) {
        return new Coordinate(v.getX(), v.getY(), 0);
    }

    public static Coordinate coordinate(Vector3 v) {
        return new Coordinate(v.x, v.z, 0);
    }

    public static VectorDirection getDirectionByVector3(Vector3 beforeV, Vector3 nowV, Vector3 nextV) {
        //Log.i(TAG, "[getDirectionByVector3]beforeV = " + beforeV.toString());
        //Log.i(TAG, "[getDirectionByVector3]nowV = " + nowV.toString());
        //Log.i(TAG, "[getDirectionByVector3]nextV = " + nextV.toString());

        Vector3 beforeToNow = Vector3.subtractAndCreate(nowV, beforeV);
        Vector3 nowToNext = Vector3.subtractAndCreate(nextV, nowV);
        Vector3 crossVector3 = Vector3.crossAndCreate(beforeToNow, nowToNext);
        Vector3 base = new Vector3(0, 1, 0);
        double angle = Vector3.dot(base, crossVector3);

        //Log.i(TAG, "[getDirectionByVector3]angle = " + angle);
        VectorDirection direction = VectorDirection.UNKNOW;
        if (Double.compare(angle, 0.0) > 0) {
            direction = VectorDirection.RIGHT;
        } else if (Double.compare(angle, 0.0) < 0){
            direction = VectorDirection.LEFT;
        } else if (Double.compare(angle, 0.0) == 0){
            direction = VectorDirection.FORWARD;
        } else if (Double.compare(angle, -0.0) == 0){
            direction = VectorDirection.BACKWARD;
        } else {
            Log.i(TAG, "[getDirectionByVector3]UNKNOW ");
        }
        //Log.i(TAG, "[getDirectionByVector3]direction = " + direction);
        return direction;
    }

    public static double getSlope(Vector2 v1, Vector2 v2) {
        //Log.i(TAG, "[getSlope]v1 = ( " + v1.getX() + ", " + v1.getY() + "), v2 = (" + v2.getX() + ", " + v2.getY() + "), Slope = " + ((v2.getY() - v1.getY()) / (v2.getX() - v1.getX())));
        return (v2.getY() - v1.getY()) / (v2.getX() - v1.getX());
    }

    public static DeviceExtrinsics getDeviceExtrinsics(Tango tango, double timestamp) {
        // Create camera to IMU transform.
        TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
        TangoPoseData imuToRgbPose = tango.getPoseAtTime(timestamp, framePair);

        // Create device to IMU transform.
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
        TangoPoseData imuToDevicePose = tango.getPoseAtTime(timestamp, framePair);

        // Create depth camera to IMU transform.
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH;
        TangoPoseData imuToDepthPose = tango.getPoseAtTime(timestamp, framePair);

        return new DeviceExtrinsics(imuToDevicePose, imuToRgbPose, imuToDepthPose);
    }
}
