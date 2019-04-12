package  trendmicro.com.tangoindoornavigation.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hugo on 28/07/2017.
 */
public class TableRegistration {

    private TableRegistration() {

    }

    private static List<BasicDbHelper.Table> sTables = null;

    public static List<BasicDbHelper.Table> getTables() {
        if (sTables == null) {
            sTables = new ArrayList<BasicDbHelper.Table>();
            sTables.add(new PointDao.Table()); //Add table sample code.
            sTables.add(new BuildingDao.Table());
            sTables.add(new ADFDao.Table());
        }
        return sTables;
    }

}