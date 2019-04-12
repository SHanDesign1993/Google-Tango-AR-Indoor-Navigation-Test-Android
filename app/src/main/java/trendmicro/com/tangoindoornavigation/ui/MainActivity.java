package trendmicro.com.tangoindoornavigation.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.atap.tangoservice.Tango;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.db.ADFDao;
import trendmicro.com.tangoindoornavigation.db.BuildingDao;
import trendmicro.com.tangoindoornavigation.db.PointDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import trendmicro.com.tangoindoornavigation.db.dto.BuildingInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointInfo;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    // The unique key string for storing the user's input.
    public static final String USE_AREA_LEARNING = "trendmicro.com.tangoindoornavigation.ui.usearealearning";
    public static final String LOAD_ADF = "trendmicro.com.tangoindoornavigation.ui.loadadf";
    public static final String BUILDING = "trendmicro.com.tangoindoornavigation.ui.building";
    public static final String LOAD_ADF_UUID = "trendmicro.com.tangoindoornavigation.ui.loadadfuuid";

    // Permission request action.
    public static final int REQUEST_CODE_TANGO_PERMISSION = 0;

    // UI elements.
    private ToggleButton mLearningModeToggleButton;
    private ToggleButton mLoadAdfToggleButton;
    private Button mStartButton;
    private Button mADListButton;
    private Button mReconstructionButton;
    private Button mPointReconstructionButton;
    private Button mBuildingButton;
    private Button mConfigurationButton;
    private Spinner mADFSpinner;
    private Spinner mBuildingSpinner;

    private boolean mIsUseAreaLearning;
    private boolean mIsLoadAdf;

    private boolean mIsPermissionGot = false;
    private HashMap<Integer, ADFInfo> mSpinnerADFMap = new HashMap<Integer, ADFInfo>();
    private HashMap<Integer, BuildingInfo> mSpinnerBuildingMap = new HashMap<Integer, BuildingInfo>();
    private ArrayAdapter<String> mADFAdapter;
    private ArrayList mADFSpinnerArray = new ArrayList();
    private ArrayAdapter<String> mBuildingAdapter;
    private ArrayList mBuildingSpinnerArray = new ArrayList();;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();

        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), REQUEST_CODE_TANGO_PERMISSION);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume in : mIsPermissionGot = " + mIsPermissionGot);
        if (mIsPermissionGot) {
            updateUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    private void initUI() {
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        // Set up UI elements.
        mLearningModeToggleButton = (ToggleButton) findViewById(R.id.learning_mode);
        mLoadAdfToggleButton = (ToggleButton) findViewById(R.id.load_adf);
        mStartButton = (Button) findViewById(R.id.start);
        mADListButton = (Button) findViewById(R.id.AdfListView);
        mReconstructionButton = (Button) findViewById(R.id.reconstructionView);
        mPointReconstructionButton = (Button) findViewById(R.id.point_reconstructionView);
        mBuildingButton = (Button) findViewById(R.id.building_view);
        mConfigurationButton =  (Button) findViewById(R.id.configuration);
        mADFSpinner = (Spinner) findViewById(R.id.uuid);
        mBuildingSpinner = (Spinner) findViewById(R.id.buildings);

        mLearningModeToggleButton.setOnClickListener(this);
        mLoadAdfToggleButton.setOnClickListener(this);
        mStartButton.setOnClickListener(this);
        mADListButton.setOnClickListener(this);
        mReconstructionButton.setOnClickListener(this);
        mPointReconstructionButton.setOnClickListener(this);
        mBuildingButton.setOnClickListener(this);
        mConfigurationButton.setOnClickListener(this);

        mIsUseAreaLearning = mLearningModeToggleButton.isChecked();
        mIsLoadAdf = mLoadAdfToggleButton.isChecked();

        mBuildingAdapter =new ArrayAdapter<String>(getApplicationContext(),R.layout.spinner_dropdown_item, mBuildingSpinnerArray);
        mBuildingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBuildingSpinner.setAdapter(mBuildingAdapter);

        mADFAdapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.spinner_dropdown_item, mADFSpinnerArray);
        mADFAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mADFSpinner.setAdapter(mADFAdapter);

        mBuildingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "onItemSelected in : " + position);
                int buildingId = Integer.parseInt(mSpinnerBuildingMap.get(position).getId());
                ADFDao adfDao = new ADFDao(getApplicationContext());
                Log.i(TAG, "buildingId : " + buildingId);
                List<ADFInfo> listADF = adfDao.queryADFsByBuildingId(buildingId);
                Log.i(TAG, "listADF.toString() : " + listADF.toString());
                mADFSpinnerArray.clear();
                mSpinnerADFMap.clear();
                mADFAdapter.clear();

                for (int i = 0; i < listADF.size(); i++) {
                    mSpinnerADFMap.put(i, listADF.get(i));
                    mADFSpinnerArray.add("(" + listADF.get(i).getFloor() + " F) " + listADF.get(i).getADFName());
                }
                mADFAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.i(TAG, "onNothingSelected in");
            }
        });
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "onClick in : " + v.toString());
        if (v == null) {
            Log.e(TAG, "v is null. skip.");
            return;
        }
        switch (v.getId()) {
            case R.id.load_adf:
                loadAdfClicked();
                break;
            case R.id.learning_mode:
                learningModeClicked();
                break;
            case R.id.start:
                startAreaDescriptionActivity();
                break;
            case R.id.AdfListView:
                startAdfListView();
                break;
            case R.id.reconstructionView:
                startReconstructionActivity();
                break;
            case R.id.point_reconstructionView:
                startPointsReconstructionActivity();
                break;
            case R.id.building_view:
                startBuildingActivity();
                break;
            case R.id.configuration:
                startConfigurationActivity();
                break;
            default:
                Log.e(TAG, "Unknown click event. skip.");
                break;
        }
    }

    public void loadAdfClicked() {
        mIsLoadAdf = mLoadAdfToggleButton.isChecked();
    }

    public void learningModeClicked() {
        mIsUseAreaLearning = mLearningModeToggleButton.isChecked();
    }

    private void startAreaDescriptionActivity() {
        Intent startAdIntent = new Intent(this, AreaDescriptionActivity.class);
        startAdIntent.putExtra(USE_AREA_LEARNING, mIsUseAreaLearning);
        startAdIntent.putExtra(LOAD_ADF, mIsLoadAdf);
        if (mSpinnerADFMap.size() > 0) {
            startAdIntent.putExtra(LOAD_ADF_UUID, mSpinnerADFMap.get(mADFSpinner.getSelectedItemPosition()).getUUID());
        } else {
            startAdIntent.putExtra(LOAD_ADF_UUID, "");
        }
        if (mSpinnerBuildingMap.size() > 0) {
            Log.i(TAG, "mBuildingSpinner = " + mSpinnerBuildingMap.get(mBuildingSpinner.getSelectedItemPosition()).getId());
            startAdIntent.putExtra(BUILDING, mSpinnerBuildingMap.get(mBuildingSpinner.getSelectedItemPosition()).getId());
        } else {
            startAdIntent.putExtra(BUILDING, "0");
        }

        startActivity(startAdIntent);
    }

    private void startPointsReconstructionActivity() {
        Intent startPointsReconstructionIntent = new Intent(this, AreaPointsConstructionActivity.class);
        startPointsReconstructionIntent.putExtra(USE_AREA_LEARNING, mIsUseAreaLearning);
        startPointsReconstructionIntent.putExtra(LOAD_ADF, mIsLoadAdf);
        if (mSpinnerADFMap.size() > 0) {
            startPointsReconstructionIntent.putExtra(LOAD_ADF_UUID, mSpinnerADFMap.get(mADFSpinner.getSelectedItemPosition()).getUUID());
        } else {
            startPointsReconstructionIntent.putExtra(LOAD_ADF_UUID, "");
        }
        if (mSpinnerBuildingMap.size() > 0) {
            Log.i(TAG, "mBuildingSpinner = " + mSpinnerBuildingMap.get(mBuildingSpinner.getSelectedItemPosition()).getId());
            startPointsReconstructionIntent.putExtra(BUILDING, mSpinnerBuildingMap.get(mBuildingSpinner.getSelectedItemPosition()).getId());
        } else {
            startPointsReconstructionIntent.putExtra(BUILDING, "0");
        }
        startActivity(startPointsReconstructionIntent);
    }

    private void startReconstructionActivity() {
        Intent startReconstructionIntent = new Intent(this, ReconstructionActivity.class);
        startActivity(startReconstructionIntent);
    }

    private void startAdfListView() {
        Intent startAdfListViewIntent = new Intent(this, AdfUuidListViewActivity.class);
        startActivity(startAdfListViewIntent);
    }

    private void startBuildingActivity() {
        Intent startBuildingIntent = new Intent(this, BuildingActivity.class);
        startActivity(startBuildingIntent);
    }

    private void startConfigurationActivity() {
        Intent startConfigurationIntent = new Intent(this, ConfigurationActivity.class);
        startActivity(startConfigurationIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The result of the permission activity.
        //
        // Note that when the permission activity is dismissed, the MainActivity's
        // onResume() callback is called. Because the Tango Service is connected in the onResume()
        // function, we do not call connect here.
        //
        // Check which request we're responding to.
        if (requestCode == REQUEST_CODE_TANGO_PERMISSION) {
            // Make sure the request was successful.
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.arealearning_permission, Toast.LENGTH_SHORT).show();
                finish();
            } else if (resultCode == RESULT_OK) {
                mIsPermissionGot = true;
            }
        }
    }

    private void updateUI() {

        BuildingDao buildingDao = new BuildingDao(getApplicationContext());
        List<BuildingInfo> buildingList= buildingDao.queryAllBuildings();
        Log.i(TAG, "buildingList.toString() : " + buildingList.toString());
        mBuildingSpinnerArray.clear();
        mSpinnerBuildingMap.clear();
        mBuildingAdapter.clear();
        if (buildingList.size() > 0) {
            for (int i = 0; i < buildingList.size(); i++) {
                mSpinnerBuildingMap.put(i, buildingList.get(i));
                mBuildingSpinnerArray.add(buildingList.get(i).getBuildingName());
            }
        }
        mBuildingAdapter.notifyDataSetChanged();

        if (mBuildingSpinner.getSelectedItemPosition() != -1 && mSpinnerBuildingMap.get(mBuildingSpinner.getSelectedItemPosition()) != null) {
            int buildingId = Integer.parseInt(mSpinnerBuildingMap.get(mBuildingSpinner.getSelectedItemPosition()).getId());
            ADFDao adfDao = new ADFDao(getApplicationContext());
            Log.i(TAG, "buildingId : " + buildingId);
            List<ADFInfo> listADF = adfDao.queryADFsByBuildingId(buildingId);
            Log.i(TAG, "listADF.toString() : " + listADF.toString());
            mADFSpinnerArray.clear();
            mSpinnerADFMap.clear();
            mADFAdapter.clear();

            for (int i = 0; i < listADF.size(); i++) {
                mSpinnerADFMap.put(i, listADF.get(i));
                mADFSpinnerArray.add("(" + listADF.get(i).getFloor() + " F) " + listADF.get(i).getADFName());
            }
            mADFAdapter.notifyDataSetChanged();
        }


        ADFDao adfDao = new ADFDao(getApplicationContext());
        List<ADFInfo> listADF = adfDao.queryAllADFs();
        Log.i(TAG, "[updateUI]pointList.size() = " + listADF.size());
        for (ADFInfo adfInfo : listADF) {
            Log.i(TAG, "[updateUI]adfInfo = " + adfInfo.toString());

        }

        PointDao pointDao = new PointDao(getApplicationContext());
        List<PointInfo> pointList = pointDao.queryAllPoints();
        Log.i(TAG, "[updateUI]pointList.size() = " + pointList.size());
        for (PointInfo point : pointList) {
            //Log.i(TAG, "[updateUI]pointInfo = " + point.toString());

        }
    }
}
