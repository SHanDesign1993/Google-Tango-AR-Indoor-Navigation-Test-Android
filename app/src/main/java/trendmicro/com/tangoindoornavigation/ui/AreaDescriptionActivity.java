/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package trendmicro.com.tangoindoornavigation.ui;

import com.google.atap.tango.reconstruction.TangoFloorplanLevel;
import com.google.atap.tango.reconstruction.TangoPolygon;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
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
import com.projecttango.rajawali.DeviceExtrinsics;
import com.projecttango.tangosupport.TangoPointCloudManager;
import com.projecttango.tangosupport.TangoSupport;
import com.projecttango.tangosupport.ux.TangoUx;
import com.projecttango.tangosupport.ux.UxExceptionEvent;
import com.projecttango.tangosupport.ux.UxExceptionEventListener;
import com.vividsolutions.jts.geom.Coordinate;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import trendmicro.com.tangoindoornavigation.db.ADFDao;
import  trendmicro.com.tangoindoornavigation.db.PointDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import  trendmicro.com.tangoindoornavigation.db.dto.PointInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointWithADFInfo;
import  trendmicro.com.tangoindoornavigation.dialog.GoWhereDialog;
import trendmicro.com.tangoindoornavigation.dialog.NavigationStatusDialog;
import trendmicro.com.tangoindoornavigation.dialog.TargetFloorConfirmDialog;
import trendmicro.com.tangoindoornavigation.eventbus.Event;
import trendmicro.com.tangoindoornavigation.preference.NavigationSharedPreference;
import trendmicro.com.tangoindoornavigation.preference.SharedPreferenceHelper;
import trendmicro.com.tangoindoornavigation.ui.custom.FloorplanView;
import  trendmicro.com.tangoindoornavigation.R;
import  trendmicro.com.tangoindoornavigation.SaveAdfTask;
import  trendmicro.com.tangoindoornavigation.ScenePreFrameCallbackAdapter;
import  trendmicro.com.tangoindoornavigation.rendering.SceneRenderer;
import  trendmicro.com.tangoindoornavigation.TangoFloorplanner;
import  trendmicro.com.tangoindoornavigation.dialog.SetAdfNameDialog;
import trendmicro.com.tangoindoornavigation.util.TangoUtil;

import static com.projecttango.tangosupport.ux.TangoUx.TYPE_HOLD_POSTURE_FORWARD;
import static com.projecttango.tangosupport.ux.TangoUx.TYPE_HOLD_POSTURE_NONE;
import static trendmicro.com.tangoindoornavigation.util.TangoUtil.getDeviceExtrinsics;


public class AreaDescriptionActivity extends Activity implements
        SetAdfNameDialog.CallbackListener,
        GoWhereDialog.CallbackListener,
        NavigationStatusDialog.CallbackListener,
        TargetFloorConfirmDialog.CallbackListener,
        SaveAdfTask.SaveAdfListener,
        FloorplanView.DrawingCallback {

    private static final String TAG = AreaDescriptionActivity.class.getSimpleName();

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    private Tango mTango;
    private TangoConfig mConfig;
    private TextView mUuidTextView;
    private TextView mRelocalizationTextView;
    private TextView mLoadADFTextView;
    private TextView mLearningModeTextView;
    private TextView mDevicePoseTextView;
    private TextView mDistanceTextView;
    private TextView mDepthTextView;
    private RajawaliSurfaceView mainSurfaceView;
    private Button mSaveAdfButton;

    protected SceneRenderer renderer;
    protected AtomicBoolean tangoIsConnected = new AtomicBoolean(false);
    protected AtomicBoolean tangoFrameIsAvailable = new AtomicBoolean(false);

    protected int connectedTextureId;
    protected TangoCameraIntrinsics intrinsics;
    protected DeviceExtrinsics extrinsics;
    protected double rgbFrameTimestamp;
    protected double cameraPoseTimestamp;
    protected static final int INVALID_TEXTURE_ID = -1;

    private boolean mIsRelocalized;
    private boolean mIsLearningMode;
    private boolean mIsConstantSpaceRelocalize;

    // Long-running task to save the ADF.
    private SaveAdfTask mSaveAdfTask;

    private static final int UPDATE_INTERVAL_MS = 100;
    private TangoPoseData[] mPoses;

    private final Object mSharedLock = new Object();
    private String mPointName = "";
    private String mUUID = "";
    private int mBuildingID;

    private int mDisplayRotation = 0;

    private FloorplanView mFloorplanView;
    private TangoFloorplanner mTangoFloorplanner;

    private boolean mIsPermissionReady = false;
    private boolean mIsZoomIn = false;
    private TangoPointCloudManager mPointCloudManager;
    private double mAverageDepth = 0.0d;
    private PointDao mPointDao;
    private ADFDao mADFDao;
    private EventBus mEventBus;
    boolean mIsDebug = false;

    // This changes the Camera Texture and Intrinsics
    protected static final int ACTIVE_CAMERA_INTRINSICS = TangoCameraIntrinsics.TANGO_CAMERA_COLOR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_learning);
        Intent intent = getIntent();
        mIsLearningMode = intent.getBooleanExtra(MainActivity.USE_AREA_LEARNING, false);
        mIsConstantSpaceRelocalize = intent.getBooleanExtra(MainActivity.LOAD_ADF, false);
        mUUID = intent.getStringExtra(MainActivity.LOAD_ADF_UUID);
        mBuildingID = Integer.parseInt(intent.getStringExtra(MainActivity.BUILDING));

        mainSurfaceView = (RajawaliSurfaceView) findViewById(R.id.gl_main_surface_view);
        mSaveAdfButton = (Button) findViewById(R.id.save_adf_button);
        mUuidTextView = (TextView) findViewById(R.id.adf_uuid_textview);
        mRelocalizationTextView = (TextView) findViewById(R.id.relocalization_textview);
        mLoadADFTextView = (TextView) findViewById(R.id.load_adf_textview);
        mLearningModeTextView = (TextView) findViewById(R.id.learning_mode_textview);
        mDevicePoseTextView = (TextView) findViewById(R.id.device_pose_textview);
        mDistanceTextView = (TextView) findViewById(R.id.distance);
        mDepthTextView = (TextView) findViewById(R.id.depth);

        renderer = new SceneRenderer(this);
        renderer.renderVirtualObjects(true);
        mainSurfaceView.setSurfaceRenderer(renderer);
        mainSurfaceView.setZOrderOnTop(false);

        mPoses = new TangoPoseData[3];
        mPointCloudManager = new TangoPointCloudManager();

        mFloorplanView = (FloorplanView) findViewById(R.id.floorplan);
        mFloorplanView.setZOrderOnTop(true);
        mFloorplanView.registerCallback(this);

        SharedPreferenceHelper.setEndPointInfo(getApplicationContext(), null); //reset preference
        mIsDebug = NavigationSharedPreference.getEnableGrid(getApplicationContext());
        mEventBus = EventBus.getDefault();
        mEventBus.register(this);

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

        mADFDao = new ADFDao(getApplicationContext());
        mPointDao = new PointDao(getApplicationContext());

        startUIThread();
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

        doOnResumeTasks();
    }

    @Override
    protected void onPause() {
        super.onPause();

        doOnPauseTasks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
    }

    private void doOnResumeTasks() {

        if (mIsPermissionReady) {

            if (renderer.getSceneInitialized()) {
                mIsRelocalized = false;
                renderer.resetData();
                renderer.reloadScene();
            }

            PointWithADFInfo endPointWithADFInfo = SharedPreferenceHelper.getEndPointInfo(getApplicationContext());
            if (endPointWithADFInfo != null && mIsConstantSpaceRelocalize) {
                ADFInfo adf = endPointWithADFInfo.getADFInfo();
                mUUID = adf.getUUID();
            }

            // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
            // in onPause, this will unbind Tango Service, so every time onResume gets called we
            // should create a new Tango object.
            mTango = new Tango(AreaDescriptionActivity.this, new Runnable() {
                // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
                // will be running on a new thread.
                // When Tango is ready, we can call Tango functions safely here only when there are no
                // UI thread changes involved.
                @Override
                public void run() {
                    synchronized (AreaDescriptionActivity.this) {
                        if (tangoIsConnected.compareAndSet(false, true)) {
                            try {
                                mConfig = setTangoConfig(
                                        mTango, mIsLearningMode, mIsConstantSpaceRelocalize);
                                mTango.connect(mConfig);
                                startupTango();
                                TangoSupport.initialize(mTango);
                                setupCameraProperties(mTango);
                                connectRenderer();
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

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (AreaDescriptionActivity.this) {
                                setupTextViewsAndButtons(mTango, mIsLearningMode,
                                        mIsConstantSpaceRelocalize);
                            }
                        }
                    });
                }
            });
        }
    }

    private void doOnPauseTasks() {
        // Clear the relocalization state; we don't know where the device will be since our app
        // will be paused.
        mIsRelocalized = false;
        synchronized (this) {
            if (tangoIsConnected.compareAndSet(true, false)) {
                try {

                    if (mTangoFloorplanner != null) {
                        mTangoFloorplanner.stopFloorplanning();
                        mTangoFloorplanner.resetFloorplan();
                        mTangoFloorplanner.release();
                    }

                    renderer.getCurrentScene().clearAnimations();
                    renderer.getCurrentScene().clearChildren();
                    renderer.getCurrentScene().clearFrameCallbacks();
                    mTango.disconnectCamera(ACTIVE_CAMERA_INTRINSICS);
                    tangoFrameIsAvailable.set(false);
                    connectedTextureId = INVALID_TEXTURE_ID;
                    mTango.disconnect();
                } catch (TangoErrorException e) {
                    Log.e(TAG, getString(R.string.tango_error), e);
                }
            }
        }
    }

    /**
     * Sets Texts views to display statistics of Poses being received. This also sets the buttons
     * used in the UI. Note that this needs to be called after TangoService and Config
     * objects are initialized since we use them for the SDK-related stuff like version number,
     * etc.
     */
    private void setupTextViewsAndButtons(Tango tango, boolean isLearningMode, boolean isLoadAdf) {

        if (isLearningMode) {
            // Disable save ADF button until Tango relocalizes to the current ADF.
            mSaveAdfButton.setEnabled(false);
            mLearningModeTextView.setText("True");
        } else {
            // Hide to save ADF button if leanring mode is off.
            mSaveAdfButton.setVisibility(View.GONE);
            mLearningModeTextView.setText("False");
        }

        if (isLoadAdf) {
            ADFInfo adf = mADFDao.queryADFByUUID(mUUID);
            if (adf == null) {
                mUuidTextView.setText(R.string.no_uuid);
            } else {
                mUuidTextView.setText(getString(R.string.current_adf_is)
                        + "(" + adf.getFloor() + " F) " + adf.getADFName() + " ( "+ mUUID + " )");
            }
            mLoadADFTextView.setText("True");
        } else {
            mLoadADFTextView.setText("False");
        }
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setTangoConfig(Tango tango, boolean isLearningMode, boolean isLoadAdf) {
        // Use default configuration for Tango Service.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        // Check if learning mode.
        if (isLearningMode) {
            // Set learning mode to config.
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);

        }
        // Check for Load ADF/Constant Space relocalization mode.
        if (isLoadAdf) {
            config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, mUUID);
        }
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);

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

        // Set Tango listeners for Poses Device wrt Start of Service, Device wrt
        // ADF and Start of Service wrt ADF.
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));
        //framePairs.add(new TangoCoordinateFramePair(
        //        TangoPoseData.COORDINATE_FRAME_PREVIOUS_DEVICE_POSE,
        //        TangoPoseData.COORDINATE_FRAME_DEVICE));

        mTango.connectListener(framePairs, new Tango.TangoUpdateCallback() {

            @Override
            public void onPoseAvailable(TangoPoseData pose) {

                // Make sure to have atomic access to Tango data so that UI loop doesn't interfere
                // while Pose call back is updating the data.
                synchronized (mSharedLock) {
                    // Check for Device wrt ADF pose, Device wrt Start of Service pose, Start of
                    // Service wrt ADF pose (this pose determines if the device is relocalized or
                    // not).
                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData
                            .COORDINATE_FRAME_DEVICE) {
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            mPoses[0] = pose;
                        } else {

                        }
                    }

                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
                            && pose.targetFrame == TangoPoseData
                            .COORDINATE_FRAME_DEVICE) {
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            mPoses[1] = pose;
                        } else {

                        }
                    }

                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData
                            .COORDINATE_FRAME_START_OF_SERVICE) {
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            mPoses[2] = pose;

                            mIsRelocalized = true;
                        } else {
                            mIsRelocalized = false;
                        }
                    }
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
            }

            @Override
                public void onPointCloudAvailable(TangoPointCloudData tangoPointCloudData) {

                mTangoFloorplanner.onPointCloudAvailable(tangoPointCloudData);
                mPointCloudManager.updatePointCloud(tangoPointCloudData);
            }

            @Override
            public void onTangoEvent(final TangoEvent event) {
                // Ignoring TangoEvents.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We are not using onFrameAvailable for this application.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Mark a camera frame as available for rendering in the OpenGL thread.
                    //mIsFrameAvailableTangoThread.set(true);
                    //mSurfaceView.requestRender();
                    tangoFrameIsAvailable.set(true);
                    renderer.setAverageDepth(mAverageDepth);
                    mainSurfaceView.requestRender();
                } else {
                    Log.i(TAG, "[onFrameAvailable]cameraId = " + cameraId);
                }
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
            synchronized (AreaDescriptionActivity.this) {
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

    protected void setupCameraProperties(Tango tango) {
        extrinsics = getDeviceExtrinsics(tango, 0.0);
        intrinsics = tango.getCameraIntrinsics(ACTIVE_CAMERA_INTRINSICS);
    }

    protected void connectRenderer() {
        renderer.getCurrentScene().registerFrameCallback(new ScenePreFrameCallbackAdapter() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                synchronized (AreaDescriptionActivity.this) {
                    if (!tangoIsConnected.get()) {
                        return;
                    }
                    if (!renderer.isSceneCameraConfigured()) {
                        renderer.setProjectionMatrix(intrinsics);
                    }
                    if (connectedTextureId != renderer.getTextureId()) {
                        mTango.connectTextureId(ACTIVE_CAMERA_INTRINSICS, renderer.getTextureId());
                        connectedTextureId = renderer.getTextureId();
                    }
                    if (tangoFrameIsAvailable.compareAndSet(true, false)) {
                        rgbFrameTimestamp = mTango.updateTexture(ACTIVE_CAMERA_INTRINSICS);
                    }
                    if (rgbFrameTimestamp > cameraPoseTimestamp) {
                        //TangoPoseData currentPose = getCurrentPose();
                        //if (currentPose != null && currentPose.statusCode == TangoPoseData.POSE_VALID) {
                        //    renderer.updateRenderCameraPose(currentPose, extrinsics);
                        //    cameraPoseTimestamp = currentPose.timestamp;
                        //}

                        TangoPoseData devicePoseFromMemory = devicePoseFromMemory();
                        if (devicePoseFromMemory != null
                                && devicePoseFromMemory.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                                && devicePoseFromMemory.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {
                            extrinsics = getDeviceExtrinsics(mTango, devicePoseFromMemory.timestamp);
                            if (mIsDebug) {
                                renderer.updateRenderCameraPoseWithGrid(devicePoseFromMemory, extrinsics, false);
                            } else {
                                renderer.updateRenderCameraPose(devicePoseFromMemory, extrinsics, false);
                            }
                            cameraPoseTimestamp = devicePoseFromMemory.timestamp;
                        }
                    }
                }
            }
        });
    }

    private TangoPoseData devicePoseFromMemory() {
        return mPoses[0];
    }

    private void updateTextViews() {
        mSaveAdfButton.setEnabled(mIsRelocalized);
        mRelocalizationTextView.setText(mIsRelocalized ?
                getString(R.string.localized) :
                getString(R.string.not_localized));
        final TangoPoseData devicePoseFromMemory = devicePoseFromMemory();
        if (devicePoseFromMemory != null
                && devicePoseFromMemory.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                && devicePoseFromMemory.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {
            mTangoFloorplanner.startFloorplanning();
            float translation[] = devicePoseFromMemory.getTranslationAsFloats();
            mDevicePoseTextView.setText("Position: " +
                    translation[0] + ", " + translation[1] + ", " + translation[2]);
            if (mIsDebug) {
                /*TangoPointCloudData pointCloud = mPointCloudManager.getLatestPointCloud();
                if (pointCloud != null) {
                    mAverageDepth = getAveragedDepth(pointCloud.points, pointCloud.numPoints);
                    mDepthTextView.setText("Num of Points : " + pointCloud.numPoints + ", Average Depth of Cloud points = " + mAverageDepth + " (m)");
                }*/
                mDepthTextView.setText("Num of Grid : " + renderer.getFloorPlanData().getFilledPoints().size());
            }

            if (renderer.isStartedNavigation()) {
                mDistanceTextView.setText("Current Device position to " + mPointName + " : "  + renderer.getNavigationPathDistance() + " (m)");
            }
        }
    }

    private void startUIThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(UPDATE_INTERVAL_MS);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    synchronized (mSharedLock) {

                                        if (mPoses == null) {
                                            return;
                                        } else {
                                            updateTextViews();
                                        }
                                    }
                                } catch (NullPointerException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * Calculates the average depth from a point cloud buffer.
     *
     * @param pointCloudBuffer
     * @param numPoints
     * @return Average depth.
     */
    private float getAveragedDepth(FloatBuffer pointCloudBuffer, int numPoints) {
        float totalZ = 0;
        float averageZ = 0;
        if (numPoints != 0) {
            int numFloats = 4 * numPoints;
            for (int i = 2; i < numFloats; i = i + 4) {
                totalZ = totalZ + pointCloudBuffer.get(i);
            }
            averageZ = totalZ / numPoints;
        }
        return averageZ;
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onAdfNameOk(String name, String uuid, int floor) {
        saveAdf(name, floor);
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onAdfNameCancelled() {
        // Continue running.
    }

    /**
     * The "Save ADF" button has been clicked.
     * Defined in {@code activity_area_description.xml}
     */
    public void saveAdfClicked(View view) {
        showSetAdfNameDialog();
    }

    public void zoomClicked(View view) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mFloorplanView.getLayoutParams();
        if (mIsZoomIn) {
            mIsZoomIn = false;
            params.height = 600;
            params.width = 900;
        } else {
            mIsZoomIn = true;
            params.height = 1200;
            params.width = 1800;
        }

        //Log.i(TAG, "params.height = " + params.height + ", params.width = " + params.width);

        mFloorplanView.setLayoutParams(params);
    }

    public void goWhereClicked(View view) {
        showGoWhereDialog();
    }

    private void showGoWhereDialog() {
        Bundle bundle = new Bundle();
        TangoPoseData poseData = devicePoseFromMemory();
        logPose(poseData);

        bundle.putSerializable(GoWhereDialog.UUID_KEY, mUUID);
        FragmentManager manager = getFragmentManager();
        GoWhereDialog setGoWhereDialog = new GoWhereDialog();
        setGoWhereDialog.setArguments(bundle);
        setGoWhereDialog.show(manager, "GoWhereDialog");
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onGoWhereOk(int currFloor, PointWithADFInfo targetPointInfo) {

        startNavigation(currFloor, targetPointInfo);
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onGoWhereCancelled() {
        // Continue running.
    }

    /**
     * Save the current Area Description File.
     * Performs saving on a background thread and displays a progress dialog.
     */
    private void saveAdf(String adfName, int floor) {
        mSaveAdfTask = new SaveAdfTask(this, this, mTango, adfName, floor, mBuildingID);
        mSaveAdfTask.execute();
    }

    /**
     * Handles failed save from mSaveAdfTask.
     */
    @Override
    public void onSaveAdfFailed(String adfName) {
        String toastMessage = String.format(
                getResources().getString(R.string.save_adf_failed_toast_format),
                adfName);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        mSaveAdfTask = null;
    }

    /**
     * Handles successful save from mSaveAdfTask.
     */
    @Override
    public void onSaveAdfSuccess(String adfName, String adfUuid) {
        String toastMessage = String.format(
                getResources().getString(R.string.save_adf_success_toast_format),
                adfName, adfUuid);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        mSaveAdfTask = null;
        finish();
    }

    /**
     * Shows a dialog for setting the ADF name.
     */
    private void showSetAdfNameDialog() {
        Bundle bundle = new Bundle();
        bundle.putString(TangoAreaDescriptionMetaData.KEY_NAME, "New ADF");
        // UUID is generated after the ADF is saved.
        bundle.putString(TangoAreaDescriptionMetaData.KEY_UUID, "");

        FragmentManager manager = getFragmentManager();
        SetAdfNameDialog setAdfNameDialog = new SetAdfNameDialog();
        setAdfNameDialog.setArguments(bundle);
        setAdfNameDialog.show(manager, "ADFNameDialog");
    }

    /**
     * Shows a dialog for navigation status.
     */
    private void showTargetFloorConfirmDialog(Bundle bundle) {

        FragmentManager manager = getFragmentManager();
        TargetFloorConfirmDialog targetFloorConfirmDialog = new TargetFloorConfirmDialog();
        targetFloorConfirmDialog.setArguments(bundle);
        targetFloorConfirmDialog.show(manager, "TargetFloorConfirmDialog");
    }

    /**
     * Implements TargetFloorConfirmDialog.CallbackListener.
     */
    @Override
    public void onTargetFloorConfirm(final int currFloor, final PointWithADFInfo targetPointInfo) {

        if (targetPointInfo != null) {
            if (!mUUID.equals(targetPointInfo.getADFInfo().getUUID())) {
                Toast.makeText(this, targetPointInfo.getADFInfo().getADFName() + ", " + targetPointInfo.getPointInfo().getPointName() , Toast.LENGTH_LONG).show();
                doOnPauseTasks();
                doOnResumeTasks();
                mPoses[0] = null; // reset pose
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    int retryCount = 1;
                    while (retryCount <= 100 && devicePoseFromMemory() == null) {
                        try {
                            Thread.sleep(UPDATE_INTERVAL_MS);
                            Log.i(TAG, "retry to get device pose : " + retryCount);
                            retryCount++;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (devicePoseFromMemory() != null) {
                        startNavigation(currFloor, targetPointInfo);
                    } else {
                        Toast.makeText(getApplicationContext(), "Can not get Device Pose" , Toast.LENGTH_LONG).show();
                    }
                }
            }).start();
        }
    }

    /**
     * Implements NavigationStatusDialog.CallbackListener.
     */
    @Override
    public void onNavigationStatusGotIt() {
        PointWithADFInfo endPointWithADFInfo = SharedPreferenceHelper.getEndPointInfo(getApplicationContext());
        if (endPointWithADFInfo != null) {
            Bundle bundle = new Bundle();
            bundle.putString(NavigationStatusDialog.ADF_UUID, endPointWithADFInfo.getADFInfo().getUUID());
            bundle.putString(NavigationStatusDialog.POINT_ID, endPointWithADFInfo.getPointInfo().getId());
            showTargetFloorConfirmDialog(bundle);
        }
    }

    /**
     * Shows a dialog for navigation status.
     */
    private void showNavigationStatusDialog(Bundle bundle) {

        FragmentManager manager = getFragmentManager();
        NavigationStatusDialog navigationStatusDialog = new NavigationStatusDialog();
        navigationStatusDialog.setArguments(bundle);
        navigationStatusDialog.show(manager, "NavigationStatusDialog");
    }

    public void onEventMainThread(Event event){
        if(event.getEventString().equals(Event.ON_NAVIGATION_FINISHED)){
            PointWithADFInfo endPointWithADFInfo = SharedPreferenceHelper.getEndPointInfo(getApplicationContext());
            Bundle bundle = new Bundle();
            if (endPointWithADFInfo != null) {
                Log.i(TAG, "[ON_NAVIGATION_FINISHED]endPointWithADFInfo = " + endPointWithADFInfo.toString());
                //Toast.makeText(AreaDescriptionActivity.this, endPointWithADFInfo.toString(), Toast.LENGTH_LONG).show();
                bundle.putBoolean(NavigationStatusDialog.IS_NAVIGATION_FINISHED_STRING, false);
                bundle.putString(NavigationStatusDialog.ADF_UUID, endPointWithADFInfo.getADFInfo().getUUID());
                bundle.putString(NavigationStatusDialog.POINT_ID, endPointWithADFInfo.getPointInfo().getId());
            } else {
                //Toast.makeText(AreaDescriptionActivity.this, "Navigation is finished!", Toast.LENGTH_LONG).show();
                bundle.putBoolean(NavigationStatusDialog.IS_NAVIGATION_FINISHED_STRING, true);
            }
            showNavigationStatusDialog(bundle);
        }
    }

    private void startNavigation(int currFloor, PointWithADFInfo targetPointInfo) {

        if (targetPointInfo != null && currFloor != 0) {
            SharedPreferenceHelper.setEndPointInfo(getApplicationContext(), null); //reset preference
            mPointName = targetPointInfo.getPointInfo().getPointName();

            Log.i(TAG, "[startNavigation]targetPointInfo.toString() = " + targetPointInfo.toString());
            List<PointInfo> pointList = mPointDao.queryAllPointsByUUID(mUUID);
            Log.i(TAG, "[startNavigation]pointList.size() = " + pointList.size());
            for (PointInfo point : pointList) {
                //Log.i(TAG, "[startNavigation]pointInfo = " + point.toString());
                Vector3 v = new Vector3(point.getCoordinate().x, point.getCoordinate().y, point.getCoordinate().z);
                Quaternion q =  new Quaternion(point.getQuaternion().w, point.getQuaternion().x, point.getQuaternion().y, point.getQuaternion().z);
                if (mIsDebug) {
                    renderer.addPointsWithGrid(v, q, extrinsics);
                } else {
                    renderer.addPoints(v, q, extrinsics);
                }
            }

            TangoPoseData startPoseData = devicePoseFromMemory();
            renderer.setStartPoint(startPoseData, extrinsics);

            if (currFloor == targetPointInfo.getADFInfo().getFloor()) {
                Coordinate coordinate = targetPointInfo.getPointInfo().getCoordinate();
                Vector3 v = new Vector3(coordinate.x, coordinate.y, coordinate.z);
                Quaternion q =  new Quaternion(targetPointInfo.getPointInfo().getQuaternion().w, targetPointInfo.getPointInfo().getQuaternion().x,
                                                targetPointInfo.getPointInfo().getQuaternion().y, targetPointInfo.getPointInfo().getQuaternion().z);
                renderer.setEndPoint(v, q, extrinsics);
            } else {
                Log.i(TAG, "[startNavigation]target point is at different floor with current floor.");

                SharedPreferenceHelper.setEndPointInfo(getApplicationContext(), targetPointInfo);

                List<PointInfo> stairAndElevatorPointsList = mPointDao.queryAllStairAndElevatorPointsByUUID(mUUID);
                if (stairAndElevatorPointsList.size() > 0) {
                    if (stairAndElevatorPointsList.size() == 1) {
                        Log.i(TAG, "[startNavigation]only one stair or elevator : " + stairAndElevatorPointsList.get(0).toString());
                        Coordinate coordinate = stairAndElevatorPointsList.get(0).getCoordinate();
                        Vector3 v = new Vector3(coordinate.x, coordinate.y, coordinate.z);
                        Quaternion q =  new Quaternion(targetPointInfo.getPointInfo().getQuaternion().w, targetPointInfo.getPointInfo().getQuaternion().x,
                                targetPointInfo.getPointInfo().getQuaternion().y, targetPointInfo.getPointInfo().getQuaternion().z);
                        renderer.setEndPoint(v, q, extrinsics);
                    } else {
                        /*if (Math.abs(currFloor - targetPointInfo.getADFInfo().getFloor()) > 2) {
                            Log.i(TAG, "[onGoWhereOk]diff floor is more than 2.");

                        } else {
                            Log.i(TAG, "[onGoWhereOk]diff floor is less than or equals 2.");
                        }*/
                        PointInfo nearestPoint = null;
                        double minDistance = 0.0;
                        Coordinate tempStartCoordinate = TangoUtil.coordinate(startPoseData);
                        for (int i = 0; i < stairAndElevatorPointsList.size(); i++) {
                            Coordinate tempEndCoordinate = stairAndElevatorPointsList.get(i).getCoordinate();
                            double tempDistance = tempStartCoordinate.distance(tempEndCoordinate);
                            if (i == 0) {
                                minDistance = tempDistance;
                                nearestPoint = stairAndElevatorPointsList.get(i);
                            } else {
                                if (tempDistance < minDistance) {
                                    minDistance = tempDistance;
                                    nearestPoint = stairAndElevatorPointsList.get(i);
                                }
                            }
                        }
                        if (nearestPoint != null) {
                            Log.i(TAG, "[startNavigation]nearestPoint = " + nearestPoint.toString());

                            Coordinate coordinate = nearestPoint.getCoordinate();
                            Vector3 v = new Vector3(coordinate.x, coordinate.y, coordinate.z);
                            Quaternion q =  new Quaternion(targetPointInfo.getPointInfo().getQuaternion().w, targetPointInfo.getPointInfo().getQuaternion().x,
                                    targetPointInfo.getPointInfo().getQuaternion().y, targetPointInfo.getPointInfo().getQuaternion().z);
                            renderer.setEndPoint(v, q, extrinsics);
                        }
                    }
                } else {
                    Log.w(TAG, "[startNavigation]no stairs or elevators.");
                }
            }
        } else {
            Log.e(TAG, "[startNavigation]targetPointInfo is null or currFloor is 0");
        }
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
                Toast.makeText(AreaDescriptionActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void logPose(TangoPoseData pose, boolean isRelocalized, double deltaTime, double mPreviousPoseTimeStamp, double mTimeToNextUpdate) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("baseFrame : " + pose.baseFrame);
        stringBuilder.append(", targetFrame : " + pose.targetFrame);
        stringBuilder.append(", isRelocalized : " + isRelocalized);
        stringBuilder.append(", deltaTime : " + deltaTime);
        stringBuilder.append(", mPreviousPoseTimeStamp : " + mPreviousPoseTimeStamp);
        stringBuilder.append(", mTimeToNextUpdate : " + mTimeToNextUpdate);

        Log.i(TAG, stringBuilder.toString());

    }

    private void logPose(TangoPoseData pose) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("baseFrame : " + pose.baseFrame);
        stringBuilder.append(", targetFrame : " + pose.targetFrame);
        stringBuilder.append(", timestamp : " + pose.timestamp);

        float translation[] = pose.getTranslationAsFloats();
        stringBuilder.append(", Position: " +
                translation[0] + ",             " + translation[1] + ",           " + translation[2]);

        //float orientation[] = pose.getRotationAsFloats();
        //stringBuilder.append(". Orientation: " +
        //        orientation[0] + ", " + orientation[1] + ", " +
        //        orientation[2] + ", " + orientation[3]);

        Log.i(TAG, stringBuilder.toString());
    }

    private void logFrame(TangoPoseData pose) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("baseFrame : " + pose.baseFrame);
        stringBuilder.append(", targetFrame : " + pose.targetFrame);

        Log.i(TAG, stringBuilder.toString());

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
                        ActivityCompat.requestPermissions(AreaDescriptionActivity.this,
                                new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    }
                })
                .create();
        dialog.show();
    }
}
