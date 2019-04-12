package trendmicro.com.tangoindoornavigation.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

import com.google.atap.tango.reconstruction.TangoFloorplanLevel;
import com.google.atap.tango.reconstruction.TangoPolygon;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.tangosupport.TangoSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import  trendmicro.com.tangoindoornavigation.R;
import  trendmicro.com.tangoindoornavigation.TangoFloorplanner;
import trendmicro.com.tangoindoornavigation.ui.custom.FloorplanView;

/**
 * Created by hugo on 01/08/2017.
 */
public class ReconstructionActivity extends Activity implements FloorplanView.DrawingCallback{

    private static final String TAG = ReconstructionActivity.class.getSimpleName();
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    private Tango mTango;
    private TangoConfig mConfig;
    private FloorplanView mFloorplanView;
    private TangoFloorplanner mTangoFloorplanner;

    protected AtomicBoolean tangoIsConnected = new AtomicBoolean(false);
    protected AtomicBoolean tangoFrameIsAvailable = new AtomicBoolean(false);
    private boolean mIsPermissionReady = false;
    private int mDisplayRotation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reconstruction);

        mFloorplanView = (FloorplanView) findViewById(R.id.floorplan);
        mFloorplanView.registerCallback(this);

        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    synchronized (this) {
                        setDisplayRotation();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            }, null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check and request camera permission at run time.
        if (checkAndRequestPermissions()) {
            mIsPermissionReady = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsPermissionReady) {
            // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
            // in onPause, this will unbind Tango Service, so every time onResume gets called we
            // should create a new Tango object.
            mTango = new Tango(ReconstructionActivity.this, new Runnable() {
                // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
                // will be running on a new thread.
                // When Tango is ready, we can call Tango functions safely here only when there are no
                // UI thread changes involved.
                @Override
                public void run() {
                    synchronized (ReconstructionActivity.this) {
                        if (tangoIsConnected.compareAndSet(false, true)) {
                            try {
                                mConfig = setTangoConfig(mTango);
                                mTango.connect(mConfig);
                                startupTango();
                                TangoSupport.initialize(mTango);
                                setDisplayRotation();
                            } catch (TangoOutOfDateException e) {
                                Log.e(TAG, getString(R.string.tango_out_of_date_exception), e);
                                showsToastAndFinishOnUiThread(R.string.tango_out_of_date_exception);
                            } catch (TangoErrorException e) {
                                Log.e(TAG, getString(R.string.tango_error), e);
                                showsToastAndFinishOnUiThread(R.string.tango_error);
                            } catch (TangoInvalidException e) {
                                Log.e(TAG, getString(R.string.tango_invalid), e);
                                showsToastAndFinishOnUiThread(R.string.tango_invalid);
                            } catch (SecurityException e) {
                                // Area Learning permissions are required. If they are not available,
                                // SecurityException is thrown.
                                Log.e(TAG, getString(R.string.no_permissions), e);
                                showsToastAndFinishOnUiThread(R.string.no_permissions);
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Clear the relocalization state; we don't know where the device will be since our app
        // will be paused.
        synchronized (this) {
            if (tangoIsConnected.compareAndSet(true, false)) {
                try {

                    if (mTangoFloorplanner != null) {
                        mTangoFloorplanner.stopFloorplanning();
                        mTangoFloorplanner.resetFloorplan();
                        mTangoFloorplanner.release();
                    }
                    mTango.disconnect();
                } catch (TangoErrorException e) {
                    Log.e(TAG, getString(R.string.tango_error), e);
                }
            }
        }
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setTangoConfig(Tango tango) {
        // Use default configuration for Tango Service.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);

        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putInt(TangoConfig.KEY_INT_DEPTH_MODE, TangoConfig.TANGO_DEPTH_MODE_POINT_CLOUD);
        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     */
    private void startupTango() {

        mTangoFloorplanner = new TangoFloorplanner(new TangoFloorplanner
                .OnFloorplanAvailableListener() {
            @Override
            public void onFloorplanAvailable(List<TangoPolygon> polygons,
                                             List<TangoFloorplanLevel> levels) {
                mFloorplanView.setFloorplan(polygons);
            }
        });
        // Set camera intrinsics to TangoFloorplanner.
        mTangoFloorplanner.setDepthCameraCalibration(mTango.getCameraIntrinsics
                (TangoCameraIntrinsics.TANGO_CAMERA_DEPTH));


        mTangoFloorplanner.startFloorplanning();

        // Set Tango listeners for Poses Device wrt Start of Service, Device wrt
        // ADF and Start of Service wrt ADF.
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();

        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {

            @Override
            public void onPoseAvailable(TangoPoseData pose) {

            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData tangoPointCloudData) {
                mTangoFloorplanner.onPointCloudAvailable(tangoPointCloudData);
            }

            @Override
            public void onTangoEvent(final TangoEvent event) {
                // Ignoring TangoEvents.
            }

            @Override
            public void onFrameAvailable(int cameraId) {

            }
        });
    }

    /**
     * Method called each time right before the floorplan is drawn. It allows use of the Tango
     * Service to get the device position and orientation.
     */
    @Override
    public void onPreDrawing() {
        try {
            // Synchronize against disconnecting while using the service.
            synchronized (ReconstructionActivity.this) {
                // Don't execute any Tango API actions if we're not connected to
                // the service.
                if (!tangoIsConnected.get()) {
                    return;
                }

                // Calculate the device pose in OpenGL engine (Y+ up).
                TangoPoseData devicePose = TangoSupport.getPoseAtTime(0.0,
                        TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                        TangoPoseData.COORDINATE_FRAME_DEVICE,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        TangoSupport.TANGO_SUPPORT_ENGINE_OPENGL,
                        mDisplayRotation);

                if (devicePose.statusCode == TangoPoseData.POSE_VALID) {
                    // Extract position and rotation around Z.
                    float[] devicePosition = devicePose.getTranslationAsFloats();
                    float[] deviceOrientation = devicePose.getRotationAsFloats();
                    float yawRadians = yRotationFromQuaternion(deviceOrientation[0],
                            deviceOrientation[1], deviceOrientation[2],
                            deviceOrientation[3]);

                    mFloorplanView.updateCameraMatrix(devicePosition[0], -devicePosition[2],
                            yawRadians);
                } else {
                    Log.w(TAG, "Can't get last device pose");
                }
            }
        } catch (TangoErrorException e) {
            Log.e(TAG, "Tango error while querying device pose.", e);
        } catch (TangoInvalidException e) {
            Log.e(TAG, "Tango exception while querying device pose.", e);
        }
    }

    /**
     * Calculates the rotation around Y (yaw) from the given quaternion.
     */
    private static float yRotationFromQuaternion(float x, float y, float z, float w) {
        return (float) Math.atan2(2 * (w * y - x * z), w * (w + x) - y * (z + y));
    }

    /**
     * Set the display rotation.
     */
    private void setDisplayRotation() {
        Display display = getWindowManager().getDefaultDisplay();
        mDisplayRotation = display.getRotation();
    }

    /**
     * Check to see if we have the necessary permissions for this app; ask for them if we don't.
     *
     * @return True if we have the necessary permissions, false if we don't.
     */
    private boolean checkAndRequestPermissions() {
        if (!hasCameraPermission()) {
            requestCameraPermission();
            return false;
        }
        return true;
    }

    /**
     * Check to see if we have the necessary permissions for this app.
     */
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the necessary permissions for this app.
     */
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
            showRequestPermissionRationale();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{CAMERA_PERMISSION},
                    CAMERA_PERMISSION_CODE);
        }
    }

    /**
     * If the user has declined the permission before, we have to explain that the app needs this
     * permission.
     */
    private void showRequestPermissionRationale() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Java Floorplan Reconstruction Example requires camera permission")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(ReconstructionActivity.this,
                                new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    }
                })
                .create();
        dialog.show();
    }

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private void showsToastAndFinishOnUiThread(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ReconstructionActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    public void onSaveButtonClick(View v) {
        mFloorplanView.saveSignature();

        showsToastAndFinishOnUiThread(R.string.image_saved);
    }
}
