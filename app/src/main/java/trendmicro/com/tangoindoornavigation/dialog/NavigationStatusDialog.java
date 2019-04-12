package trendmicro.com.tangoindoornavigation.dialog;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import  trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.db.ADFDao;
import trendmicro.com.tangoindoornavigation.db.PointDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointWithADFInfo;

/**
 * Created by hugo on 30/08/2017.
 */

public class NavigationStatusDialog extends DialogFragment {

    public static final String IS_NAVIGATION_FINISHED_STRING = "is_navigation_finished";
    public static final String ADF_UUID = "adf_uuid";
    public static final String POINT_ID = "point_id";
    TextView mStatusTextView;
    TextView mStatusDetailTextView;
    CallbackListener mCallbackListener;
    Button mGotItButton;

    public interface CallbackListener {
        void onNavigationStatusGotIt();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbackListener = (CallbackListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflator.inflate(R.layout.navigation_status_dialog, container, false);

        mStatusTextView = (TextView) dialogView.findViewById(R.id.status);
        mStatusDetailTextView =  (TextView) dialogView.findViewById(R.id.status_detail);

        setCancelable(false);

        mGotItButton = (Button) dialogView.findViewById(R.id.got_it);
        mGotItButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbackListener.onNavigationStatusGotIt();
                dismiss();
            }
        });

        boolean isNavigationFinished = this.getArguments().getBoolean(IS_NAVIGATION_FINISHED_STRING);
        if (isNavigationFinished) {
            mStatusTextView.setText(getResources().getString(R.string.navigation_is_finished));
        } else {
            mStatusTextView.setText(getResources().getString(R.string.navigation_is_not_finished));
            String UUID = this.getArguments().getString(ADF_UUID);
            String pointID = this.getArguments().getString(POINT_ID);

            queryNavigationPointInfo(UUID, pointID);
        }
        return dialogView;
    }

    private void queryNavigationPointInfo(final String UUID, final String pointID) {
        new AsyncTask<Object, Void, PointWithADFInfo>() {

            @Override
            protected void onPreExecute(){

            }

            @Override
            protected PointWithADFInfo doInBackground(Object... params) {

                ADFDao adfDao = new ADFDao(getActivity());
                ADFInfo adf = adfDao.queryADFByUUID(UUID);
                PointDao pointDao = new PointDao(getActivity());
                PointInfo point = pointDao.queryPointByID(pointID);
                PointWithADFInfo pointWithADFInfo = new PointWithADFInfo();
                pointWithADFInfo.setADFInfo(adf);
                pointWithADFInfo.setPointInfo(point);
                return pointWithADFInfo;
            }

            @Override
            protected void onPostExecute(PointWithADFInfo pointWithADFInfo){
                if (pointWithADFInfo != null) {
                    mStatusDetailTextView.setText(getResources().getString(R.string.navigation_is_not_finished_detail,
                            String.valueOf(pointWithADFInfo.getADFInfo().getFloor()), pointWithADFInfo.getADFInfo().getADFName(), pointWithADFInfo.getADFInfo().getUUID()));
                }
            }

        }.execute();
    }
}
