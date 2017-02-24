package com.prince.custommap;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.util.List;
import java.util.Locale;


public class GeoCoderIntentService extends IntentService {
    private final static String TAG = GeoCoderIntentService.class.getSimpleName();
    private String errorMessage = "";
    protected ResultReceiver resultReceiver;

    public GeoCoderIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;
        resultReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        Location mLocation = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);
        try {
            addresses = geoCoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "No address found";
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage, null);
        } else {
            Address address = addresses.get(0);
            String completeAddress = "";
            for (int i = 1; i <= address.getMaxAddressLineIndex(); i++) {
                completeAddress = completeAddress + (address.getAddressLine(i) + ",");
            }
            if (completeAddress != "") {
                completeAddress=completeAddress.substring(0,completeAddress.length()-1);
            }
            deliverResultToReceiver(Constants.SUCCESS_RESULT, address.getAddressLine(0), completeAddress);
        }

    }

    private void deliverResultToReceiver(int resultCode, String primaryAddress, String secondaryAddress) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.ADDRESS_DATA_KEY1, primaryAddress);
        bundle.putString(Constants.ADDRESS_DATA_KEY2, secondaryAddress);
        resultReceiver.send(resultCode, bundle);
    }
}
