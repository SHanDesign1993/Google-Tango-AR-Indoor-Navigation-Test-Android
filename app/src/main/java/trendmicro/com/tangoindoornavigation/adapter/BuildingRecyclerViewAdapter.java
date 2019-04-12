package trendmicro.com.tangoindoornavigation.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import trendmicro.com.tangoindoornavigation.R;
import trendmicro.com.tangoindoornavigation.db.ADFDao;
import trendmicro.com.tangoindoornavigation.db.BuildingDao;
import trendmicro.com.tangoindoornavigation.db.PointDao;
import trendmicro.com.tangoindoornavigation.db.dto.ADFInfo;
import trendmicro.com.tangoindoornavigation.db.dto.BuildingInfo;

/**
 * Created by hugo on 25/08/2017.
 */

public class BuildingRecyclerViewAdapter extends RecyclerView.Adapter<BuildingRecyclerViewAdapter.NormalTextViewHolder>  {
    private static final String TAG = BuildingRecyclerViewAdapter.class.getSimpleName();
    private List<BuildingInfo> mBuildingList;

    private final LayoutInflater mLayoutInflater;
    private static Context mContext;
    public BuildingRecyclerViewAdapter(Context context, List<BuildingInfo> buildingList) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mBuildingList = buildingList;

    }

    public void updateData(List<BuildingInfo> buildingList){
        mBuildingList = buildingList;
    }

    @Override
    public NormalTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new NormalTextViewHolder(mLayoutInflater.inflate(R.layout.building_list_row, parent, false));
    }

    @Override
    public void onBindViewHolder(NormalTextViewHolder holder, int position) {
        holder.txtBuildingName.setText(mBuildingList.get(position).getBuildingName());
    }

    @Override
    public int getItemCount() {
        return mBuildingList == null ? 0 : mBuildingList.size();
    }

    public class NormalTextViewHolder extends RecyclerView.ViewHolder {
        TextView txtBuildingName;
        NormalTextViewHolder(View view) {
            super(view);
            txtBuildingName = (TextView)view.findViewById(R.id.building_name);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "getLayoutPosition = " + getLayoutPosition());

                    new AsyncTask<Object, Void, Object>() {

                        @Override
                        protected void onPreExecute(){

                        }

                        @Override
                        protected Void doInBackground(Object... params) {

                            //@TODO : remove ADF from Tango space
                            PointDao pointDao = new PointDao(mContext);
                            ADFDao adfDao = new ADFDao(mContext);

                            // remove points
                            List<ADFInfo> listADF = adfDao.queryADFsByBuildingId(Integer.parseInt(mBuildingList.get(getLayoutPosition()).getId()));
                            for(int i = 0; i < listADF.size(); i++) {
                                pointDao.removeAllPointsByUUID(listADF.get(i).getUUID());
                            }

                            // remove ADFs
                            adfDao.removeADFByBuildingId(Integer.parseInt(mBuildingList.get(getLayoutPosition()).getId()));

                            // remove buildings
                            BuildingDao buildingDao = new BuildingDao(mContext);
                            buildingDao.removeBuildingByID(Integer.parseInt(mBuildingList.get(getLayoutPosition()).getId()));

                            for (int i = 0; i < mBuildingList.size(); i++) {
                                if (mBuildingList.get(i).getId().equals(mBuildingList.get(getLayoutPosition()).getId())) {
                                    mBuildingList.remove(i);
                                    break;
                                }
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Object object){
                            notifyDataSetChanged();
                        }

                    }.execute();
                }
            });
        }
    }
}
