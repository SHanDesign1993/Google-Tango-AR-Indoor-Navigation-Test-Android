package  trendmicro.com.tangoindoornavigation;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;

import trendmicro.com.tangoindoornavigation.db.ADFDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import  trendmicro.com.tangoindoornavigation.dialog.SaveAdfDialog;

/**
 * Created by hugo on 13/07/2017.
 */
public class SaveAdfTask extends AsyncTask<Void, Integer, String> {

    private static final String TAG = SaveAdfTask.class.getSimpleName();
    /**
     * Listener for the result of the async ADF saving task.
     */
    public interface SaveAdfListener {
        void onSaveAdfFailed(String adfName);
        void onSaveAdfSuccess(String adfName, String adfUuid);
    }

    Context mContext;
    SaveAdfListener mCallbackListener;
    SaveAdfDialog mProgressDialog;
    Tango mTango;
    String mAdfName;
    int mFloor;
    int mBuildingId;

    public SaveAdfTask(Context context, SaveAdfListener callbackListener, Tango tango, String adfName, int floor, int buildingId) {
        mContext = context;
        mCallbackListener = callbackListener;
        mTango = tango;
        mAdfName = adfName;
        mFloor = floor;
        mBuildingId = buildingId;
        mProgressDialog = new SaveAdfDialog(context);
    }

    /**
     * Sets up the progress dialog.
     */
    @Override
    protected void onPreExecute() {
        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    /**
     * Performs long-running save in the background.
     */
    @Override
    protected String doInBackground(Void... params) {
        String adfUuid = null;
        try {
            TangoConfig config = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
            Boolean isLeanring = config.getBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE);
            // Save the ADF.
            adfUuid = mTango.saveAreaDescription();

            // Read the ADF Metadata, set the desired name, and save it back.
            TangoAreaDescriptionMetaData metadata = mTango.loadAreaDescriptionMetaData(adfUuid);
            metadata.set(TangoAreaDescriptionMetaData.KEY_NAME, mAdfName.getBytes());
            mTango.saveAreaDescriptionMetadata(adfUuid, metadata);

            ADFDao adfDao = new ADFDao(mContext);
            ADFInfo adfInfo = new ADFInfo();
            adfInfo.setADFName(mAdfName);
            adfInfo.setUUID(adfUuid);
            adfInfo.setBuildingId(mBuildingId);
            adfInfo.setFloor(mFloor);
            adfDao.addADF(adfInfo);

        } catch (TangoErrorException e) {
            adfUuid = null; // There's currently no additional information in the exception.
        } catch (TangoInvalidException e) {
            adfUuid = null; // There's currently no additional information in the exception.
        }
        return adfUuid;
    }

    /**
     * Responds to progress update events by updating the UI.
     */
    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mProgressDialog != null) {
            mProgressDialog.setProgress(progress[0]);
        }
    }

    /**
     * Dismisses the progress dialog and call the activity.
     */
    @Override
    protected void onPostExecute(String adfUuid) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        if (mCallbackListener != null) {
            if (adfUuid == null) {
                mCallbackListener.onSaveAdfFailed(mAdfName);
            } else {
                mCallbackListener.onSaveAdfSuccess(mAdfName, adfUuid);
            }
        }
    }
}
