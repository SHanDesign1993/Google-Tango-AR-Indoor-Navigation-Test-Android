package trendmicro.com.tangoindoornavigation.db.dto;

/**
 * Created by hugo on 24/08/2017.
 */
public class ADFInfo {

    private String id;
    private String uuid;
    private int building_id;
    private String name;
    private int floor;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public int getBuildingId() {
        return building_id;
    }

    public void setBuildingId(int id) {
        this.building_id = id;
    }

    public String getADFName() {
        return name;
    }

    public void setADFName(String name) {
        this.name = name;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }


    @Override
    public String toString() {
        return "ADFInfo{" +
                "id='" + id + '\'' +
                ", uuid='" + uuid + '\'' +
                ", building_id='" + building_id + '\'' +
                ", name='" + name + '\'' +
                ", floor='" + floor + '\'' +
                '}';
    }
}
