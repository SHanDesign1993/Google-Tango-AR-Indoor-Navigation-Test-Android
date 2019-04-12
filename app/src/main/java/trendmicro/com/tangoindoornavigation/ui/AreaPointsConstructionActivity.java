package trendmicro.com.tangoindoornavigation.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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
import com.projecttango.tangosupport.TangoSupport;
import com.vividsolutions.jts.geom.Coordinate;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import  trendmicro.com.tangoindoornavigation.R;
import  trendmicro.com.tangoindoornavigation.ScenePreFrameCallbackAdapter;
import trendmicro.com.tangoindoornavigation.data.RenderCameraPoseResult;
import trendmicro.com.tangoindoornavigation.db.ADFDao;
import  trendmicro.com.tangoindoornavigation.db.PointDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import  trendmicro.com.tangoindoornavigation.db.dto.PointInfo;
import  trendmicro.com.tangoindoornavigation.dialog.WaypointNameDialog;
import trendmicro.com.tangoindoornavigation.eventbus.Event;
import trendmicro.com.tangoindoornavigation.preference.NavigationSharedPreference;
import  trendmicro.com.tangoindoornavigation.rendering.SceneRenderer;
import trendmicro.com.tangoindoornavigation.ui.custom.MapView;
import trendmicro.com.tangoindoornavigation.util.TangoUtil;

import static trendmicro.com.tangoindoornavigation.rendering.SceneRenderer.QUAD_TREE_RANGE;
import static trendmicro.com.tangoindoornavigation.ui.custom.MapView.LINE_POINT_COUNT;
import static trendmicro.com.tangoindoornavigation.ui.custom.MapView.RECTANGLE_POINT_COUNT;
import static trendmicro.com.tangoindoornavigation.util.TangoUtil.getDeviceExtrinsics;

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


public class AreaPointsConstructionActivity extends Activity implements WaypointNameDialog.CallbackListener{

    private static final String TAG = AreaPointsConstructionActivity.class.getSimpleName();

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int CAMERA_PERMISSION_CODE = 0;

    private Tango mTango;
    private TangoConfig mConfig;
    private TextView mUuidTextView;
    private TextView mRelocalizationTextView;
    private TextView mLoadADFTextView;
    private TextView mLearningModeTextView;
    private TextView mDevicePoseTextView;
    private RajawaliSurfaceView mainSurfaceView;
    private Button mMarkPointButton;
    private Button mStartScanButton;
    private ToggleButton mMarkDeletedPointsToggleButton;
    private ToggleButton mMarkFilledPointsToggleButton;
    private ToggleButton mDeletedPointModeToggleButton;
    private MapView mapView;

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

    private static final int UPDATE_INTERVAL_MS = 100;
    private TangoPoseData[] mPoses;

    private final Object mSharedLock = new Object();
    private String mUUID = "";
    private int mBuildingID;
    private PointDao mPointDao;
    private ADFDao mADFDao;
    private boolean mStartScan = false;
    private boolean mMarkedforDelete = false;
    private EventBus mEventBus;
    private int mCleanPointsClickCount = 0;


    private boolean mIsPermissionReady = false;

    // This changes the Camera Texture and Intrinsics
    protected static final int ACTIVE_CAMERA_INTRINSICS = TangoCameraIntrinsics.TANGO_CAMERA_COLOR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_points_construction);
        Intent intent = getIntent();
        mIsLearningMode = intent.getBooleanExtra(MainActivity.USE_AREA_LEARNING, false);
        mIsConstantSpaceRelocalize = intent.getBooleanExtra(MainActivity.LOAD_ADF, false);
        mUUID = intent.getStringExtra(MainActivity.LOAD_ADF_UUID);
        mBuildingID = Integer.parseInt(intent.getStringExtra(MainActivity.BUILDING));

        mainSurfaceView = (RajawaliSurfaceView) findViewById(R.id.gl_main_surface_view);
        mUuidTextView = (TextView) findViewById(R.id.adf_uuid_textview);
        mRelocalizationTextView = (TextView) findViewById(R.id.relocalization_textview);
        mLoadADFTextView = (TextView) findViewById(R.id.load_adf_textview);
        mLearningModeTextView = (TextView) findViewById(R.id.learning_mode_textview);
        mDevicePoseTextView = (TextView) findViewById(R.id.device_pose_textview);
        mMarkPointButton = (Button) findViewById(R.id.mark_point);
        mStartScanButton = (Button) findViewById(R.id.start_scan);
        mMarkDeletedPointsToggleButton = (ToggleButton) findViewById(R.id.mark_deleted_points);
        mMarkFilledPointsToggleButton = (ToggleButton) findViewById(R.id.mark_filled_points);
        mDeletedPointModeToggleButton = (ToggleButton) findViewById(R.id.delete_point_mode);
        mapView = (MapView) findViewById(R.id.map_view);
        renderer = new SceneRenderer(this);
        renderer.renderVirtualObjects(true);
        mainSurfaceView.setSurfaceRenderer(renderer);
        mainSurfaceView.setZOrderOnTop(false);
        mapView.setFloorPlanData(renderer.getFloorPlanData());

        mPoses = new TangoPoseData[3];

        mADFDao = new ADFDao(getApplicationContext());
        mPointDao = new PointDao(getApplicationContext());

        mEventBus = EventBus.getDefault();
        mEventBus.register(this);

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

        if (mIsPermissionReady) {

            // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
            // in onPause, this will unbind Tango Service, so every time onResume gets called we
            // should create a new Tango object.
            mTango = new Tango(AreaPointsConstructionActivity.this, new Runnable() {
                // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
                // will be running on a new thread.
                // When Tango is ready, we can call Tango functions safely here only when there are no
                // UI thread changes involved.
                @Override
                public void run() {
                    synchronized (AreaPointsConstructionActivity.this) {
                        if (tangoIsConnected.compareAndSet(false, true)) {
                            try {
                                mConfig = setTangoConfig(
                                        mTango, mIsLearningMode, mIsConstantSpaceRelocalize);
                                mTango.connect(mConfig);
                                startupTango();
                                TangoSupport.initialize(mTango);
                                setupCameraProperties(mTango);
                                connectRenderer();
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
                            synchronized (AreaPointsConstructionActivity.this) {
                                setupTextViewsAndButtons(mTango, mIsLearningMode,
                                        mIsConstantSpaceRelocalize);
                            }
                        }
                    });
                }
            });
            mCleanPointsClickCount = 0;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Clear the relocalization state; we don't know where the device will be since our app
        // will be paused.
        mIsRelocalized = false;
        synchronized (this) {
            if (tangoIsConnected.compareAndSet(true, false)) {
                try {

                    renderer.getCurrentScene().clearFrameCallbacks();
                    mTango.disconnectCamera(ACTIVE_CAMERA_INTRINSICS);
                    connectedTextureId = INVALID_TEXTURE_ID;
                    mTango.disconnect();
                } catch (TangoErrorException e) {
                    Log.e(TAG, getString(R.string.tango_error), e);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEventBus.unregister(this);
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
            mLearningModeTextView.setText("True");
        } else {
            // Hide to save ADF button if leanring mode is off.
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
            mMarkPointButton.setVisibility(View.VISIBLE);
            mStartScanButton.setVisibility(View.VISIBLE);
            mMarkPointButton.setEnabled(false);
            mStartScanButton.setEnabled(false);
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
            ArrayList<String> fullUuidList;
            // Returns a list of ADFs with their UUIDs.
            fullUuidList = tango.listAreaDescriptions();
            // Load the latest ADF if ADFs are found.
            if (fullUuidList.size() > 0) {

                //mUUID = fullUuidList.get(fullUuidList.size() - 1);
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, mUUID);
            }
        }
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true);

        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     */
    private void startupTango() {

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
                    mainSurfaceView.requestRender();
                    //Log.i(TAG, "[onFrameAvailable]TANGO_CAMERA_COLOR");
                } else {
                    Log.i(TAG, "[onFrameAvailable]cameraId = " + cameraId);
                }
            }
        });
    }



    protected void setupCameraProperties(Tango tango) {
        extrinsics = getDeviceExtrinsics(tango, 0.0);
        intrinsics = tango.getCameraIntrinsics(ACTIVE_CAMERA_INTRINSICS);
    }

    protected void connectRenderer() {
        renderer.getCurrentScene().registerFrameCallback(new ScenePreFrameCallbackAdapter() {
            @Override
            public void onPreFrame(long sceneTime, double deltaTime) {
                synchronized (AreaPointsConstructionActivity.this) {
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


                        TangoPoseData devicePoseFromMemory = devicePoseFromMemory();
                        if (mStartScan && devicePoseFromMemory != null
                                && devicePoseFromMemory.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                                && devicePoseFromMemory.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {

                            //extrinsics = getDeviceExtrinsics(mTango, devicePoseFromMemory.timestamp);
                            boolean isDebug = NavigationSharedPreference.getEnableGrid(getApplicationContext());
                            RenderCameraPoseResult renderCameraPoseResult = null;
                            if (isDebug) {
                                renderCameraPoseResult = renderer.updateRenderCameraPoseWithGrid(devicePoseFromMemory, extrinsics, true);
                            } else {
                                renderCameraPoseResult = renderer.updateRenderCameraPose(devicePoseFromMemory, extrinsics, true);
                            }

                            if (renderCameraPoseResult != null) {

                                if (renderCameraPoseResult.getIsSetNewGrid()) {
                                    addPointInfoToDB(devicePoseFromMemory, renderCameraPoseResult);

                                } else {
                                    if (!mMarkedforDelete && renderer.getFloorPlanData().markedforDeleteVectorSet.contains(renderCameraPoseResult.getGridVector())) {
                                        renderer.getFloorPlanData().markedforDeleteVectorSet.remove(renderCameraPoseResult.getGridVector());
                                        addPointInfoToDB(devicePoseFromMemory, renderCameraPoseResult);
                                    }
                                }

                                if (mMarkedforDelete) {
                                    if (renderCameraPoseResult.getGridVector() != null && !renderer.getFloorPlanData().markedforDeleteVectorSet.contains(renderCameraPoseResult.getGridVector())) {
                                        renderer.getFloorPlanData().markedforDeleteVectorSet.add(renderCameraPoseResult.getGridVector());
                                        Log.i(TAG, "add vertor to set : " + renderCameraPoseResult.getGridVector().getX() + ", " + renderCameraPoseResult.getGridVector().getY());
                                        renderer.updateDeleteGridScene();
                                    }
                                }
                            }
                            cameraPoseTimestamp = devicePoseFromMemory.timestamp;
                        }
                    }
                }
            }
        });
    }

    private void addPointInfoToDB(TangoPoseData poseData, RenderCameraPoseResult renderCameraPoseResult) {

        Coordinate coordinate = new Coordinate(poseData.translation[0],
                poseData.translation[1], poseData.translation[2]);
        Quaternion quaternion = new Quaternion(poseData.rotation[3], poseData.rotation[0],
                poseData.rotation[1], poseData.rotation[2]);

        PointInfo pointInfo = new PointInfo();
        pointInfo.setPointName("");
        pointInfo.setUUID(mUUID);
        pointInfo.setType(PointInfo.PointType.GENERAL);
        pointInfo.setCoordinate(coordinate);
        pointInfo.setQuaternion(quaternion);
        pointInfo.setGridVector(renderCameraPoseResult.getGridVector());
        mPointDao.addPoint(pointInfo);
        Log.i(TAG, "[addPointInfoToDB] = " + coordinate.toString() + ", Quaternion = " + quaternion.toString() + ", getGridVector = " + renderCameraPoseResult.getGridVector().getX() + ", " + renderCameraPoseResult.getGridVector().getY());
    }

    private void addPointInfoToDB(Vector3 vector, Quaternion quaternion, RenderCameraPoseResult renderCameraPoseResult) {

        Coordinate coordinate = new Coordinate(vector.x, vector.y, vector.z);

        PointInfo pointInfo = new PointInfo();
        pointInfo.setPointName("");
        pointInfo.setUUID(mUUID);
        pointInfo.setType(PointInfo.PointType.GENERAL);
        pointInfo.setCoordinate(coordinate);
        pointInfo.setQuaternion(quaternion);
        pointInfo.setGridVector(renderCameraPoseResult.getGridVector());
        mPointDao.addPoint(pointInfo);
        Log.i(TAG, "[addPointInfoToDB] = " + coordinate.toString() + ", Quaternion = " + quaternion.toString() + ", getGridVector = " + renderCameraPoseResult.getGridVector().getX() + ", " + renderCameraPoseResult.getGridVector().getY());
    }

    private TangoPoseData devicePoseFromMemory() {
        return mPoses[0];
    }

    private void updateTextViews() {
        mMarkPointButton.setEnabled(mIsRelocalized);
        mStartScanButton.setEnabled(mIsRelocalized);
        mRelocalizationTextView.setText(mIsRelocalized ?
                getString(R.string.localized) :
                getString(R.string.not_localized));
        final TangoPoseData devicePoseFromMemory = devicePoseFromMemory();
        if (devicePoseFromMemory != null
                && devicePoseFromMemory.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                && devicePoseFromMemory.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {
            float translation[] = devicePoseFromMemory.getTranslationAsFloats();
            mDevicePoseTextView.setText("Position: " +
                    translation[0] + ", " + translation[1] + ", " + translation[2]);

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

    private void addExistingPoints() {
        List<PointInfo> pointList = mPointDao.queryAllPointsByUUID(mUUID);
        Log.i(TAG, "[addExistingPoints]pointList.size() = " + pointList.size());
        boolean isDebug = NavigationSharedPreference.getEnableGrid(getApplicationContext());
        for (PointInfo point : pointList) {
            Log.i(TAG, "[addExistingPoints]pointInfo = " + point.toString());
            Vector3 v = new Vector3(point.getCoordinate().x, point.getCoordinate().y, point.getCoordinate().z);
            Quaternion q =  new Quaternion(point.getQuaternion().w, point.getQuaternion().x, point.getQuaternion().y, point.getQuaternion().z);
            if (isDebug) {
                renderer.addPointsWithGrid(v, q, extrinsics);
                if (!point.getPointName().equals("")) {
                    renderer.addNamingPointCube(point.getGridVector());
                    mapView.addNamingVector(point.getGridVector());
                }
            } else {
                renderer.addPoints(v, q, extrinsics);
            }
        }
    }

    public void startScanClicked(View view) {
        addExistingPoints();
        Toast.makeText(this, "ready to scan points", Toast.LENGTH_LONG).show();
        mStartScan = true;
    }

    public void clearPointsClicked(View view) {
        Log.i(TAG, "[clearPointsClicked] in");
        mCleanPointsClickCount++;
        if (mCleanPointsClickCount >= 5) {
            mStartScan = false;
            mPointDao.removeAllPointsByUUID(mUUID);
            renderer.resetFloorPlanScene();
            mapView.setFloorPlanData(renderer.getFloorPlanData());
            Toast.makeText(this, "All points are deleted!", Toast.LENGTH_LONG).show();
            Log.i(TAG, "[clearPointsClicked] resetFloorPlanScene");
            mCleanPointsClickCount = 0;
        }
    }

    public void deletePointModeClicked(View view) {
        Log.i(TAG, "[deletePointModeClicked] in");
        boolean isDeleteMode = mDeletedPointModeToggleButton.isChecked();
        mapView.setDeleteMode(isDeleteMode);

        if (isDeleteMode) {
            Toast.makeText(this, "enable delete point mode", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "disable delete point mode", Toast.LENGTH_LONG).show();
        }

        Log.i(TAG, "[deletePointModeClicked] isDeleteMode = " + isDeleteMode);
    }

    public void markDeletedPointsClicked(View view) {

        mMarkedforDelete = mMarkDeletedPointsToggleButton.isChecked();
        if (mMarkedforDelete) {
            renderer.getFloorPlanData().isMarkForDeleteEnabled = true;
            Toast.makeText(this, "Start to mark deleted points", Toast.LENGTH_LONG).show();
        } else {
            mStartScan = false;
            renderer.getFloorPlanData().isMarkForDeleteEnabled = false;
            Toast.makeText(this, "Stop to mark deleted points", Toast.LENGTH_LONG).show();

            for (Vector2 vector : renderer.getFloorPlanData().markedforDeleteVectorSet) {
                Log.i(TAG, "mMarkedforDeleteVectorSet : vector : " + vector.getX() + ", " + vector.getY());
                mPointDao.removePointByGridVectorAndUUID(vector, mUUID);
                renderer.deleteNamingPointCube(vector);
            }

            renderer.removeDeleteGridScene();
            mStartScan = true;
        }
    }

    public void markfilledPointsClicked(View view) {

        Log.i(TAG, "[markfilledPointsClicked] in");
        boolean isFilledMode = mMarkFilledPointsToggleButton.isChecked();
        mapView.setMarkFilledPointsMode(isFilledMode);

        if (isFilledMode) {
            Toast.makeText(this, "enable mark filled points mode", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "disable mark filled points mode", Toast.LENGTH_LONG).show();
        }

        Log.i(TAG, "[markfilledPointsClicked] isFilledMode = " + isFilledMode);
    }

    public void autoFilledPointsClicked(View view) {
        Log.i(TAG, "[autoFilledPointsClicked] in");
        Map<String,Vector2> markedforFillVectorMap = mapView.getMarkedforFillVectorMap();

        if (markedforFillVectorMap.size() == RECTANGLE_POINT_COUNT) {
            double quadTreeRangeUnit = getQuadTreeRange(8, QUAD_TREE_RANGE);
            Log.i(TAG, "[autoFilledPointsClicked]quadTreeRangeUnit = " + quadTreeRangeUnit + ", size = " + RECTANGLE_POINT_COUNT);
            int width = Math.abs((int)(markedforFillVectorMap.get("rightTopVector").getX() / quadTreeRangeUnit) - (int)(markedforFillVectorMap.get("leftTopVector").getX() / quadTreeRangeUnit));
            int height = Math.abs((int)(markedforFillVectorMap.get("rightTopVector").getY() / quadTreeRangeUnit) - (int)(markedforFillVectorMap.get("rightBottomVector").getY() / quadTreeRangeUnit));
            Log.i(TAG, "[autoFilledPointsClicked]width = " + width + ", height = " + height);

            if (width >= 2 && height >= 2) {
                PointInfo rightTopPoint = mPointDao.queryPointByGridVectorAndUUID(markedforFillVectorMap.get("rightTopVector"), mUUID);
                PointInfo leftTopPoint = mPointDao.queryPointByGridVectorAndUUID(markedforFillVectorMap.get("leftTopVector"), mUUID);
                PointInfo leftBottomPoint = mPointDao.queryPointByGridVectorAndUUID(markedforFillVectorMap.get("leftBottomVector"), mUUID);
                PointInfo rightBottomPoint = mPointDao.queryPointByGridVectorAndUUID(markedforFillVectorMap.get("rightBottomVector"), mUUID);

                if (rightTopPoint != null && leftTopPoint != null && leftBottomPoint != null && rightBottomPoint != null) {

                    ArrayList<PointInfo> pointInfoList = new ArrayList<>();
                    pointInfoList.add(rightTopPoint);
                    pointInfoList.add(leftTopPoint);
                    pointInfoList.add(leftBottomPoint);
                    pointInfoList.add(rightBottomPoint);
                    Log.i(TAG, "[autoFilledPointsClicked]rightTopPoint = " + rightTopPoint.toString());
                    Log.i(TAG, "[autoFilledPointsClicked]leftTopPoint = " + leftTopPoint.toString());
                    Log.i(TAG, "[autoFilledPointsClicked]leftBottomPoint = " + leftBottomPoint.toString());
                    Log.i(TAG, "[autoFilledPointsClicked]rightBottomPoint = " + rightBottomPoint.toString());

                    double widthUnit = (rightBottomPoint.getCoordinate().x - leftBottomPoint.getCoordinate().x) / width;
                    double heightUnit = (rightTopPoint.getCoordinate().y - rightBottomPoint.getCoordinate().y) / height;
                    double averageCoordinateZ = getAverageCoordinateZ(pointInfoList);
                    Quaternion averageQuaternion = getAverageQuaternion(pointInfoList);

                    int x = 1;
                    for (int wIndex = 1; wIndex < width; wIndex++) {
                        for (int hIndex = 1; hIndex < height; hIndex++) {
                            Log.i(TAG, "[autoFilledPointsClicked]x = " + x);
                            Vector3 v = new Vector3((leftBottomPoint.getCoordinate().x + (widthUnit * wIndex)), (rightBottomPoint.getCoordinate().y + (heightUnit * hIndex)), averageCoordinateZ);
                            RenderCameraPoseResult renderCameraPoseResult = renderer.updateRenderCameraPoseWithGrid(v, averageQuaternion, extrinsics, true);
                            Log.i(TAG, "[autoFilledPointsClicked]getIsSetNewGrid = " + renderCameraPoseResult.getIsSetNewGrid());
                            //Log.i(TAG, "[autoFilledPointsClicked]getCameraPose = " + renderCameraPoseResult.getCameraPose().toString());
                            Log.i(TAG, "[autoFilledPointsClicked]getGridVector = " + renderCameraPoseResult.getGridVector().getX() + ", " + renderCameraPoseResult.getGridVector().getY());
                            if (renderCameraPoseResult.getIsSetNewGrid()) {
                                addPointInfoToDB(v, averageQuaternion, renderCameraPoseResult);
                            }
                            x++;
                        }
                    }
                }
            }
        } else if (markedforFillVectorMap.size() == LINE_POINT_COUNT) {
            double quadTreeRangeUnit = getQuadTreeRange(8, QUAD_TREE_RANGE);
            Log.i(TAG, "[autoFilledPointsClicked]quadTreeRangeUnit = " + quadTreeRangeUnit + ", size = " + LINE_POINT_COUNT);

            Log.i(TAG, "[autoFilledPointsClicked]startVector = " + markedforFillVectorMap.get("startVector").getX() + ", " + markedforFillVectorMap.get("startVector").getY());
            Log.i(TAG, "[autoFilledPointsClicked]endVector = " + markedforFillVectorMap.get("endVector").getX() + ", " + markedforFillVectorMap.get("endVector").getY());
            PointInfo startPoint = mPointDao.queryPointByGridVectorAndUUID(markedforFillVectorMap.get("startVector"), mUUID);
            PointInfo endPoint = mPointDao.queryPointByGridVectorAndUUID(markedforFillVectorMap.get("endVector"), mUUID);

            if (startPoint != null && endPoint != null) {
                ArrayList<PointInfo> pointInfoList = new ArrayList<>();
                pointInfoList.add(startPoint);
                pointInfoList.add(endPoint);
                int length = 0;
                double averageCoordinateZ = getAverageCoordinateZ(pointInfoList);
                Quaternion averageQuaternion = getAverageQuaternion(pointInfoList);
                if (markedforFillVectorMap.get("startVector").getX() == markedforFillVectorMap.get("endVector").getX()) {
                    length = Math.abs((int)(markedforFillVectorMap.get("startVector").getY() / quadTreeRangeUnit) - (int)(markedforFillVectorMap.get("endVector").getY() / quadTreeRangeUnit));
                    Log.i(TAG, "lengthX = " + length);
                    if (length > 1) {
                        double yUnit = (endPoint.getCoordinate().y - startPoint.getCoordinate().y) / length;
                        for (int index = 1; index < length; index++) {
                            Log.i(TAG, "[autoFilledPointsClicked]index = " + index);
                            Vector3 v = new Vector3(startPoint.getCoordinate().x, (startPoint.getCoordinate().y + (yUnit * index)), averageCoordinateZ);
                            RenderCameraPoseResult renderCameraPoseResult = renderer.updateRenderCameraPoseWithGrid(v, averageQuaternion, extrinsics, true);
                            Log.i(TAG, "[autoFilledPointsClicked]getIsSetNewGrid = " + renderCameraPoseResult.getIsSetNewGrid());
                            //Log.i(TAG, "[autoFilledPointsClicked]getCameraPose = " + renderCameraPoseResult.getCameraPose().toString());
                            Log.i(TAG, "[autoFilledPointsClicked]getGridVector = " + renderCameraPoseResult.getGridVector().getX() + ", " + renderCameraPoseResult.getGridVector().getY());
                            if (renderCameraPoseResult.getIsSetNewGrid()) {
                                addPointInfoToDB(v, averageQuaternion, renderCameraPoseResult);
                            }
                        }
                    }
                } else {
                    length = Math.abs((int)(markedforFillVectorMap.get("startVector").getX() / quadTreeRangeUnit) - (int)(markedforFillVectorMap.get("endVector").getX() / quadTreeRangeUnit));
                    Log.i(TAG, "lengthY = " + length);
                    if (length > 1) {
                        double xUnit = (endPoint.getCoordinate().x - startPoint.getCoordinate().x) / length;
                        for (int index = 1; index < length; index++) {
                            Log.i(TAG, "[autoFilledPointsClicked]index = " + index);
                            Vector3 v = new Vector3((startPoint.getCoordinate().x + (xUnit * index)), startPoint.getCoordinate().y , averageCoordinateZ);
                            RenderCameraPoseResult renderCameraPoseResult = renderer.updateRenderCameraPoseWithGrid(v, averageQuaternion, extrinsics, true);
                            Log.i(TAG, "[autoFilledPointsClicked]getIsSetNewGrid = " + renderCameraPoseResult.getIsSetNewGrid());
                            //Log.i(TAG, "[autoFilledPointsClicked]getCameraPose = " + renderCameraPoseResult.getCameraPose().toString());
                            Log.i(TAG, "[autoFilledPointsClicked]getGridVector = " + renderCameraPoseResult.getGridVector().getX() + ", " + renderCameraPoseResult.getGridVector().getY());
                            if (renderCameraPoseResult.getIsSetNewGrid()) {
                                addPointInfoToDB(v, averageQuaternion, renderCameraPoseResult);
                            }
                        }
                    }
                }

            } else {
                Log.i(TAG, "points are null");
            }
        }
    }

    public void markPointClicked(View view) {
        showMarkPointDialog();
    }

    private void showMarkPointDialog() {
        Bundle bundle = new Bundle();
        TangoPoseData poseData = devicePoseFromMemory();
        logPose(poseData);
        Vector2 filledVector = renderer.getFilledPositionByPose(poseData, extrinsics);
        if (filledVector != null) {
            Log.i(TAG, "[showMarkPointDialog] filledVector = " + filledVector.getX() + ", " + filledVector.getY());
            bundle.putSerializable(WaypointNameDialog.VECTOR_X_KEY, filledVector.getX());
            bundle.putSerializable(WaypointNameDialog.VECTOR_Y_KEY, filledVector.getY());
            FragmentManager manager = getFragmentManager();
            WaypointNameDialog setADFNameDialog = new WaypointNameDialog();
            setADFNameDialog.setArguments(bundle);
            setADFNameDialog.show(manager, "WaypointNameDialog");
        } else {
            Log.w(TAG, "[showMarkPointDialog]filledVector is null");
        }
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onMarkPointOk(String name, Vector2 gridVector, PointInfo.PointType type) {
        Log.i(TAG, "onMarkPointOk : " + name + ", type = " + type);

        synchronized (AreaPointsConstructionActivity.this) {

            if (gridVector != null) {
                PointInfo pointInfo = mPointDao.queryPointByGridVectorAndUUID(gridVector, mUUID);
                if (pointInfo != null) {
                    pointInfo.setPointName(name);
                    pointInfo.setUUID(pointInfo.getUUID());
                    pointInfo.setType(type);
                    pointInfo.setCoordinate(pointInfo.getCoordinate());
                    pointInfo.setQuaternion(pointInfo.getQuaternion());
                    pointInfo.setGridVector(pointInfo.getGridVector());
                    mPointDao.updatePointByItem(pointInfo);
                    renderer.addNamingPointCube(gridVector);
                    mapView.addNamingVector(gridVector);
                    Log.i(TAG, "[onMarkPointOk]updatePointByItem = " + pointInfo.getCoordinate().toString() + ", Quaternion = " + pointInfo.getQuaternion().toString() + ", GridVector = " + pointInfo.getGridVector().getX() + ", " + pointInfo.getGridVector().getY());
                }
            }
        }
    }

    public void onEventMainThread(Event event){
        if(event.getEventString().equals(Event.ON_DELETE_GRID_VECTOR)){
            Bundle bundle = event.getBundle();
            Vector2 gridVector = new Vector2(bundle.getDouble(MapView.DELETE_GRID_VECTOR_X), bundle.getDouble(MapView.DELETE_GRID_VECTOR_Y));
            if (gridVector != null) {
                Log.i(TAG, "[ON_DELETE_GRID_VECTOR]gridVector = " + gridVector.getX() + ", " + gridVector.getY());
                mPointDao.removePointByGridVectorAndUUID(gridVector, mUUID);
                renderer.deleteNamingPointCube(gridVector);
            }
        }
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onMarkPointCancelled() {
        // Continue running.
    }

    private Quaternion getAverageQuaternion(ArrayList<PointInfo> pointInfoList) {

        double quaternionW = 0.0;
        double quaternionX = 0.0;
        double quaternionY = 0.0;
        double quaternionZ = 0.0;

        for (int x = 0; x < pointInfoList.size(); x++) {
            quaternionW += pointInfoList.get(x).getQuaternion().w;
            quaternionX += pointInfoList.get(x).getQuaternion().x;
            quaternionY += pointInfoList.get(x).getQuaternion().y;
            quaternionZ += pointInfoList.get(x).getQuaternion().z;
        }
        return new Quaternion(quaternionW/pointInfoList.size(), quaternionX/pointInfoList.size(),
                quaternionY/pointInfoList.size(), quaternionZ/pointInfoList.size());
    }

    private double getAverageCoordinateZ(ArrayList<PointInfo> pointInfoList) {

        double coordinateZ = 0.0;

        for (int x = 0; x < pointInfoList.size(); x++) {
            coordinateZ += pointInfoList.get(x).getCoordinate().z;
        }
        return coordinateZ/pointInfoList.size();
    }

    private double getQuadTreeRange(int depth, double quadTreeRange) {
        if (depth == 0) {
            return quadTreeRange;
        } else {
            return getQuadTreeRange(depth - 1,  quadTreeRange / 2.0);
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
                Toast.makeText(AreaPointsConstructionActivity.this,
                        getString(resId), Toast.LENGTH_LONG).show();
                finish();
            }
        });
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
                        ActivityCompat.requestPermissions(AreaPointsConstructionActivity.this,
                                new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    }
                })
                .create();
        dialog.show();
    }

    private void logPose(TangoPoseData pose) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("baseFrame : " + pose.baseFrame);
        stringBuilder.append(", targetFrame : " + pose.targetFrame);
        stringBuilder.append(", timestamp : " + pose.timestamp);

        float translation[] = pose.getTranslationAsFloats();
        stringBuilder.append(", Position: " +
                translation[0] + ",             " + translation[1] + ",           " + translation[2]);

        float orientation[] = pose.getRotationAsFloats();
        //stringBuilder.append(". Orientation: " +
        //        orientation[0] + ", " + orientation[1] + ", " +
        //        orientation[2] + ", " + orientation[3]);

        Log.i(TAG, stringBuilder.toString());

    }
}

