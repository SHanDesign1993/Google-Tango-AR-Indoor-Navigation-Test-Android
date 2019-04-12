package trendmicro.com.tangoindoornavigation.rendering;

import android.util.Log;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Hugo Huang on 8/9/2017.
 */

public class NavigationPath extends Object3D {

    private static final String TAG = NavigationPath.class.getSimpleName();
    public static final double RADIUS = 0.3;
    private static final int MAX_VERTICES = 10000;
    private final float[] color;
    private Vector3 mVectorStart;
    private Vector3 mVectorEnd;
    private Vector3 mVectorUp = new Vector3(0, 1, 0);

    public NavigationPath(Vector2 start, Vector2 end) {
        super();
        this.color = new float[]{0.34510f, 0.82745f, 0.96863f, 0.8f};
        this.mVectorStart = new Vector3(start.getX(), 0, start.getY());
        this.mVectorEnd = new Vector3(end.getX(), 0, end.getY());
        Log.i(TAG, "[NavigationPath] mVectorStart = " + mVectorStart.toString());
        Log.i(TAG, "[NavigationPath] mVectorEnd = " + mVectorEnd.toString());
        init();
    }

    public Vector3 getStartVector3() {
        return this.mVectorStart;
    }

    public Vector3 getEndVector3() {
        return this.mVectorEnd;
    }

    public Vector2 getStartVector() {
        return new Vector2(this.mVectorStart.x, this.mVectorStart.z);
    }

    public Vector2 getEndVector() {
        return new Vector2(this.mVectorEnd.x, this.mVectorEnd.z);
    }

    public void updateVector(Vector2 start, Vector2 end) {
        this.mVectorStart = new Vector3(start.getX(), 0, start.getY());
        this.mVectorEnd = new Vector3(end.getX(), 0, end.getY());
        rebuildPoints();
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
        mGeometry.setNumIndices(pointCount);
        mGeometry.setVertices(pointCloudBuffer);
        mGeometry.changeBufferData(mGeometry.getVertexBufferInfo(), mGeometry.getVertices(), 0, pointCount * 3);
    }

    private List<Vector2> getPointsAsPolygon() {

        List<Vector2> list = new ArrayList<>();

        Vector3 vSE = Vector3.subtractAndCreate(mVectorEnd, mVectorStart);

        Vector3 crossSE = Vector3.crossAndCreate(vSE, mVectorUp);
        crossSE.normalize();
        Vector3 vRadius = crossSE.multiply(RADIUS);

        Vector3 v1 = Vector3.addAndCreate(mVectorStart, vRadius);
        Vector3 v2 = Vector3.subtractAndCreate(mVectorStart, vRadius);
        Vector3 v3 = Vector3.addAndCreate(mVectorEnd, vRadius);
        Vector3 v4 = Vector3.subtractAndCreate(mVectorEnd, vRadius);

        list.add(new Vector2(v1.x, v1.z));
        list.add(new Vector2(v3.x, v3.z));
        list.add(new Vector2(v2.x, v2.z));

        list.add(new Vector2(v2.x, v2.z));
        list.add(new Vector2(v3.x, v3.z));
        list.add(new Vector2(v4.x, v4.z));

        return  list;
    }
}
