package trendmicro.com.tangoindoornavigation.ui.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.atap.tango.reconstruction.TangoPolygon;
import com.vividsolutions.jts.geom.Coordinate;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import  trendmicro.com.tangoindoornavigation.R;

/**
 * Created by hugo on 26/07/2017.
 */
public class FloorplanView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = FloorplanView.class.getSimpleName();

    // Scale between meters and pixels. Hardcoded to a reasonable default.
    private static final float SCALE = 50f;

    private volatile List<TangoPolygon> mPolygons = new ArrayList<>();

    private Paint mBackgroundPaint;
    private Paint mWallPaint;
    private Paint mSpacePaint;
    private Paint mFurniturePaint;
    private Paint mUserMarkerPaint;
    private Paint mMarkerPaint;
    private Paint mMarkerPaint0;
    private Paint mMarkerPaint1;
    private Paint mMarkerPaint2;
    private Paint mMarkerPaint3;
    private Paint mMarkerPaint4;

    private Path mUserMarkerPath;
    private Coordinate mUserMarkerCoordinate;

    private Matrix mCamera;
    private Matrix mCameraTest;
    private Matrix mCameraInverse;
    private Matrix mCameraTestInverse;

    private boolean mIsDrawing = false;
    private boolean mHasMarker = false;
    private SurfaceHolder mSurfaceHolder;

    private float mMinAreaSpace = 0f;
    private float mMinAreaWall = 0f;

    private float mTranslationX = 0;
    private float mTranslationY = 0;
    private float mUserMarkerTranslationX = 0;
    private float mUserMarkerTranslationY = 0;
    private boolean mUserMarkerTranslationKeep = false;


    /**
     * Custom render thread, running at a fixed 10Hz rate.
     */
    private class RenderThread extends Thread {
        @Override
        public void run() {
            while (mIsDrawing) {
                Canvas canvas = mSurfaceHolder.lockCanvas();
                if (canvas != null) {
                    doDraw(canvas);
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    };
    private RenderThread mDrawThread;

    /**
     * Pre-drawing callback.
     */
    public interface DrawingCallback {
        /**
         * Called during onDraw, before any element is drawn to the view canvas.
         */
        void onPreDrawing();
    }

    private DrawingCallback mCallback;

    public FloorplanView(Context context) {
        super(context);
        init();
    }

    public FloorplanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FloorplanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Get parameters.
        TypedValue typedValue = new TypedValue();
        getResources().getValue(R.dimen.min_area_space, typedValue, true);
        mMinAreaSpace = typedValue.getFloat();
        getResources().getValue(R.dimen.min_area_wall, typedValue, true);
        mMinAreaWall = typedValue.getFloat();

        // Pre-create graphics objects.
        mWallPaint = new Paint();
        mWallPaint.setColor(getResources().getColor(android.R.color.black));
        mWallPaint.setStyle(Paint.Style.STROKE);
        mWallPaint.setStrokeWidth(3);
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(getResources().getColor(android.R.color.white));
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mSpacePaint = new Paint();
        mSpacePaint.setColor(getResources().getColor(R.color.explored_space));
        mSpacePaint.setStyle(Paint.Style.FILL);
        mFurniturePaint = new Paint();
        mFurniturePaint.setColor(getResources().getColor(R.color.furniture));
        mFurniturePaint.setStyle(Paint.Style.FILL);
        mUserMarkerPaint = new Paint();
        mUserMarkerPaint.setColor(getResources().getColor(R.color.user_marker));
        mUserMarkerPaint.setStyle(Paint.Style.FILL);
        mMarkerPaint = new Paint();
        mMarkerPaint.setColor(getResources().getColor(R.color.colorAccent));
        mMarkerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mMarkerPaint.setStrokeWidth(5);

        mMarkerPaint0 = new Paint();
        mMarkerPaint0.setColor(getResources().getColor(R.color.colorPrimary));
        mMarkerPaint0.setStyle(Paint.Style.FILL_AND_STROKE);
        mMarkerPaint0.setStrokeWidth(5);
        mMarkerPaint1 = new Paint();
        mMarkerPaint1.setColor(getResources().getColor(R.color.colorPrimary1));
        mMarkerPaint1.setStyle(Paint.Style.FILL_AND_STROKE);
        mMarkerPaint1.setStrokeWidth(5);
        mMarkerPaint2 = new Paint();
        mMarkerPaint2.setColor(getResources().getColor(R.color.colorPrimary2));
        mMarkerPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
        mMarkerPaint2.setStrokeWidth(5);
        mMarkerPaint3 = new Paint();
        mMarkerPaint3.setColor(getResources().getColor(R.color.colorPrimary3));
        mMarkerPaint3.setStyle(Paint.Style.FILL_AND_STROKE);
        mMarkerPaint3.setStrokeWidth(5);
        mMarkerPaint4 = new Paint();
        mMarkerPaint4.setColor(getResources().getColor(R.color.colorPrimary4));
        mMarkerPaint4.setStyle(Paint.Style.FILL_AND_STROKE);
        mMarkerPaint4.setStrokeWidth(5);

        mUserMarkerPath = new Path();
        mUserMarkerPath.lineTo(-0.2f * SCALE, 0);
        mUserMarkerPath.lineTo(-0.2f * SCALE, -0.05f * SCALE);
        mUserMarkerPath.lineTo(0.2f * SCALE, -0.05f * SCALE);
        mUserMarkerPath.lineTo(0.2f * SCALE, 0);
        mUserMarkerPath.lineTo(0, 0);
        mUserMarkerPath.lineTo(0, -0.05f * SCALE);
        mUserMarkerPath.lineTo(-0.4f * SCALE, -0.5f  * SCALE);
        mUserMarkerPath.lineTo(0.4f  * SCALE, -0.5f * SCALE);
        mUserMarkerPath.lineTo(0, 0);
        mCamera = new Matrix();
        mCameraInverse = new Matrix();
        mCameraTest = new Matrix();
        mCameraTestInverse = new Matrix();

        // Register for surface callback events.
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mIsDrawing = true;
        mDrawThread = new RenderThread();
        mDrawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        mSurfaceHolder = surfaceHolder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mIsDrawing = false;
    }

    private void doDraw(Canvas canvas) {
        // Notify the activity so that it can use Tango to query the current device pose.
        if (mCallback != null) {
            mCallback.onPreDrawing();
        }

        // Erase the previous canvas image.
        canvas.drawColor(getResources().getColor(android.R.color.white));

        // Start drawing from the center of the canvas.
        float translationX = canvas.getWidth() / 2f;
        float translationY = canvas.getHeight() / 2f;
        canvas.translate(translationX, translationY);

        // Update position and orientation based on the device position and orientation.
        canvas.concat(mCamera);

        // Draw all the polygons. Make a shallow copy in case mPolygons is reset while rendering.
        List<TangoPolygon> drawPolygons = mPolygons;
        //Log.i(TAG, "drawPolygons.size() = " + drawPolygons.size());
        boolean largestSpaceDrawn = false;
        for (TangoPolygon polygon : drawPolygons) {
            Paint paint;
            switch(polygon.layer) {
                case TangoPolygon.TANGO_3DR_LAYER_FURNITURE:
                    paint = mFurniturePaint;
                    break;
                case TangoPolygon.TANGO_3DR_LAYER_SPACE:
                    // Only draw free space polygons larger than 2 square meter.
                    // The goal of this is to suppress free space polygons in front of windows.
                    // Always draw holes (=negative area) independent of surface area.
                    if (polygon.area > 0) {
                        if (largestSpaceDrawn && polygon.area < mMinAreaSpace) {
                            continue;
                        }
                        largestSpaceDrawn = true;
                    }
                    paint = mSpacePaint;
                    break;
                case TangoPolygon.TANGO_3DR_LAYER_WALLS:
                    // Only draw wall polygons larger than 20cm x 20cm to suppress noise.
                    if (Math.abs(polygon.area) < mMinAreaWall) {
                        continue;
                    }
                    paint = mWallPaint;
                    break;
                default:
                    //Log.w(TAG, "Ignoring polygon with unknown layer value: " + polygon.layer);
                    continue;
            }
            if (polygon.area < 0.0) {
                paint = mBackgroundPaint;
            }
            Path path = new Path();
            float[] p = polygon.vertices2d.get(0);
            // NOTE: We need to flip the Y axis since the polygon data is in Tango start of
            // service frame (Y+ forward) and we want to draw image coordinates (Y+ 2D down).
            path.moveTo(p[0] * SCALE, -1 * p[1] * SCALE);

            for (int i = 1; i < polygon.vertices2d.size(); i++) {
                float[] point = polygon.vertices2d.get(i);
                path.lineTo(point[0] * SCALE, -1 * point[1] * SCALE);
            }
            if (polygon.isClosed) {
                path.close();
            }
            canvas.drawPath(path, paint);

        }



        canvas.concat(mCameraInverse);
        canvas.drawPath(mUserMarkerPath, mUserMarkerPaint);

        if (mHasMarker) {
            //float CircleX = mUserMarkerCoordinate.x -
            /*float CircleXDiff = 0;
            if ((mTranslationX > 0 && mUserMarkerTranslationX > 0) || (mTranslationX < 0 && mUserMarkerTranslationX < 0)) {
                CircleXDiff = Math.abs(mTranslationX - mUserMarkerTranslationX);
                if (mTranslationX < 0) {
                    CircleXDiff = CircleXDiff * -1;
                }
            } else if (mTranslationX > 0 && mUserMarkerTranslationX < 0) {
                CircleXDiff = mTranslationX - mUserMarkerTranslationX;
            } else if (mTranslationX < 0 && mUserMarkerTranslationX > 0) {
                CircleXDiff = (mUserMarkerTranslationX - mTranslationX) * -1;
            }
            float CircleYDiff = 0;
            if ((mTranslationY > 0 && mUserMarkerTranslationY > 0) || (mTranslationY < 0 && mUserMarkerTranslationY < 0)) {
                CircleYDiff = Math.abs(mTranslationY - mUserMarkerTranslationY);
                if (mTranslationY < 0) {
                    CircleYDiff = CircleYDiff * -1;
                }
            } else if (mTranslationY > 0 && mUserMarkerTranslationY < 0) {
                CircleYDiff = mTranslationY - mUserMarkerTranslationY;
            } else if (mTranslationY < 0 && mUserMarkerTranslationY > 0) {
                CircleYDiff = (mUserMarkerTranslationY - mTranslationY) * -1;
            }
            float CircleX = (float) mUserMarkerCoordinate.x + CircleXDiff;
            float CircleY = (float) mUserMarkerCoordinate.y + CircleYDiff;*/
            //canvas.drawCircle( mTranslationX * (float) 18.897, mTranslationY * (float) 18.897 * - 1, 10, mMarkerPaint);
            canvas.drawCircle(mTranslationX * SCALE, mTranslationY * SCALE * -1, 10, mMarkerPaint);
            //Log.i(TAG, "drawCircle : X = " + mTranslationX * SCALE + ", Y = " + mTranslationY * SCALE * -1);
            //mHasMarker = false;
        }

        // Draw a user / device marker.
        /*canvas.drawCircle(0, 0, 10, mMarkerPaint0);
        canvas.drawCircle(50, 50, 10, mMarkerPaint1);
        canvas.drawCircle(50, -50, 10, mMarkerPaint2);
        canvas.drawCircle(-50, 50, 10, mMarkerPaint3);
        canvas.drawCircle(-50, -50, 10, mMarkerPaint4);
        canvas.drawCircle(450, 300, 10, mMarkerPaint1);
        canvas.drawCircle(450, -300, 10, mMarkerPaint2);
        canvas.drawCircle(-450, 300, 10, mMarkerPaint3);
        canvas.drawCircle(-450, -300, 10, mMarkerPaint4);*/

    }

    /**
     * Sets the new floorplan polygons model and levels.
     */
    public void setFloorplan(List<TangoPolygon> polygons) {
        mPolygons = polygons;
    }

    public void registerCallback(DrawingCallback callback) {
        mCallback = callback;
    }

    public void setMarker(Coordinate coordinate) {
        mHasMarker = true;
        mUserMarkerCoordinate = coordinate;
    }

    /**
     * Updates the current rotation and translation to be used for the map. This is called with the
     * current device position and orientation.
     */
    public void updateCameraMatrix(float translationX, float translationY, float yawRadians) {
        //Log.i(TAG, "updateCameraMatrix : translationX = " + translationX+ ", translationY = " + translationY);
        mTranslationX = translationX;
        mTranslationY = translationY;
        /*if (mHasMarker && !mUserMarkerTranslationKeep) {
            mUserMarkerTranslationX = translationX;
            mUserMarkerTranslationY = translationY;
            mUserMarkerTranslationKeep = true;
            //Log.i(TAG, "updateCameraMatrix : mUserMarkerTranslationX = " + mUserMarkerTranslationX+ ", mUserMarkerTranslationY = " + mUserMarkerTranslationY);
            //Log.i(TAG, "updateCameraMatrix : mUserMarkerCoordinate.x = " + mUserMarkerCoordinate.x+ ", mUserMarkerCoordinate.y = " + mUserMarkerCoordinate.y);
        }*/
        mCamera.setTranslate(-translationX * SCALE, translationY * SCALE);
        mCamera.preRotate((float) Math.toDegrees(yawRadians), translationX * SCALE, -translationY
                * SCALE);
        mCamera.invert(mCameraInverse);
    }

    public Bitmap saveSignature(){

        Bitmap bitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        //this.draw(canvas);
        doDraw(canvas);

        File file = new File(Environment.getExternalStorageDirectory() + "/sign.png");
        Log.i(TAG, "saveSignature = " + file.getAbsolutePath());

        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
