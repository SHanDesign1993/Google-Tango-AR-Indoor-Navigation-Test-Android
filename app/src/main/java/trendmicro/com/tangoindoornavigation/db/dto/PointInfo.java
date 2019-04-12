package  trendmicro.com.tangoindoornavigation.db.dto;

import com.vividsolutions.jts.geom.Coordinate;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector2;

/**
 * Created by hugo on 28/07/2017.
 */
public class PointInfo {

    public enum PointType {
        GENERAL,
        NAMING,
        STAIRS,
        ELEVATOR
    }

    private String id;
    private String name;
    private String uuid;
    private PointType type;
    private Coordinate coordinate;
    private Quaternion quaternion;
    private Vector2 gridVector;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPointName() {
        return name;
    }

    public void setPointName(String name) {
        this.name = name;
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public PointType getType() {
        return type;
    }

    public void setType(PointType type) {
        this.type = type;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public Quaternion getQuaternion() {
        return quaternion;
    }

    public void setQuaternion(Quaternion quaternion) {
        this.quaternion = quaternion;
    }

    public Vector2 getGridVector() {
        return gridVector;
    }

    public void setGridVector(Vector2 vector) {
        this.gridVector = vector;
    }

    @Override
    public String toString() {
        return "PointInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", uuid='" + uuid + '\'' +
                ", type='" + type + '\'' +
                ", coordinate='" + coordinate.toString() + '\'' +
                ", quaternion='" + quaternion.toString() + '\'' +
                ", grid='" + gridVector.getX() + ", " + gridVector.getY() + '\'' +
                '}';
    }
}
