package  trendmicro.com.tangoindoornavigation.dialog;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import  trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.db.ADFDao;
import trendmicro.com.tangoindoornavigation.db.TablesJoinDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import  trendmicro.com.tangoindoornavigation.db.dto.PointInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointWithADFInfo;

/**
 * Created by hugo on 01/08/2017.
 */
public class GoWhereDialog extends DialogFragment {
    private static final String TAG = GoWhereDialog.class.getSimpleName();
    public static final String UUID_KEY = "UUID";
    private Spinner mNameSpinner;
    private HashMap<Integer,PointWithADFInfo> mSpinnerMap;
    CallbackListener mCallbackListener;
    Button mOkButton;
    Button mCancelButton;
    private String mUUID;
    private int mUUIDFloor = 0;

    public interface CallbackListener {
        void onGoWhereOk(int currFloor, PointWithADFInfo targetPointInfo);
        void onGoWhereCancelled();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbackListener = (CallbackListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflator.inflate(R.layout.go_where_dialog, container, false);
        getDialog().setTitle(R.string.waypoint_name_dialogTitle);
        mNameSpinner = (Spinner) dialogView.findViewById(R.id.name);

        mNameSpinner.setEnabled(true);
        setCancelable(false);

        mOkButton = (Button) dialogView.findViewById(R.id.ok);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "[onClick]mNameSpinner.getSelectedItemPosition = " + mNameSpinner.getSelectedItemPosition());
                mCallbackListener.onGoWhereOk(mUUIDFloor, mSpinnerMap.get(mNameSpinner.getSelectedItemPosition()));
                dismiss();
            }
        });
        mCancelButton = (Button) dialogView.findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbackListener.onGoWhereCancelled();
                dismiss();
            }
        });

        mUUID = getArguments().getSerializable(UUID_KEY).toString();
        if (!mUUID.equals("")) {
            new AsyncTask<Object, Void, ArrayList<PointWithADFInfo>>() {

                @Override
                protected void onPreExecute(){

                }

                @Override
                protected ArrayList<PointWithADFInfo> doInBackground(Object... params) {
                    return getPointWithADFList();
                }

                @Override
                protected void onPostExecute(ArrayList<PointWithADFInfo> pointWithADFList){
                    setSpinner(pointWithADFList);
                }

            }.execute();

        }

        return dialogView;
    }

    private ArrayList<PointWithADFInfo> getPointWithADFList() {

        ArrayList<PointWithADFInfo> pointWithADFList = new ArrayList();
        List<PointWithADFInfo> tempPointWithADFList;

        ADFDao adfDao = new ADFDao(getActivity());
        ADFInfo adfInfo = adfDao.queryADFByUUID(mUUID);

        if (adfInfo != null) {
            TablesJoinDao tablesJoinDao = new TablesJoinDao(getActivity());

            mUUIDFloor = adfInfo.getFloor();
            List<ADFInfo> listADF = adfDao.queryADFsByBuildingId(adfInfo.getBuildingId());

            for (int i = 0; i < listADF.size(); i++) {
                if (listADF.get(i).getUUID().equals(mUUID)) {
                    tempPointWithADFList = tablesJoinDao.queryAllNamingPointsWithADFInfoByUUID(mUUID);
                    pointWithADFList.addAll(tempPointWithADFList);
                } else {
                    if (listADF.get(i).getFloor() != adfInfo.getFloor()) {
                        tempPointWithADFList = tablesJoinDao.queryAllNamingPointsWithADFInfoByUUID(listADF.get(i).getUUID());
                        pointWithADFList.addAll(tempPointWithADFList);
                    }
                }
            }
        }

        return pointWithADFList;
    }

    private void setSpinner(ArrayList<PointWithADFInfo> pointWithADFList) {

        int pointSize = pointWithADFList.size();
        Log.i(TAG, "[setSpinner]pointSize = " + pointSize);

        ArrayList spinnerArray = new ArrayList();
        mSpinnerMap = new HashMap<Integer, PointWithADFInfo>();

        for (int i = 0; i < pointSize; i++)
        {
            PointInfo point = pointWithADFList.get(i).getPointInfo();
            ADFInfo adf = pointWithADFList.get(i).getADFInfo();
            //Log.i(TAG, "[setSpinner]pointWithADF = " + pointWithADFList.get(i).toString());
            mSpinnerMap.put(i, pointWithADFList.get(i));
            spinnerArray.add("(" + adf.getFloor() + " F) " + point.getPointName());
        }

        ArrayAdapter<String> adapter =new ArrayAdapter<String>(getActivity(),android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNameSpinner.setAdapter(adapter);

    }
}
