package trendmicro.com.tangoindoornavigation.data;

import com.projecttango.rajawali.Pose;

import org.rajawali3d.math.vector.Vector2;

/**
 * Created by hugo on 11/09/2017.
 */

public class RenderCameraPoseResult {

    private boolean isSetNewGrid;
    private Pose cameraPose;
    private Vector2 gridVector;

    public RenderCameraPoseResult(boolean isSet, Pose pose, Vector2 gridVector) {
        this.isSetNewGrid = isSet;
        this.cameraPose = pose;
        this.gridVector = gridVector;
    }

    public boolean getIsSetNewGrid() {
        return isSetNewGrid;
    }

    public void setIsSetNewGrid(boolean isSet) {
        this.isSetNewGrid = isSet;
    }

    public Pose getCameraPose() {
        return cameraPose;
    }

    public void setCameraPose(Pose pose) {
        this.cameraPose = pose;
    }

    public Vector2 getGridVector() {
        return gridVector;
    }

    public void setGridVector(Vector2 gridVector) {
        this.gridVector = gridVector;
    }

    @Override
    public String toString() {
        return "RenderCameraPoseResult{" +
                "isSetNewGrid='" + isSetNewGrid + '\'' +
                ", getCameraPose='" + getCameraPose().toString() + '\'' +
                ", gridVector=" + gridVector.getX() + ", " + gridVector.getY() +
                '}';
    }
}
