package trendmicro.com.tangoindoornavigation.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.EventBus;
import trendmicro.com.tangoindoornavigation.data.QuadTree;
import trendmicro.com.tangoindoornavigation.db.PointDao;
import trendmicro.com.tangoindoornavigation.eventbus.Event;
import trendmicro.com.tangoindoornavigation.util.MapTransformationGestureDetector;

/**
 * Created by hugo on 18/09/2017.
 */

public class MapView extends View implements View.OnTouchListener, MapTransformationGestureDetector.OnMapTransformationGestureListener, QuadTree.QuadTreeDataListener {

    public static final String DELETE_GRID_VECTOR_X = "delete_grid_vector_x";
    public static final String DELETE_GRID_VECTOR_Y = "delete_grid_vector_y";
    private static final double RECT_SIZE_CONST = 50.0;
    private static final double MAP_SCALE_CONSTANT = 30.0;

    private static final String TAG = MapView.class.getSimpleName();
    public static final int RECTANGLE_POINT_COUNT = 4;
    public static final int LINE_POINT_COUNT = 2;
    private final ArrayList<Vector3> points = new ArrayList<>();

    private Paint greenPaint;
    private Paint bluePaint;
    private Paint redPaint;
    private Paint yellowPaint;
    private QuadTree floorPlanData;
    private boolean isDeleteMode = false;
    private boolean isFilledMode = false;
    private MapTransformationGestureDetector mapTransformationGestureDetector;

    private Matrix4 activeTransformation = Matrix4.createTranslationMatrix(new Vector3());

    private Vector3 previousTranslation = new Vector3(0, 0, 0);
    private float previousScale = 1.0f;
    private float previousRotation = 0f;

    private ArrayList<Vector2> gridVectorList = new ArrayList<>();
    private Set<Vector2> markedforFillVectorSet = new HashSet<Vector2>();
    private Set<Vector2> namingVectorSet = new HashSet<Vector2>();
    private Vector2 currGridVector;


    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public MapView(Context context) {
        super(context);
        init();
    }

    public MapView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenPaint.setColor(Color.GREEN);
        greenPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        bluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bluePaint.setColor(Color.BLUE);
        bluePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setColor(Color.RED);
        redPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        yellowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yellowPaint.setColor(Color.YELLOW);
        yellowPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mapTransformationGestureDetector = new MapTransformationGestureDetector(this);
        setOnTouchListener(this);
        transformPoints();
    }

    public void addNamingVector(Vector2 vector) {
        Log.i(TAG, "[addNamingVector]vector = " + vector.getX() + ", " + vector.getY());
        // namingVectorSet.contains(vector) not work since object are call by value?
        for (Vector2 v : gridVectorList) {
            if (v.getX() == vector.getX() && v.getY() == vector.getY()) {
                namingVectorSet.add(v);
                break;
            }
        }
    }

    private void transformPoints() {
        if (floorPlanData != null) {

            List<Vector2> filledPoints = floorPlanData.getFilledPoints();
            synchronized (points) {
                points.clear();
                gridVectorList.clear();
                for (Vector2 filledPoint : filledPoints) {
                    Vector3 v3 = new Vector3(filledPoint.getX(), filledPoint.getY(), 0);
                    v3.multiply(activeTransformation);
                    points.add(v3);
                    gridVectorList.add(filledPoint);
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);
        synchronized (points) {
            int index = 0;
            for (Vector3 point : points) {
                //Log.i(TAG, "[onDraw]point = " + point.toString());
                if (!this.floorPlanData.markedforDeleteVectorSet.contains(gridVectorList.get(index))) {
                    if (!markedforFillVectorSet.contains(gridVectorList.get(index))) {
                        if ((this.currGridVector != null) && (this.currGridVector.getX() == gridVectorList.get(index).getX()) && (this.currGridVector.getY() == gridVectorList.get(index).getY())){
                            drawRect(canvas, point.x, point.y, bluePaint); // current grid
                        } else if (namingVectorSet.contains(gridVectorList.get(index))) {
                            drawRect(canvas, point.x, point.y, yellowPaint); // naming grids
                        } else {
                            drawRect(canvas, point.x, point.y, greenPaint); // other grids
                        }
                    } else {
                        drawRect(canvas, point.x, point.y, redPaint); // marked for filled grids
                    }
                }
                index++;
            }
        }
    }

    private void drawRect(Canvas canvas, double x, double y, Paint paint) {
        canvas.drawRect(
                (int) (x * MAP_SCALE_CONSTANT),
                (int) (y * MAP_SCALE_CONSTANT),
                (int) (x * MAP_SCALE_CONSTANT + RECT_SIZE_CONST),
                (int) (y * MAP_SCALE_CONSTANT + RECT_SIZE_CONST), paint);
    }

    public void setFloorPlanData(QuadTree floorPlanData) {
        if (this.floorPlanData != null) {
            this.floorPlanData.clear();
            transformPoints();
            postInvalidate();
        }
        this.floorPlanData = floorPlanData;
        this.floorPlanData.setListener(this);
    }

    public void setDeleteMode(boolean isDeleteMode) {
        this.isDeleteMode = isDeleteMode;
    }

    public void setMarkFilledPointsMode(boolean isFilledMode) {
        this.isFilledMode = isFilledMode;
        if (!isFilledMode) {
            markedforFillVectorSet.clear();
            transformPoints();
            postInvalidate();
        }
    }

    public Map<String,Vector2> getMarkedforFillVectorMap() {

        Map<String,Vector2> markedforFillVectorMap = new HashMap<>();

        Log.i(TAG, "[getMarkedforFillVectorMap]markedforFillVectorSet.size() = " + markedforFillVectorSet.size());
        if (markedforFillVectorSet.size() == RECTANGLE_POINT_COUNT) {
            Vector2 leftBottomVector = null;
            Vector2 leftTopVector = null;
            Vector2 rightBottomVector = null;
            Vector2 rightTopVector = null;
            for (Vector2 vector : markedforFillVectorSet) {
                Log.i(TAG, "[getMarkedforFillVectorMap]vector = " + vector.getX() + ", " + vector.getY());
                if (leftBottomVector == null || rightTopVector == null) {
                    leftBottomVector = vector;
                    rightTopVector = vector;
                } else {
                    if (vector.getX() >= rightTopVector.getX() && vector.getY() >= rightTopVector.getY()) {
                        rightTopVector = vector;
                    } else if (vector.getX() <= leftBottomVector.getX() && vector.getY() <= leftBottomVector.getY()) {
                        leftBottomVector = vector;
                    }
                }
            }
            if (leftBottomVector != null && rightTopVector != null) {
                markedforFillVectorMap.put("leftBottomVector", leftBottomVector);
                markedforFillVectorMap.put("rightTopVector", rightTopVector);
                markedforFillVectorSet.remove(leftBottomVector);
                markedforFillVectorSet.remove(rightTopVector);
            }

            for (Vector2 vector : markedforFillVectorSet) {
                Log.i(TAG, "[getMarkedforFillVectorMap]vector = " + vector.getX() + ", " + vector.getY());
                if (leftBottomVector != null || rightTopVector != null) {
                    if (vector.getX() == rightTopVector.getX() && vector.getY() < rightTopVector.getY()) {
                        rightBottomVector = vector;
                        markedforFillVectorMap.put("rightBottomVector", rightBottomVector);
                        markedforFillVectorSet.remove(rightBottomVector);
                        break;
                    }
                }
            }

            for (Vector2 vector : markedforFillVectorSet) {
                Log.i(TAG, "[getMarkedforFillVectorMap]vector = " + vector.getX() + ", " + vector.getY());
                if (leftBottomVector != null || rightTopVector != null) {
                    if (vector.getX() == leftBottomVector.getX() && vector.getY() > leftBottomVector.getY()) {
                        leftTopVector = vector;
                        markedforFillVectorMap.put("leftTopVector", leftTopVector);
                        markedforFillVectorSet.remove(leftTopVector);
                        break;
                    }
                }
            }
        } else if (markedforFillVectorSet.size() == LINE_POINT_COUNT) {
            Vector2 firstVector = null;
            Vector2 secondVector = null;
            int index = 1;
            for (Vector2 vector : markedforFillVectorSet) {
                if (index == 1) {
                    firstVector = vector;
                } else {
                    secondVector = vector;
                }
                index++;
            }

            Log.i(TAG, "[getMarkedforFillVectorMap]firstVector = " + firstVector.getX() + ", " + firstVector.getY());
            Log.i(TAG, "[getMarkedforFillVectorMap]secondVector = " + secondVector.getX() + ", " + secondVector.getY());
            if (firstVector.getX() == secondVector.getX()) {
                if (firstVector.getY() > secondVector.getY()) {
                    markedforFillVectorMap.put("startVector", secondVector);
                    markedforFillVectorSet.remove(secondVector);
                    markedforFillVectorMap.put("endVector", firstVector);
                    markedforFillVectorSet.remove(firstVector);
                } else {
                    markedforFillVectorMap.put("startVector", firstVector);
                    markedforFillVectorSet.remove(firstVector);
                    markedforFillVectorMap.put("endVector", secondVector);
                    markedforFillVectorSet.remove(secondVector);
                }
            } else if (firstVector.getY() == secondVector.getY()) {
                if (firstVector.getX() > secondVector.getX()) {
                    markedforFillVectorMap.put("startVector", secondVector);
                    markedforFillVectorSet.remove(secondVector);
                    markedforFillVectorMap.put("endVector", firstVector);
                    markedforFillVectorSet.remove(firstVector);
                } else {
                    markedforFillVectorMap.put("startVector", firstVector);
                    markedforFillVectorSet.remove(firstVector);
                    markedforFillVectorMap.put("endVector", secondVector);
                    markedforFillVectorSet.remove(secondVector);
                }
            }
        }
        return markedforFillVectorMap;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mapTransformationGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void OnClicked(float x, float y) {
        if (isDeleteMode || isFilledMode) {
            int index = 0;
            for (Vector3 point : points) {
                double pointXStart = point.x * MAP_SCALE_CONSTANT;
                double pointYStart = point.y * MAP_SCALE_CONSTANT;
                double pointXEnd = point.x * MAP_SCALE_CONSTANT + RECT_SIZE_CONST;
                double pointYEnd = point.y * MAP_SCALE_CONSTANT + RECT_SIZE_CONST;
                if (x >= pointXStart && x <= pointXEnd) {
                    if (y >= pointYStart && y <= pointYEnd){

                        Vector2 gridVector = gridVectorList.get(index);
                        Log.i(TAG, "[OnClicked]Hit : GridVector = " + gridVector.getX() + ", " + gridVector.getY());
                        if (isDeleteMode) {
                            floorPlanData.markedforDeleteVectorSet.add(gridVector);
                            if (namingVectorSet.contains(gridVector)) {
                                namingVectorSet.remove(gridVector);
                            }
                            Bundle bundle = new Bundle();
                            bundle.putDouble(DELETE_GRID_VECTOR_X, gridVector.getX());
                            bundle.putDouble(DELETE_GRID_VECTOR_Y, gridVector.getY());
                            Event event = new Event(Event.ON_DELETE_GRID_VECTOR);
                            event.setBundle(bundle);
                            EventBus.getDefault().post(event);
                        } else if (isFilledMode) {
                            if (markedforFillVectorSet.size() < RECTANGLE_POINT_COUNT) {
                                markedforFillVectorSet.add(gridVector);
                            }
                        }

                        transformPoints();
                        postInvalidate();
                        break;
                    }
                }
                index++;
            }
        }
    }

    @Override
    public void OnTransform(MapTransformationGestureDetector detector) {
        Vector3 translation = detector.getTranslation();
        translation.x = translation.x / MAP_SCALE_CONSTANT;
        translation.y = translation.y / MAP_SCALE_CONSTANT;
        translation = translation.clone().add(this.previousTranslation);
        //activeTransformation = Matrix4.createRotationMatrix(Vector3.Axis.Z, detector.getAngle() + this.previousRotation);
        activeTransformation = Matrix4.createRotationMatrix(Vector3.Axis.Z, 0);
        activeTransformation.translate(translation);
        //activeTransformation.scale(detector.getScale() + this.previousScale - 1.0f);
        activeTransformation.scale(3.0);
        transformPoints();
        postInvalidate();
    }

    @Override
    public void OnTransformEnd(MapTransformationGestureDetector rotationDetector) {
        this.previousTranslation = rotationDetector.getTranslation().add(this.previousTranslation);
        this.previousRotation = rotationDetector.getAngle() + this.previousRotation;
        this.previousScale = rotationDetector.getScale() + this.previousScale - 1.0f;
    }

    @Override
    public void OnQuadTreeUpdate() {
        transformPoints();
        postInvalidate();
    }

    @Override
    public void OnCurrVectorUpdate(Vector2 currGridVector) {

        if (currGridVector != null) {
            if (this.currGridVector != null) {
                if (this.currGridVector.getX() != currGridVector.getX() || this.currGridVector.getY() != currGridVector.getY()) {
                    //Log.i(TAG, "[OnCurrVectorUpdate]this.currGridVector = " + this.currGridVector.getX() + ", " + this.currGridVector.getY() + ", currGridVector = " + currGridVector.getX() + ", " + currGridVector.getY());
                    this.currGridVector = currGridVector;
                    transformPoints();
                    postInvalidate();
                }
            } else {
                this.currGridVector = currGridVector;
                transformPoints();
                postInvalidate();
            }
        }
    }
}
