package trendmicro.com.tangoindoornavigation.ui;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.adapter.BuildingRecyclerViewAdapter;
import trendmicro.com.tangoindoornavigation.db.BuildingDao;
import trendmicro.com.tangoindoornavigation.db.dto.BuildingInfo;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by hugo on 25/08/2017.
 */

public class BuildingActivity extends Activity implements View.OnClickListener{

    private static final String TAG = BuildingActivity.class.getSimpleName();
    private Button mAddBuildingButton;
    private EditText mNewBuildingText;
    private RecyclerView mBuildingListView;
    private BuildingDao mBuiildingDao;
    private List<BuildingInfo> mBuildingList = new ArrayList<BuildingInfo>();
    private BuildingRecyclerViewAdapter mBuildingRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_buildings);
        mNewBuildingText = (EditText) findViewById(R.id.txt_building);
        mAddBuildingButton = (Button) findViewById(R.id.add_building);
        mAddBuildingButton.setOnClickListener(this);

        //mBuildingListView = (ListView) findViewById(R.id.building_list);

        mBuiildingDao = new BuildingDao(getApplicationContext());
        //mBuildingArrayAdapter = new BuildingRecyclerViewAdapter(this, mBuildingList);
        //mBuildingListView.setAdapter(mBuildingArrayAdapter);

        mBuildingListView = (RecyclerView)findViewById(R.id.building_recyclerView);
        mBuildingListView.setLayoutManager(new LinearLayoutManager(this));
        mBuildingRecyclerViewAdapter = new BuildingRecyclerViewAdapter(this, mBuildingList);
        mBuildingListView.setAdapter(mBuildingRecyclerViewAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick in : " + v.toString());
        if (v == null) {
            Log.e(TAG, "v is null. skip.");
            return;
        }
        switch (v.getId()) {
            case R.id.add_building:
                addBuildingClicked();
                break;
            default:
                Log.e(TAG, "Unknown click event. skip.");
                break;
        }
    }

    private void addBuildingClicked() {
        if (!mNewBuildingText.getText().toString().trim().equals("")) {
            final String newBuildingText = mNewBuildingText.getText().toString();
            Log.i(TAG, "newBuildingText : " + newBuildingText);
            new AsyncTask<Object, Void, Object>() {

                @Override
                protected void onPreExecute(){

                }

                @Override
                protected Void doInBackground(Object... params) {
                    BuildingInfo buildingInfo = new BuildingInfo();
                    buildingInfo.setBuildingName(newBuildingText);
                    mBuiildingDao.addBuilding(buildingInfo);
                    return null;
                }

                @Override
                protected void onPostExecute(Object object){
                    mNewBuildingText.setText("");
                    updateList();
                }

            }.execute();
        }
    }

    private void updateList() {
        new AsyncTask<Object, Void, Object>() {

            @Override
            protected void onPreExecute(){

            }

            @Override
            protected Void doInBackground(Object... params) {
                // Update Building Listview.
                mBuildingList = mBuiildingDao.queryAllBuildings();
                Log.i(TAG, "mBuildingList.size() : " + mBuildingList.size());
                return null;
            }

            @Override
            protected void onPostExecute(Object object){
                mBuildingRecyclerViewAdapter.updateData(mBuildingList);
                mBuildingRecyclerViewAdapter.notifyDataSetChanged();
            }

        }.execute();
    }

}
