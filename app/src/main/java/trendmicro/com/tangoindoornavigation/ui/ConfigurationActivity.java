package trendmicro.com.tangoindoornavigation.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.text.DecimalFormat;

import trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.preference.NavigationSharedPreference;

/**
 * Created by hugo on 05/09/2017.
 */

public class ConfigurationActivity extends Activity implements View.OnClickListener  {

    private static final String TAG = ConfigurationActivity.class.getSimpleName();
    private ToggleButton mDebugToggleButton;
    private EditText mQuadTreeStartText;
    private EditText mQuadTreeRangeText;
    private EditText mDetectDistanceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configuration);
        mDebugToggleButton = (ToggleButton) findViewById(R.id.enable_grid);
        mQuadTreeStartText = (EditText) findViewById(R.id.txt_quad_tree_start);
        mQuadTreeRangeText =  (EditText) findViewById(R.id.txt_quad_tree_range);
        mDetectDistanceText =  (EditText) findViewById(R.id.txt_detect_distance);

        mDebugToggleButton.setOnClickListener(this);

        mQuadTreeStartText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    int quadTreeStart = -120;
                    if (mQuadTreeStartText.getText().toString().startsWith("-")) {
                        quadTreeStart = Integer.parseInt(mQuadTreeStartText.getText().toString());
                    } else {
                        quadTreeStart = Integer.parseInt("-" + mQuadTreeStartText.getText().toString());
                    }
                    NavigationSharedPreference.setQuadTreeStart(getApplicationContext(), quadTreeStart);
                    Log.i(TAG, "[onFocusChange]quadTreeStart = " + quadTreeStart);
                }
            }
        });

        mQuadTreeRangeText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    int quadTreeRange = Integer.parseInt(mQuadTreeRangeText.getText().toString());
                    NavigationSharedPreference.setQuadTreeRange(getApplicationContext(), quadTreeRange);
                    Log.i(TAG, "[onFocusChange]quadTreeRange = " + quadTreeRange);
                }
            }
        });

        mDetectDistanceText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus) {
                    double detectDistance = Double.parseDouble(mDetectDistanceText.getText().toString());
                    NavigationSharedPreference.setDetectDistance(getApplicationContext(), detectDistance);
                    Log.i(TAG, "[onFocusChange]detectDistance = " + detectDistance);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean isDebug = NavigationSharedPreference.getEnableGrid(getApplicationContext());
        mDebugToggleButton.setChecked(isDebug);
        if (String.valueOf(NavigationSharedPreference.getQuadTreeStart(getApplicationContext())).startsWith("-")) {
            mQuadTreeStartText.setText(String.valueOf(NavigationSharedPreference.getQuadTreeStart(getApplicationContext())).replace("-", ""));
        } else {
            mQuadTreeStartText.setText(String.valueOf(NavigationSharedPreference.getQuadTreeStart(getApplicationContext())));
        }

        mQuadTreeRangeText.setText(String.valueOf(NavigationSharedPreference.getQuadTreeRange(getApplicationContext())));
        mDetectDistanceText.setText(String.valueOf(NavigationSharedPreference.getDetectDistance(getApplicationContext())));
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
            case R.id.enable_grid:
                debugClicked();
                break;

            default:
                Log.e(TAG, "Unknown click event. skip.");
                break;
        }
    }

    private void debugClicked() {
        boolean isDebug = mDebugToggleButton.isChecked();
        NavigationSharedPreference.setEnableGrid(getApplicationContext(), isDebug);
    }
}
