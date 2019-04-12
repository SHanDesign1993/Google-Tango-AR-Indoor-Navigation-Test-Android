package trendmicro.com.tangoindoornavigation.rendering;

import android.util.Log;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector2;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import trendmicro.com.tangoindoornavigation.data.QuadTree;

import static trendmicro.com.tangoindoornavigation.data.QuadTree.PLANE_SPACER;
import static trendmicro.com.tangoindoornavigation.rendering.SceneRenderer.QUAD_TREE_RANGE;

/**
 * Created by hugo on 12/09/2017.
 */

public class FloorPlanMarkedForDelete extends Object3D {

    private static final String TAG = FloorPlanMarkedForDelete.class.getSimpleName();
    private static final int MAX_VERTICES = 10000;
    private final float[] color;
    private final double halfRange;
    private Set<Vector2> vectorSet;

    public FloorPlanMarkedForDelete(Set<Vector2> vectorSet) {
        super();
        this.color = new float[]{5.5f, 5.5f, 5.5f, 0.5f};
        this.vectorSet = vectorSet;
        this.halfRange = QUAD_TREE_RANGE / 2.0;
        init();
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
        return  getPointsAsPolygonWithDepth(list, 8, halfRange);
    }

    private List<Vector2> getPointsAsPolygonWithDepth(List<Vector2> list, int depth, double halfRange) {

        if (depth == 0) {
            for (Vector2 vector : vectorSet) {
                //Log.i(TAG, "[FloorPlanMarkedForDelete]vector = " + vector.getX() + ", " + vector.getY());
                list.add(new Vector2(vector.getX() - halfRange, vector.getY() - halfRange));
                list.add(new Vector2(vector.getX() + halfRange - PLANE_SPACER, vector.getY() - halfRange));
                list.add(new Vector2(vector.getX() - halfRange, vector.getY() + halfRange - PLANE_SPACER));

                list.add(new Vector2(vector.getX() - halfRange, vector.getY() + halfRange - PLANE_SPACER));
                list.add(new Vector2(vector.getX() + halfRange - PLANE_SPACER, vector.getY() - halfRange));
                list.add(new Vector2(vector.getX() + halfRange - PLANE_SPACER, vector.getY() + halfRange - PLANE_SPACER));
            }
            return list;
        } else {
            return getPointsAsPolygonWithDepth(list, depth - 1,  halfRange / 2.0);
        }
    }
}