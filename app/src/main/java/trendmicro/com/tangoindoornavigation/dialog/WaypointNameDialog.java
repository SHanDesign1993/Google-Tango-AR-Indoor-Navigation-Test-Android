package  trendmicro.com.tangoindoornavigation.dialog;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.vividsolutions.jts.geom.Coordinate;

import org.rajawali3d.math.vector.Vector2;

import java.util.ArrayList;
import java.util.HashMap;

import  trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import trendmicro.com.tangoindoornavigation.db.dto.PointInfo;

/**
 * Created by hugo on 24/07/2017.
 */
public class WaypointNameDialog extends DialogFragment {
    public static final String VECTOR_X_KEY = "VECTOR_X";
    public static final String VECTOR_Y_KEY = "VECTOR_Y";
    private EditText mNameEditText;
    private Vector2 vector;
    CallbackListener mCallbackListener;
    Button mOkButton;
    Button mCancelButton;
    Spinner mTypeSpinner;
    ArrayList mSpinnerArray = new ArrayList();
    HashMap<Integer, PointInfo.PointType> mSpinnerTypeMap = new HashMap<Integer, PointInfo.PointType>();

    public interface CallbackListener {
        void onMarkPointOk(String name, Vector2 vector, PointInfo.PointType type);
        void onMarkPointCancelled();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbackListener = (CallbackListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflator.inflate(R.layout.set_name_dialog, container, false);
        getDialog().setTitle(R.string.waypoint_name_dialogTitle);
        mNameEditText = (EditText) dialogView.findViewById(R.id.name);
        mTypeSpinner = (Spinner) dialogView.findViewById(R.id.type);
        setCancelable(false);

        vector = new Vector2((double) getArguments().getSerializable(VECTOR_X_KEY), (double) getArguments().getSerializable(VECTOR_Y_KEY));
        mOkButton = (Button) dialogView.findViewById(R.id.ok);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbackListener.onMarkPointOk(
                        mNameEditText.getText().toString(), vector, mSpinnerTypeMap.get(mTypeSpinner.getSelectedItemPosition()));
                dismiss();
            }
        });
        mCancelButton = (Button) dialogView.findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbackListener.onMarkPointCancelled();
                dismiss();
            }
        });
        String name = this.getArguments().getString(TangoAreaDescriptionMetaData.KEY_NAME);
        if (name != null) {
            mNameEditText.setText(name);
        }

        setSpinner();

        return dialogView;
    }

    private void setSpinner() {

        mSpinnerArray.add(PointInfo.PointType.NAMING);
        mSpinnerTypeMap.put(0, PointInfo.PointType.NAMING);
        mSpinnerArray.add(PointInfo.PointType.STAIRS);
        mSpinnerTypeMap.put(1, PointInfo.PointType.STAIRS);
        mSpinnerArray.add(PointInfo.PointType.ELEVATOR);
        mSpinnerTypeMap.put(2, PointInfo.PointType.ELEVATOR);

        ArrayAdapter<String> adapter =new ArrayAdapter<String>(getActivity(), R.layout.spinner_dropdown_item, mSpinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTypeSpinner.setAdapter(adapter);
    }
}
