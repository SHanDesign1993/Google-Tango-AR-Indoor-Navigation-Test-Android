package trendmicro.com.tangoindoornavigation.data;

import org.rajawali3d.math.vector.Vector2;

/**
 * Created by Hugo Huang on 8/9/2017.
 */

public class PathVector {

    public enum VectorType {
        START,
        END,
        LINE_START,
        LINE_END,
        TURN_RIGHT,
        TURN_LEFT,
        OTHER
    }

    public PathVector(VectorType type, Vector2 vector) {
        this.type = type;
        this.vector = vector;
    }

    private VectorType type;
    private Vector2 vector;

    public VectorType getType() {
        return type;
    }

    public void setType(VectorType type) {
        this.type = type;
    }

    public Vector2 getVector() {
        return vector;
    }

    public void setVector(Vector2 vector) {
        this.vector = vector;
    }

    @Override
    public String toString() {
        return "PathVector{" +
                "type='" + type + '\'' +
                ", x='" + vector.getX() + '\'' +
                ", y=" + vector.getY() +
                '}';
    }
}
