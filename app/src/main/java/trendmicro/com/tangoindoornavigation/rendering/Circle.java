package trendmicro.com.tangoindoornavigation.rendering;

import android.opengl.GLES20;
import android.util.Log;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hugo Huang on 8/11/2017.
 */

public class Circle extends Object3D {

    private static final String TAG = NavigationPath.class.getSimpleName();
    public static final double RADIUS = 0.3;
    private static final double TWICE_PI = 2.0f * 3.141592;
    private static final int Triangle_AMOUNT = 40;
    private static final int MAX_VERTICES = 10000;
    private final float[] color;
    private Vector2 mVector;

    public Circle(Vector2 vector) {
        super();
        this.color = new float[]{0.34510f, 0.82745f, 0.96863f, 0.8f};
        this.mVector = vector;
        Log.i(TAG, "[Circle] mVector.getX() = " + mVector.getX() + ",  mVector.getY() = " + mVector.getY());
        init();
    }

    public void updateVector(Vector2 vector) {
        this.mVector = vector;
        rebuildPoints();
    }

    public Vector2 getVector() {
        return this.mVector;
    }

    public void rebuildPoints() {

        List<Vector2> filledPoints = getPointsAsPolygon();
        FloatBuffer points = FloatBuffer.allocate(filledPoints.size() * 3);
        for (Vector2 filledPoint : filledPoints) {
            points.put((float) filledPoint.getX());
            points.put(0);
            points.put((float) filledPoint.getY());
        }
        updatePoints(filledPoints.size(), points);
    }

    // for CircleAnimation3D
    public void rebuildPoints(int amout) {

        List<Vector2> filledPoints = getPointsAsPolygon(amout);
        FloatBuffer points = FloatBuffer.allocate(filledPoints.size() * 3);
        for (Vector2 filledPoint : filledPoints) {
            points.put((float) filledPoint.getX());
            points.put(0);
            points.put((float) filledPoint.getY());
        }
        updatePoints(filledPoints.size(), points);
    }

    private void init() {
        this.setTransparent(true);
        this.setDoubleSided(true);

        float[] vertices = new float[MAX_VERTICES * 3];
        float[] normals = new float[MAX_VERTICES * 3];
        int[] indices = new int[MAX_VERTICES];
        for (int i = 0; i < indices.length; ++i) {
            indices[i] = i;
            int index = i * 3;
            normals[index] = 0;
            normals[index + 1] = 1;
            normals[index + 2] = 0;
        }
        setData(vertices, normals, null, null, indices, false);
        Material material = new Material();
        material.setColor(color);
        setMaterial(material);
        rebuildPoints();
        setPosition(new Vector3(0, -1.4, 0));
    }

    private void updatePoints(int pointCount, FloatBuffer pointCloudBuffer) {
        setDrawingMode(GLES20.GL_TRIANGLE_FAN);
        mGeometry.setNumIndices(pointCount);
        mGeometry.setVertices(pointCloudBuffer);
        mGeometry.changeBufferData(mGeometry.getVertexBufferInfo(), mGeometry.getVertices(), 0, pointCount * 3);
    }

    private List<Vector2> getPointsAsPolygon() {

        List<Vector2> list = new ArrayList<>();

        list.add(new Vector2(mVector.getX(), mVector.getY()));
        for (int i = 0; i < Triangle_AMOUNT; i++) {
            Vector2 v = new Vector2(mVector.getX() + (RADIUS * Math.cos(i * TWICE_PI / Triangle_AMOUNT)), mVector.getY() + (RADIUS *Math.sin(i * TWICE_PI / Triangle_AMOUNT)));
            list.add(v);
            if (i == Triangle_AMOUNT - 1) {
                Vector2 v1 = new Vector2(mVector.getX() + (RADIUS * Math.cos(0 * TWICE_PI / Triangle_AMOUNT)), mVector.getY() + (RADIUS *Math.sin(0 * TWICE_PI / Triangle_AMOUNT)));
                list.add(v1);
            }
        }
        return  list;
    }

    // for CircleAnimation3D
    private List<Vector2> getPointsAsPolygon(int amout) {

        List<Vector2> list = new ArrayList<>();

        list.add(new Vector2(mVector.getX(), mVector.getY()));
        for (int i = 0; i < amout; i++) {
            Vector2 v = new Vector2(mVector.getX() + (RADIUS * Math.cos(i * TWICE_PI / Triangle_AMOUNT)), mVector.getY() + (RADIUS *Math.sin(i * TWICE_PI / Triangle_AMOUNT)));
            list.add(v);
            if (i == Triangle_AMOUNT - 1) {
                Vector2 v1 = new Vector2(mVector.getX() + (RADIUS * Math.cos(0 * TWICE_PI / Triangle_AMOUNT)), mVector.getY() + (RADIUS *Math.sin(0 * TWICE_PI / Triangle_AMOUNT)));
                list.add(v1);
            }
        }
        return  list;
    }
}
