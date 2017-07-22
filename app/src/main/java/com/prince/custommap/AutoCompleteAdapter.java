package com.prince.custommap;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class AutoCompleteAdapter extends RecyclerView.Adapter<AutoCompleteAdapter.PredictionHolder> implements Filterable{
    private static final String TAG = AutoCompleteAdapter.class.getSimpleName();

    public interface PlaceAutoCompleteInterface{
        void onPlaceClick(List<PlaceAutoComplete> mResultList, int position);
    }

    private List<PlaceAutoComplete> mResultList=new ArrayList<>();
    private final Context mContext;
    private final int mLayout;
    private final GoogleApiClient mGoogleApiClient;
    private final AutocompleteFilter mPlaceFilter;
    private LatLngBounds mBounds;

    private final PlaceAutoCompleteInterface mPlaceClickInterface;

    public AutoCompleteAdapter(Context mContext, int mLayout, GoogleApiClient mGoogleApiClient, LatLngBounds mBounds, AutocompleteFilter mPlaceFilter, PlaceAutoCompleteInterface mPlaceClickInterface){
        this.mContext=mContext;
        this.mLayout=mLayout;
        this.mGoogleApiClient=mGoogleApiClient;
        this.mPlaceFilter=mPlaceFilter;
        this.mBounds=mBounds;
        this.mPlaceClickInterface=mPlaceClickInterface;
    }

    /**
     * Setting Bounds for subsequent queries
     */
    public void setmBounds(LatLngBounds mBounds){
        this.mBounds=mBounds;
    }

    /*
   Clear List items
    */
    public void clearList(){
        if(mResultList!=null ){
            mResultList.clear();
        }
        notifyDataSetChanged();
    }

    public static class PredictionHolder extends RecyclerView.ViewHolder{
        private final TextView mAddress1;
        private final TextView mAddress2;
        private final LinearLayout mPredictionLayout;
        public PredictionHolder(View holder){
            super(holder);
            mAddress1=(TextView)holder.findViewById(R.id.primary_address);
            mAddress2=(TextView)holder.findViewById(R.id.secondary_address);
            mPredictionLayout=(LinearLayout)holder.findViewById(R.id.prediction_layout);
        }
    }

    /**
     * Holder class for query result
     */
    public class PlaceAutoComplete{
        private final CharSequence placeId;
        private final CharSequence placeAddress1;
        private final CharSequence placeAddress2;

        public PlaceAutoComplete(CharSequence placeId,CharSequence placeAddress1,CharSequence placeAddress2){
            this.placeId=placeId;
            this.placeAddress1=placeAddress1;
            this.placeAddress2=placeAddress2;
        }

        public String getPlaceAddress1(){
            return placeAddress1.toString();
        }
        public String getPlaceAddress2(){
            return placeAddress2.toString();
        }

        public String getPlaceId(){
            return placeId.toString();
        }

    }


    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results=new FilterResults();
                ArrayList<PlaceAutoComplete> queryResults;
                if(constraint!=null && constraint.toString().trim().length()>0) {
                    queryResults = getAutoComplete(constraint);
                    if(queryResults!=null){
                        results.values = queryResults;
                        results.count = queryResults.size();
                    }
                }
                return  results;
            }
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results!=null&& results.count > 0){
                    mResultList = (ArrayList<PlaceAutoComplete>) results.values;
                }else{
                    mResultList=null;
                }
                notifyDataSetChanged();
            }
        };
    }

    /**
     * Method to call API for each user input
     * @param constraint User input character string
     * @return ArrayList containing suggestion results
     */
    private ArrayList<PlaceAutoComplete> getAutoComplete(CharSequence constraint){
        if(mGoogleApiClient.isConnected()){
            //Making a query and fetching result in a pendingResult

            PendingResult<AutocompletePredictionBuffer> results= Places.GeoDataApi
                    .getAutocompletePredictions(mGoogleApiClient,constraint.toString(),mBounds,mPlaceFilter);

            //Block and wait for 60s for a result
            AutocompletePredictionBuffer autocompletePredictions=results.await(60, TimeUnit.SECONDS);

            final Status status=autocompletePredictions.getStatus();

            // Confirm that the query completed successfully, otherwise return null
            if(!status.isSuccess()){
                Log.e(TAG, "Error getting autocomplete prediction API call: " + status.toString());
                autocompletePredictions.release();
                return null;
            }

            Log.i(TAG, "Query completed. Received " + autocompletePredictions.getCount()
                    + " predictions.");

            // Copy the results into our own data structure, because we can't hold onto the buffer.
            // AutocompletePrediction objects encapsulate the API response (place ID and description).

            Iterator<AutocompletePrediction> iterator=autocompletePredictions.iterator();
            ArrayList resultList=new ArrayList<>(autocompletePredictions.getCount());
            while(iterator.hasNext()){
                AutocompletePrediction prediction=iterator.next();
                resultList.add(new PlaceAutoComplete(prediction.getPlaceId(),prediction.getPrimaryText(null),prediction.getSecondaryText(null)));
            }
            autocompletePredictions.release();
            return resultList;
        }else{
            Log.e(TAG,"GoogleApiClient Not Connected");
            return  null;
        }
    }


    @Override
    public PredictionHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater mLayoutInflater=(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View convertView=mLayoutInflater.inflate(mLayout,parent,false);

        return  new PredictionHolder(convertView);
    }

    @Override
    public void onBindViewHolder(final PredictionHolder holder, int position) {
        holder.mAddress1.setText(mResultList.get(position).getPlaceAddress1());
        holder.mAddress2.setText(mResultList.get(position).getPlaceAddress2());
        holder.mPredictionLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaceClickInterface.onPlaceClick(mResultList,holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        if(mResultList!=null){
            return mResultList.size();
        } else {
            return 0;
        }
    }

    public PlaceAutoComplete getItem(int position) {
        return mResultList.get(position);
    }

}