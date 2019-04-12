package trendmicro.com.tangoindoornavigation.eventbus;

import android.os.Bundle;

/**
 * Created by hugo on 30/08/2017.
 */

public class Event {

    public static final String ON_NAVIGATION_FINISHED = "on_navigation_finished";
    public static final String ON_DELETE_GRID_VECTOR = "on_delete_grid_vector";

    private String mEventString = "";
    private Bundle bundle;
    public Event(String eventString){
        mEventString = eventString;
    }

    public String getEventString() {
        return mEventString;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }
}
