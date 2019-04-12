package trendmicro.com.tangoindoornavigation.db.dto;

/**
 * Created by hugo on 29/08/2017.
 */

public class PointWithADFInfo {

    private PointInfo pointInfo;
    private ADFInfo adfInfo;

    public PointInfo getPointInfo() {
        return pointInfo;
    }

    public void setPointInfo(PointInfo pointInfo) {
        this.pointInfo = pointInfo;
    }

    public ADFInfo getADFInfo() {
        return adfInfo;
    }

    public void setADFInfo(ADFInfo adfInfo) {
        this.adfInfo = adfInfo;
    }

    @Override
    public String toString() {
        return "PointWithADFInfo{" +
                "pointInfo='" + pointInfo.toString() + '\'' +
                ", adfInfo='" + adfInfo.toString() + '\'' +
                '}';
    }
}
