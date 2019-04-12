package trendmicro.com.tangoindoornavigation.db.dto;

/**
 * Created by hugo on 24/08/2017.
 */
public class BuildingInfo {

    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBuildingName() {
        return name;
    }

    public void setBuildingName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "BuildingInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
