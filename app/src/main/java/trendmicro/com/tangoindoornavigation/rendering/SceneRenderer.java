package  trendmicro.com.tangoindoornavigation.rendering;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.LinearInterpolator;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.rajawali.DeviceExtrinsics;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ScenePoseCalculator;
import com.vividsolutions.jts.geom.Coordinate;

import org.rajawali3d.animation.Animation;
import org.rajawali3d.animation.Animation3D;
import org.rajawali3d.animation.RotateOnAxisAnimation;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.renderer.RajawaliRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.microedition.khronos.opengles.GL10;

import de.greenrobot.event.EventBus;
import  trendmicro.com.tangoindoornavigation.R;
import  trendmicro.com.tangoindoornavigation.data.PathFinder;
import trendmicro.com.tangoindoornavigation.data.PathVector;
import  trendmicro.com.tangoindoornavigation.data.QuadTree;
import trendmicro.com.tangoindoornavigation.data.RenderCameraPoseResult;
import trendmicro.com.tangoindoornavigation.eventbus.Event;
import trendmicro.com.tangoindoornavigation.preference.NavigationSharedPreference;
import trendmicro.com.tangoindoornavigation.rendering.animation.CircleAnimation3D;
import trendmicro.com.tangoindoornavigation.rendering.animation.NavigationPathAnimation3D;
import trendmicro.com.tangoindoornavigation.util.TangoUtil;

import static trendmicro.com.tangoindoornavigation.util.TangoUtil.getSlope;

/**
 * Created by hugo on 24/07/2017.
 */
public class SceneRenderer extends RajawaliRenderer {
    public static int QUAD_TREE_START = -120;
    public static int QUAD_TREE_RANGE = 240;
    private static final String TAG = SceneRenderer.class.getSimpleName();
    private QuadTree data;
    // Rajawali texture used to render the Tango color camera
    private ATexture mTangoCameraTexture;
    // Keeps track of whether the scene camera has been configured
    private boolean mSceneCameraConfigured;

    private FloorPlan floorPlan;
    private Pose cameraPoseStartPoint;
    private Pose cameraPoseEndPoint;
    private HashMap<String, Cube> namginVectorCubes = new HashMap<>();
    private List<Cube> pathCubes = new ArrayList<>();
    private List<NavigationPath> navigationPaths = new ArrayList<>();
    private NavigationPath firstNavigationPathScene;
    private NavigationPath secondNavigationPathScene;
    private Circle circle;
    private List<Circle> circles = new ArrayList<>();
    private List<FloorPlanMarkedForDelete> floorPlanMarkedForDeleteList = new ArrayList<>();
    private boolean fillPath = false;
    private boolean mIsDebug = false;
    private double mDetectDistance = 1.0;
    private Material blue;
    private Material red;
    private Material yellow;
    private Material right;
    private Material left;
    private boolean renderVirtualObjects;
    private Cube logo;
    private boolean startNavigation = false;
    private Vector2 currentV;
    private Pose currentCameraPose;
    private Vector2 lastV;
    private Vector2 endV;
    private double mNavigationPathDistance = 0.0d;
    private double mAverageDepth = 0.0d;
    private QuadTree.QuadTreeDataListener quadTreeDataListener;
    private List<PathVector> mPathVectorList;
    private List<Vector2> mMnimumPathVectorList;
    public  Set<Vector2> mVisitedVectorSet = new HashSet<Vector2>();


    public SceneRenderer(Context context) {
        super(context);
        QUAD_TREE_START = NavigationSharedPreference.getQuadTreeStart(getContext());
        QUAD_TREE_RANGE = NavigationSharedPreference.getQuadTreeRange(getContext());
        data = new QuadTree(new Vector2(QUAD_TREE_START, QUAD_TREE_START), QUAD_TREE_RANGE, 8);
        mIsDebug = NavigationSharedPreference.getEnableGrid(context);
        mDetectDistance = NavigationSharedPreference.getDetectDistance(context);
    }

    @Override
    protected void initScene() {
        Log.i(TAG, "initScene in");
        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        reloadScene();

    }

    public void resetData() {
        data.clear();
        fillPath = false;
        startNavigation = false;
    }

    public void reloadScene() {
        ScreenQuad backgroundQuad = new ScreenQuad();
        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering
        mTangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture);
            backgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e);
        }
        getCurrentScene().addChildAt(backgroundQuad, 0);

        blue = new Material();
        blue.setColor(Color.BLUE);

        yellow = new Material();
        yellow.setColor(Color.YELLOW);

        red = new Material();
        red.setColor(Color.RED);

        right = new Material();
        right.setColor(Color.parseColor("#BD2BC5")); // purple

        left = new Material();
        left.setColor(Color.parseColor("#2BC557")); // green

        floorPlan = new FloorPlan(data);
        getCurrentScene().addChild(floorPlan);
        floorPlan.setVisible(renderVirtualObjects);
        quadTreeDataListener = data.getListener();

        // A floating Project Tango logo as a world reference.
        Material logoMaterial = new Material();
        logoMaterial.setColorInfluence(0);

        try {
            Texture t = new Texture("logo", R.drawable.tango_logo);
            logoMaterial.addTexture(t);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception generating logo texture", e);
        }

        logo = new Cube(0.5f);
        // Change the texture coordinates to be in the right position for the viewer.
        logo.getGeometry().setTextureCoords(new float[]
                {
                        1, 0, 0, 0, 0, 1, 1, 1, // THIRD
                        0, 0, 0, 1, 1, 1, 1, 0, // SECOND
                        0, 1, 1, 1, 1, 0, 0, 0, // FIRST
                        1, 0, 0, 0, 0, 1, 1, 1, // FOURTH
                        0, 1, 1, 1, 1, 0, 0, 0, // TOP
                        0, 1, 1, 1, 1, 0, 0, 0, // BOTTOM

                });
        // Update the buffers after changing the geometry.
        logo.getGeometry().changeBufferData(logo.getGeometry().getTexCoordBufferInfo(),
                logo.getGeometry().getTextureCoords(), 0);
        logo.rotate(Vector3.Axis.Y, 180);
        logo.setPosition(0, 0, 0);
        logo.setMaterial(logoMaterial);
        getCurrentScene().addChild(logo);
        logo.setVisible(false);

        // Rotate around its Y axis.
        Animation3D animLogo = new RotateOnAxisAnimation(Vector3.Axis.Y, 0, -360);
        animLogo.setInterpolator(new LinearInterpolator());
        animLogo.setDurationMilliseconds(6000);
        animLogo.setRepeatMode(Animation.RepeatMode.INFINITE);
        animLogo.setTransformable3D(logo);
        getCurrentScene().registerAnimation(animLogo);
        animLogo.play();

        /*Animation3D animCircle = new CircleAnimation3D();
            animCircle.setInterpolator(new LinearInterpolator());
            animCircle.setDurationMilliseconds(2000);
            animCircle.setRepeatMode(Animation.RepeatMode.NONE);
            animCircle.setTransformable3D(circle);
            getCurrentScene().registerAnimation(animCircle);

        NavigationPathAnimation3D animNavigationPath = new NavigationPathAnimation3D();
        animNavigationPath.setInterpolator(new LinearInterpolator());
        animNavigationPath.setDurationMilliseconds(2000);
        animNavigationPath.setRepeatMode(Animation.RepeatMode.NONE);
        animNavigationPath.setTransformable3D(firstNavigationPathScene);
        getCurrentScene().registerAnimation(animNavigationPath);*/
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The device pose should match the pose of the device at the time the last rendered RGB
     * frame, which can be retrieved with this.getTimestamp();
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    /*public void updateRenderCameraPose(TangoPoseData devicePose, DeviceExtrinsics extrinsics) {
        Pose cameraPose = ScenePoseCalculator.toOpenGlCameraPose(devicePose, extrinsics);
        getCurrentCamera().setRotation(cameraPose.getOrientation());
        getCurrentCamera().setPosition(cameraPose.getPosition());
        //floorPlan.setTrajectoryPosition(cameraPose.getPosition());
    }*/

    //public Vector3 updateRenderCameraPose(TangoPoseData devicePose, Coordinate coordinate, DeviceExtrinsics extrinsics) {
    public RenderCameraPoseResult updateRenderCameraPose(TangoPoseData devicePose, DeviceExtrinsics extrinsics, boolean returnGridVector) {

        RenderCameraPoseResult renderCameraPoseResult = new RenderCameraPoseResult(false, null, null);
        Pose cameraPose = ScenePoseCalculator.toOpenGlCameraPose(devicePose, extrinsics);
        getCurrentCamera().setRotation(cameraPose.getOrientation());
        getCurrentCamera().setPosition(cameraPose.getPosition());

        boolean isSet = floorPlan.setTrajectoryPosition(cameraPose.getPosition());
        if (startNavigation || returnGridVector) {
            currentV = floorPlan.getFilledPositionByPoint(cameraPose.getPosition());
            //Log.i(TAG, "[updateRenderCameraPoseWithGrid]currentV = " +currentV.getX() + ", y = " + currentV.getY());
            renderCameraPoseResult.setGridVector(currentV);
            if (quadTreeDataListener != null) {
                quadTreeDataListener.OnCurrVectorUpdate(currentV);
            }
        }
        currentCameraPose = cameraPose;
        renderCameraPoseResult.setIsSetNewGrid(isSet);
        renderCameraPoseResult.setCameraPose(cameraPose);

        return renderCameraPoseResult;
    }

    public RenderCameraPoseResult updateRenderCameraPoseWithGrid(TangoPoseData devicePose, DeviceExtrinsics extrinsics, boolean returnGridVector) {

        RenderCameraPoseResult renderCameraPoseResult = new RenderCameraPoseResult(false, null, null);
        Pose cameraPose = ScenePoseCalculator.toOpenGlCameraPose(devicePose, extrinsics);
        getCurrentCamera().setRotation(cameraPose.getOrientation());
        getCurrentCamera().setPosition(cameraPose.getPosition());

        boolean isSet = floorPlan.setTrajectoryPositionWithGrid(cameraPose.getPosition());
        if (startNavigation || returnGridVector) {
            currentV = floorPlan.getFilledPositionByPoint(cameraPose.getPosition());
            //Log.i(TAG, "[updateRenderCameraPoseWithGrid]currentV = " +currentV.getX() + ", y = " + currentV.getY());
            renderCameraPoseResult.setGridVector(currentV);
            if (quadTreeDataListener != null) {
                quadTreeDataListener.OnCurrVectorUpdate(currentV);
            }
        }
        currentCameraPose = cameraPose;
        renderCameraPoseResult.setIsSetNewGrid(isSet);
        renderCameraPoseResult.setCameraPose(cameraPose);

        return renderCameraPoseResult;
    }

    public RenderCameraPoseResult updateRenderCameraPoseWithGrid(Vector3 position, Quaternion quaternion, DeviceExtrinsics extrinsics, boolean returnGridVector) {

        RenderCameraPoseResult renderCameraPoseResult = new RenderCameraPoseResult(false, null, null);
        Pose cameraPose = ScenePoseCalculator.toOpenGlCameraPose(position, quaternion, extrinsics);
        getCurrentCamera().setRotation(cameraPose.getOrientation());
        getCurrentCamera().setPosition(cameraPose.getPosition());

        boolean isSet = floorPlan.setTrajectoryPositionWithGrid(cameraPose.getPosition());

        if (startNavigation || returnGridVector) {
            currentV = floorPlan.getFilledPositionByPoint(cameraPose.getPosition());
            //Log.i(TAG, "[updateRenderCameraPoseWithGrid]currentV = " +currentV.getX() + ", y = " + currentV.getY());
            renderCameraPoseResult.setGridVector(currentV);
            if (quadTreeDataListener != null) {
                quadTreeDataListener.OnCurrVectorUpdate(currentV);
            }
        }
        currentCameraPose = cameraPose;
        renderCameraPoseResult.setIsSetNewGrid(isSet);
        renderCameraPoseResult.setCameraPose(cameraPose);

        return renderCameraPoseResult;
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public int getTextureId() {
        return mTangoCameraTexture == null ? -1 : mTangoCameraTexture.getTextureId();
    }

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
        super.onRenderSurfaceSizeChanged(gl, width, height);
        mSceneCameraConfigured = false;
    }

    public boolean isSceneCameraConfigured() {
        return mSceneCameraConfigured;
    }

    /**
     * Sets the projection matrix for the scene camera to match the parameters of the color camera,
     * provided by the {@code TangoCameraIntrinsics}.
     */
    public void setProjectionMatrix(TangoCameraIntrinsics intrinsics) {
        Matrix4 projectionMatrix = ScenePoseCalculator.calculateProjectionMatrix(
                intrinsics.width, intrinsics.height,
                intrinsics.fx, intrinsics.fy, intrinsics.cx, intrinsics.cy);
        getCurrentCamera().setProjectionMatrix(projectionMatrix);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset,
                                 float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);

        if (startNavigation && currentV != null) {
            renderNavigationPath();
            lastV = currentV;
        }

        // add routing cubes to scene graph if available
        if (fillPath) {

            CleanSceneItems(true, true, true);
            PathFinder finder = new PathFinder(floorPlan.getData());
            try {
                Log.e(TAG, "cameraPoseStartPoint : " + cameraPoseStartPoint.getPosition().toString());
                Log.e(TAG, "cameraPoseEndPoint : " + cameraPoseEndPoint.getPosition().toString());
                Vector2 floorPlanStartPointV = floorPlan.getFilledPositionByPoint(cameraPoseStartPoint.getPosition());
                Vector2 floorPlanEndPointV = floorPlan.getFilledPositionByPoint(cameraPoseEndPoint.getPosition());
                //Log.e(TAG, "floorPlanStartPointV : " + floorPlanStartPointV.getX()/0.9375 + ", " + floorPlanEndPointV.getY()/0.9375 );
                //Log.e(TAG, "floorPlanEndPointV : " + floorPlanEndPointV.getX()/0.9375 + ", " + floorPlanEndPointV.getY()/0.9375 );
                List<Vector2> path = finder.findPathBetween(new Vector3(floorPlanStartPointV.getX(), 0, floorPlanStartPointV.getY()), new Vector3(floorPlanEndPointV.getX(), 0, floorPlanEndPointV.getY()));
                Log.i(TAG, "[findPathBetween]path.size() = " + path.size());


                if (path.size() > 0) {
                    path.remove(0);
                    path.add(0, new Vector2(cameraPoseEndPoint.getPosition().x, cameraPoseEndPoint.getPosition().z));

                    //List<Vector2> minimumPath = getMinimumPathVector(path);
                    //Log.i(TAG, "[findPathBetween]minimumPath.size() = " + minimumPath.size());
                    //for (int i = 0; i < minimumPath.size() - 1; i++) {
                    //    NavigationPath navigationPath = new NavigationPath(minimumPath.get(i), minimumPath.get(i + 1));
                    //    getCurrentScene().addChild(navigationPath);
                    //    navigationPaths.add(navigationPath);
                    //    updateNavigationPathDistance(minimumPath.get(i), minimumPath.get(i + 1));

                    //    if (i > 0) {
                    //        Circle circle = new Circle(minimumPath.get(i));
                    //        getCurrentScene().addChild(circle);
                    //        circles.add(circle);
                    //    }
                    //}


                    mMnimumPathVectorList = refineStartPath(getMinimumPathVector(path));
                    mPathVectorList = getPathVectors(path);
                    Log.i(TAG, "[findPathBetween]mPathVectorList.size() = " + mPathVectorList.size());

                    mNavigationPathDistance = 0;

                    if (mIsDebug) {
                        int index = 0;
                        for (PathVector pathVector : mPathVectorList) {
                            Cube cube = new Cube(0.2f);
                            if (pathVector.getType() == PathVector.VectorType.START) {
                                cube.setMaterial(yellow);
                            } else if (pathVector.getType() == PathVector.VectorType.END) {
                                cube.setMaterial(red);
                            } else if (pathVector.getType() == PathVector.VectorType.OTHER) {
                                cube.setMaterial(blue);
                            } else if (pathVector.getType() == PathVector.VectorType.TURN_LEFT) {
                                cube.setMaterial(left);
                            } else if (pathVector.getType() == PathVector.VectorType.TURN_RIGHT) {
                                cube.setMaterial(right);
                            }

                            Log.i(TAG, "[findPathBetween]vector2 x = " + pathVector.getVector().getX() + ", y = " + pathVector.getVector().getY() + ", type = " + pathVector.getType().toString());
                            cube.setPosition(new Vector3(pathVector.getVector().getX(), -1.2, pathVector.getVector().getY()));
                            getCurrentScene().addChild(cube);
                            pathCubes.add(cube);

                            /*if (index < mPathVectorList.size() - 1) {
                                NavigationPath navigationPath = new NavigationPath(PathVectorList.get(index).getVector(), PathVectorList.get(index + 1).getVector());
                                getCurrentScene().addChild(navigationPath);
                                navigationPaths.add(navigationPath);
                                updateNavigationPathDistance(PathVectorList.get(index).getVector(), PathVectorList.get(index + 1).getVector());

                                if (index > 0 && ((pathVector.getType() == PathVector.VectorType.TURN_LEFT) || (pathVector.getType() == PathVector.VectorType.TURN_RIGHT))) {
                                    Circle circle = new Circle(PathVectorList.get(index).getVector());
                                    getCurrentScene().addChild(circle);
                                    circles.add(circle);
                                }
                            }*/
                            index++;
                        }
                    }

                    Cube endCube = new Cube(0.2f);
                    endCube.setMaterial(red);
                    endCube.setPosition(new Vector3(cameraPoseEndPoint.getPosition().x, -1.2, cameraPoseEndPoint.getPosition().z));
                    getCurrentScene().addChild(endCube);
                    pathCubes.add(endCube);

                    logo.setPosition(cameraPoseEndPoint.getPosition().x, 0.0, cameraPoseEndPoint.getPosition().z);
                    logo.setVisible(true);

                    mVisitedVectorSet.clear();
                    cleanNavigationInfo();
                    startNavigation = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "onRender: " + e.getMessage(), e);
            } finally {
                fillPath = false;
            }
        }
    }

    private void renderNavigationPath() {

        Vector2 currentCameraPoseVector = new Vector2(currentCameraPose.getPosition().x, currentCameraPose.getPosition().z);
        if (lastV == null || (currentV.getX() != lastV.getX() || currentV.getY() != lastV.getY())) {

            CleanSceneItems(false, true, true);

            Log.i(TAG, "------------------------------------------------------");
            Log.i(TAG, "currentV = " +currentV.getX() + ", y = " + currentV.getY());

            try {

                mNavigationPathDistance = 0;
                boolean isFound = false;
                for (int i = 1; i < mMnimumPathVectorList.size() - 1; i++) {

                    if (!isFound && !mVisitedVectorSet.contains(mMnimumPathVectorList.get(i))) {

                        if (isNearVector(mMnimumPathVectorList.get(i))) {
                            if (i + 1 < mMnimumPathVectorList.size()) {
                                if (firstNavigationPathScene == null) {
                                    firstNavigationPathScene = new NavigationPath(currentCameraPoseVector, mMnimumPathVectorList.get(i + 1));
                                    getCurrentScene().addChild(firstNavigationPathScene);
                                } else {
                                    firstNavigationPathScene.updateVector(currentCameraPoseVector, mMnimumPathVectorList.get(i + 1));
                                }
                            }
                            if (i + 2 < mMnimumPathVectorList.size()) {
                                for (int y = 0; y < mPathVectorList.size(); y++) {
                                    if (y + 1 < mPathVectorList.size()) {
                                        if (mMnimumPathVectorList.get(i + 1).getX() == mPathVectorList.get(y).getVector().getX() && mMnimumPathVectorList.get(i + 1).getY() == mPathVectorList.get(y).getVector().getY()) {

                                            if (secondNavigationPathScene == null) {
                                                secondNavigationPathScene = new NavigationPath(mMnimumPathVectorList.get(i + 1), mPathVectorList.get(y + 1).getVector());
                                                getCurrentScene().addChild(secondNavigationPathScene);
                                            } else {
                                                secondNavigationPathScene.updateVector(mMnimumPathVectorList.get(i + 1), mPathVectorList.get(y + 1).getVector());
                                            }

                                            if (circle == null) {
                                                circle = new Circle(mMnimumPathVectorList.get(i + 1));
                                                getCurrentScene().addChild(circle);
                                                //animCircle.play();
                                            } else {
                                                circle.updateVector(mMnimumPathVectorList.get(i + 1));
                                                //animCircle.play();
                                            }
                                            break;
                                        }
                                    }
                                }
                            } else {
                                if (secondNavigationPathScene != null) {
                                    getCurrentScene().removeChild(secondNavigationPathScene);
                                    secondNavigationPathScene = null;
                                }
                                if (circle != null) {
                                    getCurrentScene().removeChild(circle);
                                    circle = null;
                                }
                            }
                            mVisitedVectorSet.add(mMnimumPathVectorList.get(i));
                            updateNavigationPathDistance(currentCameraPoseVector, mMnimumPathVectorList.get(i));
                        } else if (mVisitedVectorSet.size() == 0) {
                            if (firstNavigationPathScene == null) {
                                firstNavigationPathScene = new NavigationPath(currentCameraPoseVector, mMnimumPathVectorList.get(i));
                                getCurrentScene().addChild(firstNavigationPathScene);
                            } else {
                                firstNavigationPathScene.updateVector(currentCameraPoseVector, mMnimumPathVectorList.get(i));
                            }
                            if (i + 1 < mMnimumPathVectorList.size()) {
                                for (int y = 0; y < mPathVectorList.size(); y++) {
                                    if (y + 1 < mPathVectorList.size()) {
                                        if (mMnimumPathVectorList.get(i).getX() == mPathVectorList.get(y).getVector().getX() && mMnimumPathVectorList.get(i).getY() == mPathVectorList.get(y).getVector().getY()) {

                                            if (secondNavigationPathScene == null) {
                                                secondNavigationPathScene = new NavigationPath(mMnimumPathVectorList.get(i), mPathVectorList.get(y + 1).getVector());
                                                getCurrentScene().addChild(secondNavigationPathScene);
                                            } else {
                                                secondNavigationPathScene.updateVector(mMnimumPathVectorList.get(i), mPathVectorList.get(y + 1).getVector());
                                            }

                                            if (circle == null) {
                                                circle = new Circle(mMnimumPathVectorList.get(i));
                                                getCurrentScene().addChild(circle);
                                                //animCircle.play();
                                            } else {
                                                circle.updateVector(mMnimumPathVectorList.get(i));
                                                //animCircle.play();
                                            }
                                            break;
                                        }
                                    }
                                }
                            } else {
                                if (secondNavigationPathScene != null) {
                                    getCurrentScene().removeChild(secondNavigationPathScene);
                                    secondNavigationPathScene = null;
                                }
                                if (circle != null) {
                                    //getCurrentScene().unregisterAnimation(animCircle);
                                    getCurrentScene().removeChild(circle);
                                    circle = null;
                                }
                            }
                            updateNavigationPathDistance(currentCameraPoseVector, mMnimumPathVectorList.get(i));

                        } else {
                            updateNavigationPathDistance(currentCameraPoseVector, mMnimumPathVectorList.get(i));
                        }
                        isFound = true;
                        continue;
                    }
                    if (isFound) {
                        updateNavigationPathDistance(mMnimumPathVectorList.get(i), mMnimumPathVectorList.get(i + 1));
                    }
                }

                if (mNavigationPathDistance <= 1.5) {
                    CleanSceneItems(false, true, true);
                    cleanNavigationInfo();
                    Log.i(TAG, "Arrived the endPoint : Navigation is finished. ");
                    EventBus.getDefault().post(new Event(Event.ON_NAVIGATION_FINISHED));
                }

            } catch (Exception e) {
                Log.e(TAG, "onRender: " + e.getMessage(), e);
            }
        } else if (lastV != null && (currentV.getX() == lastV.getX() && currentV.getY() == lastV.getY())) {
            if (firstNavigationPathScene != null) {
                firstNavigationPathScene.updateVector(new Vector2(currentCameraPose.getPosition().x, currentCameraPose.getPosition().z), firstNavigationPathScene.getEndVector());
            }
        }
    }

    private void cleanNavigationInfo() {
        if (firstNavigationPathScene != null) {
            getCurrentScene().removeChild(firstNavigationPathScene);
            firstNavigationPathScene = null;
        }
        if (secondNavigationPathScene != null) {
            getCurrentScene().removeChild(secondNavigationPathScene);
            secondNavigationPathScene = null;
        }
        if (circle != null) {
            getCurrentScene().removeChild(circle);
            circle = null;
        }
        startNavigation = false;
        cameraPoseStartPoint = null;
        cameraPoseEndPoint = null;
        lastV = null;
        endV = null;
    }

    private boolean isNearVector(Vector2 vector) {
        boolean isNear = false;
        Coordinate currentCoordinate = TangoUtil.coordinate(currentV);
        Coordinate specifiedCoordinate = TangoUtil.coordinate(vector);
        double distance = currentCoordinate.distance(specifiedCoordinate);
        if (Double.compare(distance, mDetectDistance) <= 0) {
            isNear = true;
        }

        return isNear;
    }

    public boolean isStartedNavigation() {
        return startNavigation;
    }

    public void setAverageDepth(double averageDepth) {
        mAverageDepth = averageDepth;
    }

    public double getNavigationPathDistance() {
        return mNavigationPathDistance;
    }

    private void updateNavigationPathDistance(Vector2 start, Vector2 end) {
        Coordinate startCoordinate = TangoUtil.coordinate(start);
        Coordinate endCoordinate = TangoUtil.coordinate(end);
        double distance = startCoordinate.distance(endCoordinate);
        Log.i(TAG, "distance = " + distance);
        mNavigationPathDistance += distance;
    }

    private List<Vector2> refineStartPath(List<Vector2> pathVectorList) {
        Log.i(TAG, "[refineStartPath] in ");
        if (pathVectorList != null && pathVectorList.size() >= 3) {
            Coordinate firstCoordinate = TangoUtil.coordinate(pathVectorList.get(0));
            Coordinate secondCoordinate = TangoUtil.coordinate(pathVectorList.get(1));
            Coordinate thirdCoordinate = TangoUtil.coordinate(pathVectorList.get(2));

            double firstPathDistance = firstCoordinate.distance(secondCoordinate);
            double secondPathDistance = secondCoordinate.distance(thirdCoordinate);
            Log.i(TAG, "[refineStartPath]firstPathDistance = " + firstPathDistance);
            Log.i(TAG, "[refineStartPath]secondPathDistance = " + secondPathDistance);
            if (Double.compare(firstPathDistance, (getQuadTreeRange(8, QUAD_TREE_RANGE) * 3)) < 0) {
                if (Double.compare(firstPathDistance, secondPathDistance) <= 0) {
                    Log.i(TAG, "[refineStartPath] refine path ");
                    pathVectorList.remove(1);
                }
            }
        }

        return pathVectorList;
    }

    private double getQuadTreeRange(int depth, double quadTreeRange) {
        if (depth == 0) {
            return quadTreeRange;
        } else {
            return getQuadTreeRange(depth - 1,  quadTreeRange / 2.0);
        }
    }

    private void CleanSceneItems(boolean clearCubes, boolean clearPaths, boolean clearCircles) {

        if (clearCubes) {
            for (Cube pathCube : pathCubes) {
                getCurrentScene().removeChild(pathCube);
            }
            pathCubes.clear();
        }
        if (clearPaths) {
            for (NavigationPath navigationPath : navigationPaths) {
                getCurrentScene().removeChild(navigationPath);
            }
            navigationPaths.clear();
        }
        if (clearCircles) {
            for (Circle circle : circles) {
                getCurrentScene().removeChild(circle);
            }
            circles.clear();
        }
    }

    // for 3D space
    private List<PathVector> getPathVectors(List<Vector2> path) {
        List<PathVector> pathVectors = new ArrayList<>();

        int pathSize = path.size();
        if (pathSize >= 3) {
            // start node
            PathVector startVector = new PathVector(PathVector.VectorType.START, path.get(pathSize - 1));
            pathVectors.add(startVector);
            Log.i(TAG, "[getPathVectors]startVector = " + path.get(pathSize - 1).getX() + ", " + path.get(pathSize - 1).getY());
            for (int i = pathSize - 2; i >= 1; i--) {
                // last vector, now vector, next vector
                TangoUtil.VectorDirection direction = TangoUtil.getDirectionByVector3(new Vector3(path.get(i + 1).getX(), 0, path.get(i + 1).getY()),
                        new Vector3(path.get(i).getX(), 0, path.get(i).getY()),
                        new Vector3(path.get(i - 1).getX(), 0, path.get(i - 1).getY()));

                PathVector.VectorType vectorType = PathVector.VectorType.OTHER;
                if (direction == TangoUtil.VectorDirection.LEFT) {
                    vectorType = PathVector.VectorType.TURN_LEFT;
                } else if (direction == TangoUtil.VectorDirection.RIGHT){
                    vectorType = PathVector.VectorType.TURN_RIGHT;
                }
                Log.i(TAG, "[getPathVectors]vectorType = " + vectorType + ", vector = " + path.get(i).getX() + ", " + path.get(i).getY());
                PathVector otherVector = new PathVector(vectorType, path.get(i));
                pathVectors.add(otherVector);
            }

            // end node
            PathVector endVector = new PathVector(PathVector.VectorType.END, path.get(0));
            pathVectors.add(endVector);
            Log.i(TAG, "[getPathVectors]endVector = " + path.get(0).getX() + ", " + path.get(0).getY());
        } else if (pathSize == 2){
            PathVector startVector = new PathVector(PathVector.VectorType.START, path.get(1));
            pathVectors.add(startVector);
            PathVector endVector = new PathVector(PathVector.VectorType.END, path.get(0));
            pathVectors.add(endVector);
        } else if (pathSize == 1){
            PathVector vector = new PathVector(PathVector.VectorType.OTHER, path.get(0));
            pathVectors.add(vector);
        }

        return pathVectors;
    }

    // for 2D space
    private List<PathVector> getPathVectorsBySlope(List<Vector2> path) {
        //List<Vector2> path = getMinimumPathVector(refinePathStep1);
        List<PathVector> pathVectors = new ArrayList<>();
        List<Vector2> reversePath = new ArrayList<>();
        for (int i = path.size() - 1; i >= 0; i--) {
            reversePath.add(path.get(i));
        }

        if (reversePath.size() >= 2) {
            Log.i(TAG, "[getPathVectorsBySlope]reversePath.size() " + reversePath.size());
            Log.i(TAG, "[getPathVectorsBySlope]vector2 x = " + reversePath.get(0).getX() + ", y = " + reversePath.get(0).getY());
            PathVector startVector = new PathVector(PathVector.VectorType.START, reversePath.get(0));
            pathVectors.add(startVector);
            for (int i = 1; i < reversePath.size() - 1; i++) {
                Log.i(TAG, "[getPathVectorsBySlope]vector2 x = " + reversePath.get(i).getX() + ", y = " + reversePath.get(i).getY());
                double slope = getSlope(reversePath.get(i), reversePath.get(i + 1));
                PathVector.VectorType vectorType = PathVector.VectorType.OTHER;
                Log.i(TAG, "[getPathVectorsBySlope]currSlope = " + slope);

                if (slope > 0.0 && !Double.isInfinite(slope)) {
                    Log.i(TAG, "[getPathVectorsBySlope]currSlope > 0 (TURN_RIGHT)");
                    vectorType = PathVector.VectorType.TURN_RIGHT;
                } else if (slope == -0.0) {
                    Log.i(TAG, "[getPathVectorsBySlope]currSlope other : -0.0");
                } else if (slope < 0.0) {
                    Log.i(TAG, "[getPathVectorsBySlope]currSlope < 0 (TURN_LEFT)");
                    vectorType = PathVector.VectorType.TURN_LEFT;
                } else {
                    Log.i(TAG, "[getPathVectorsBySlope]currSlope other ");
                }
                PathVector otherVector = new PathVector(vectorType, reversePath.get(i));
                pathVectors.add(otherVector);
            }
            Log.i(TAG, "[getPathVectorsBySlope]vector2 x = " + reversePath.get(reversePath.size() - 1).getX() + ", y = " + reversePath.get(reversePath.size() - 1).getY());
            PathVector endVector = new PathVector(PathVector.VectorType.END, reversePath.get(reversePath.size() - 1));
            pathVectors.add(endVector);
        } else if (reversePath.size() == 1){
            PathVector vector = new PathVector(PathVector.VectorType.OTHER, reversePath.get(0));
            pathVectors.add(vector);
        }

        return pathVectors;
    }

    private List<Vector2> getMinimumPathVector(List<Vector2> path) {
        List<Vector2> pathVector = new ArrayList<>();

        if (path.size() >= 2) {
            double lastSlope = getSlope(path.get(path.size() - 1), path.get(path.size() - 2));
            pathVector.add(path.get(path.size() - 1));
            Log.i(TAG, "[getMinimumPathVector] x = " + path.get(path.size() - 1).getX() + ", y = " + path.get(path.size() - 1).getY());
            for (int i = path.size() - 2; i >= 0; i--) {
                double currSlope = getSlope(path.get(i + 1), path.get(i));

                if (currSlope != lastSlope) {
                    pathVector.add(path.get(i + 1));
                    Log.i(TAG, "[getMinimumPathVector] x = " + path.get(i + 1).getX() + ", y = " + path.get(i + 1).getY());
                    lastSlope = currSlope;
                }
            }

            pathVector.add(path.get(0));
            Log.i(TAG, "[getMinimumPathVector] x = " + path.get(0).getX() + ", y = " + path.get(0).getY());
        } else if (path.size() == 1){
            pathVector.add(path.get(0));
        }
        return pathVector;
    }

    private List<PathVector> getRefinePath(List<Vector2> path) {
        List<PathVector> refinePath = new ArrayList<>();

        if (path.size() >= 2) {
            double lastSlope = getSlope(path.get(path.size() - 1), path.get(path.size() - 2));
            //Log.i(TAG, "[getRefinePath]init lastSlope = " + lastSlope);
            //refinePath.add(path.get(path.size() - 1));
            PathVector startVector = new PathVector(PathVector.VectorType.START, path.get(path.size() - 1));
            refinePath.add(startVector);
            Log.i(TAG, "[getRefinePath]init add index= " + (path.size() - 1));
            for (int i = path.size() - 2; i >= 0; i--) {
                double currSlope = getSlope(path.get(i + 1), path.get(i));
                //Log.i(TAG, "[getRefinePath]currSlope = " + currSlope);
                /*if (isLineEnd) {
                    refinePath.add(path.get(i + 1));
                    Log.i(TAG, "[getRefinePath][1] add index= " + (i + 1));
                    isLineEnd = false;
                }*/
                if (currSlope != lastSlope) {
                    //refinePath.add(path.get(i + 1));
                    Log.i(TAG, "[getRefinePath][2] add index= " + (i + 1));
                    PathVector.VectorType vectorType = PathVector.VectorType.OTHER;
                    if (currSlope > 0) {
                        Log.i(TAG, "[getRefinePath]currSlope > 0 ");
                        vectorType = PathVector.VectorType.TURN_LEFT;
                    } else if (currSlope < 0){
                        Log.i(TAG, "[getRefinePath]currSlope < 0 ");
                        vectorType = PathVector.VectorType.TURN_RIGHT;
                    } else {
                        Log.i(TAG, "[getRefinePath]currSlope other ");
                    }
                    PathVector otherVector = new PathVector(vectorType, path.get(i + 1));
                    refinePath.add(otherVector);
                    lastSlope = currSlope;
                    //isLineEnd = true;
                }
            }
            //refinePath.add(path.get(0));
            Log.i(TAG, "[getRefinePath]end add index= " + (0));
            PathVector endVector = new PathVector(PathVector.VectorType.END, path.get(0));
            refinePath.add(endVector);
        } else if (path.size() == 1){
            PathVector vector = new PathVector(PathVector.VectorType.OTHER, path.get(0));
            refinePath.add(vector);
        }

        return refinePath;
    }

    public void setStartPoint(TangoPoseData currentTangoPose, DeviceExtrinsics extrinsics) {
        cameraPoseStartPoint = ScenePoseCalculator.toOpenGlCameraPose(currentTangoPose, extrinsics);
        floorPlan.addPoint(cameraPoseStartPoint.getPosition());
        if (cameraPoseStartPoint != null && cameraPoseEndPoint != null) {
            fillPath = true;
        }
    }

    public void setEndPoint(TangoPoseData currentTangoPose, DeviceExtrinsics extrinsics) {
        cameraPoseEndPoint = ScenePoseCalculator.toOpenGlCameraPose(currentTangoPose, extrinsics);
        floorPlan.addPoint(cameraPoseEndPoint.getPosition());
        if (cameraPoseStartPoint != null && cameraPoseEndPoint != null) {
            fillPath = true;
        }
    }

    public void setStartPoint(Vector3 position, Quaternion quaternion, DeviceExtrinsics extrinsics) {
        cameraPoseStartPoint = ScenePoseCalculator.toOpenGlCameraPose(position, quaternion, extrinsics);
        floorPlan.addPoint(cameraPoseStartPoint.getPosition());
        if (cameraPoseStartPoint != null && cameraPoseEndPoint != null) {
            fillPath = true;
        }
    }

    public void setEndPoint(Vector3 position, Quaternion quaternion, DeviceExtrinsics extrinsics) {
        cameraPoseEndPoint = ScenePoseCalculator.toOpenGlCameraPose(position, quaternion, extrinsics);
        floorPlan.addPoint(cameraPoseEndPoint.getPosition());
        if (cameraPoseStartPoint != null && cameraPoseEndPoint != null) {
            fillPath = true;
        }
    }

    public void addPointsWithGrid(Vector3 position, Quaternion quaternion, DeviceExtrinsics extrinsics) {
        Pose cameraPose = ScenePoseCalculator.toOpenGlCameraPose(position, quaternion, extrinsics);
        floorPlan.setTrajectoryPositionWithGrid(cameraPose.getPosition());
    }

    public void addPoints(Vector3 position, Quaternion quaternion, DeviceExtrinsics extrinsics) {
        Pose cameraPose = ScenePoseCalculator.toOpenGlCameraPose(position, quaternion, extrinsics);
        floorPlan.setTrajectoryPosition(cameraPose.getPosition());
    }

    public Vector2 getFilledPositionByPose(TangoPoseData devicePose, DeviceExtrinsics extrinsics) {

        Pose cameraPose = ScenePoseCalculator.toOpenGlCameraPose(devicePose, extrinsics);
        getCurrentCamera().setRotation(cameraPose.getOrientation());
        getCurrentCamera().setPosition(cameraPose.getPosition());

        return floorPlan.getFilledPositionByPoint(cameraPose.getPosition());
    }

    public void addNamingPointCube(Vector2 namingVector) {
        if (namingVector != null) {
            Cube cube = new Cube(0.2f);
            cube.setMaterial(yellow);
            Log.i(TAG, "[addNamingPointCube]namingVector x = " + namingVector.getX() + ", y = " + namingVector.getY());
            cube.setPosition(new Vector3(namingVector.getX(), -1.4, namingVector.getY()));
            getCurrentScene().addChild(cube);
            String cubeKey = namingVector.getX() + ":" + namingVector.getY();
            namginVectorCubes.put(cubeKey, cube);
        }
    }

    public void deleteNamingPointCube(Vector2 namingVector) {
        if (namingVector != null) {
            String cubeKey = namingVector.getX() + ":" + namingVector.getY();
            if (namginVectorCubes.containsKey(cubeKey)) {
                Log.i(TAG, "[deleteNamingPointCube]namingVector x = " + namingVector.getX() + ", y = " + namingVector.getY());
                getCurrentScene().removeChild(namginVectorCubes.get(cubeKey));
                namginVectorCubes.remove(cubeKey);
            }
        }
    }

    public QuadTree getFloorPlanData() {
        return data;
    }

    public void resetFloorPlanScene() {
        getCurrentScene().removeChild(floorPlan);
        resetData();
        floorPlan = new FloorPlan(data);
        getCurrentScene().addChild(floorPlan);
    }

    public void renderVirtualObjects(boolean renderObjects) {
        renderVirtualObjects = renderObjects;
        if (this.floorPlan != null)
            this.floorPlan.setVisible(renderObjects);
    }

    public void updateDeleteGridScene() {
        removeDeleteGridScene();
        FloorPlanMarkedForDelete floorPlanMarkedForDelete = new FloorPlanMarkedForDelete(data.markedforDeleteVectorSet);
        getCurrentScene().addChild(floorPlanMarkedForDelete);
        floorPlanMarkedForDeleteList.add(floorPlanMarkedForDelete);
    }

    public void removeDeleteGridScene() {
        for (FloorPlanMarkedForDelete floorPlanMarkedForDelete : floorPlanMarkedForDeleteList) {
            getCurrentScene().removeChild(floorPlanMarkedForDelete);
        }
        floorPlanMarkedForDeleteList.clear();
    }
}
