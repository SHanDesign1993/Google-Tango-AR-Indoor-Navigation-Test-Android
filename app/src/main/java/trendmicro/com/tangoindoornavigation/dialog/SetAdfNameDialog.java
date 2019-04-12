package  trendmicro.com.tangoindoornavigation.dialog;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;

import java.util.ArrayList;

import  trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.db.ADFDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;

/**
 * Created by hugo on 13/07/2017.
 */
public class SetAdfNameDialog extends DialogFragment {

    EditText mNameEditText;
    TextView mUuidTextView;
    CallbackListener mCallbackListener;
    Button mOkButton;
    Button mCancelButton;
    Spinner mFloorSpinner;
    ArrayList mSpinnerArray;

    public interface CallbackListener {
        void onAdfNameOk(String name, String uuid, int floor);
        void onAdfNameCancelled();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbackListener = (CallbackListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflator.inflate(R.layout.set_adf_info_dialog, container, false);
        getDialog().setTitle(R.string.set_name_dialog_title);
        mNameEditText = (EditText) dialogView.findViewById(R.id.name);
        mUuidTextView = (TextView) dialogView.findViewById(R.id.uuidDisplay);
        mFloorSpinner = (Spinner) dialogView.findViewById(R.id.floor);

        setCancelable(false);
        mOkButton = (Button) dialogView.findViewById(R.id.ok);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbackListener.onAdfNameOk(
                        mNameEditText.getText().toString(),
                        mUuidTextView.getText().toString(),
                        Integer.parseInt(mSpinnerArray.get(mFloorSpinner.getSelectedItemPosition()).toString()));
                dismiss();
            }
        });
        mCancelButton = (Button) dialogView.findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallbackListener.onAdfNameCancelled();
                dismiss();
            }
        });
        String name = this.getArguments().getString(TangoAreaDescriptionMetaData.KEY_NAME);
        final String id = this.getArguments().getString(TangoAreaDescriptionMetaData.KEY_UUID);

        setSpinner();

        if (name != null) {
            mNameEditText.setText(name);
        }
        if (id != null) {
            mUuidTextView.setText(id);
            new AsyncTask<Object, Void, ADFInfo>() {

                @Override
                protected void onPreExecute(){

                }

                @Override
                protected ADFInfo doInBackground(Object... params) {
                    ADFDao adfDao = new ADFDao(getActivity());
                    ADFInfo adf = adfDao.queryADFByUUID(id);

                    return adf;
                }

                @Override
                protected void onPostExecute(ADFInfo adf){
                    if (adf != null) {

                        for (int i = 0; i < mSpinnerArray.size(); i++) {
                            if (adf.getFloor() == Integer.parseInt(mSpinnerArray.get(i).toString())) {
                                mFloorSpinner.setSelection(i);
                                break;
                            }
                        }
                    }
                }

            }.execute();
        }

        return dialogView;
    }

    private void setSpinner() {

        mSpinnerArray = new ArrayList();
        for (int i = -5; i <= 20; i++) {

            if (i != 0) {
                mSpinnerArray.add(i);
            }
        }

        ArrayAdapter<String> adapter =new ArrayAdapter<String>(getActivity(), R.layout.spinner_dropdown_item, mSpinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFloorSpinner.setAdapter(adapter);
        mFloorSpinner.setSelection(5);
    }
}
