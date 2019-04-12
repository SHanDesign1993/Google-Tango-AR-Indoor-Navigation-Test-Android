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

import  trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.db.ADFDao;
import trendmicro.com.tangoindoornavigation.db.PointDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointWithADFInfo;


/**
 * Created by hugo on 01/09/2017.
 */

public class TargetFloorConfirmDialog extends DialogFragment {
    public static final String ADF_UUID = "adf_uuid";
    public static final String POINT_ID = "point_id";
    TextView mConfirmDetailTextView;
    CallbackListener mCallbackListener;
    Button mConfirmButton;
    PointWithADFInfo mPointWithADFInfo;
    int mCurrFloor = 0;

    public interface CallbackListener {
        void onTargetFloorConfirm(int currFloor, PointWithADFInfo targetPointInfo);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbackListener = (CallbackListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflator.inflate(R.layout.target_floor_confirm_dialog, container, false);

        mConfirmDetailTextView = (TextView) dialogView.findViewById(R.id.confirm_detail);

        setCancelable(false);

        mConfirmButton = (Button) dialogView.findViewById(R.id.confirm);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbackListener.onTargetFloorConfirm(mCurrFloor, mPointWithADFInfo);
                dismiss();
            }
        });

        String UUID = this.getArguments().getString(ADF_UUID);
        String pointID = this.getArguments().getString(POINT_ID);

        queryNavigationPointInfo(UUID, pointID);

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
                    mConfirmDetailTextView.setText(getResources().getString(R.string.target_floor_confirm_detail,
                            String.valueOf(pointWithADFInfo.getADFInfo().getFloor()), pointWithADFInfo.getADFInfo().getADFName(), pointWithADFInfo.getADFInfo().getUUID()));
                    mPointWithADFInfo = pointWithADFInfo;
                    mCurrFloor = mPointWithADFInfo.getADFInfo().getFloor();
                }
            }

        }.execute();
    }
}
