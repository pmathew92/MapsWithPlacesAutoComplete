package com.prince.custommap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, AutoCompleteAdapter.PlaceAutoCompleteInterface {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    private static final int REQUEST_CHECK_SETTINGS = 2;

    private static final LatLngBounds BOUNDS_INDIA = new LatLngBounds(
            new LatLng(23.63936, 68.14712), new LatLng(28.20453, 97.34466));


    private GoogleMap mMap;
    private Button mSelectButton;
    private TextView mPrimaryAddress, mSecondaryAddress;
    private LinearLayout mAddressLayout;
    private ImageButton mCurrentLocation;
    private Location mLocation;
    private LocationRequest mLocationRequest;

    protected GoogleApiClient mGoogleApiClient;

    private Double mLatitude, mLongitude;

    private EditText mSearchText;
    private RecyclerView mRecyclerView;
    private AutoCompleteAdapter mAdapter;
    private ImageView mCustomMarker;

    private static boolean sCameraMoved = true;

    private AddressResultReceiver mAddressResultReceiver;


    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (isGooglePlayServicesAvailable()) {
            buildGoogleAPiClient();
        } else {
            Log.e(TAG, "Google Play Services not available");
        }
        createLocationRequest();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        mSelectButton = (Button) findViewById(R.id.select_button);
        mPrimaryAddress = (TextView) findViewById(R.id.firstAddress);
        mSecondaryAddress = (TextView) findViewById(R.id.secondAddress);
        mAddressLayout = (LinearLayout) findViewById(R.id.address_layout);

        mCurrentLocation = (ImageButton) findViewById(R.id.get_current_location);
        mCustomMarker = (ImageView) findViewById(R.id.map_custom_marker);

        mSearchText = (EditText) findViewById(R.id.search_box);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        MapFragment mapFragment = ((MapFragment)getFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(this);

        isGpsOn();

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new AutoCompleteAdapter(this, R.layout.layout_recommendation, mGoogleApiClient, BOUNDS_INDIA, null, this);
        mRecyclerView.setAdapter(mAdapter);


        mAddressResultReceiver = new AddressResultReceiver(null);

        mSearchText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mSearchText.setCursorVisible(true);
                return false;
            }
        });

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (!s.equals("") && mGoogleApiClient.isConnected()) {
                    mAdapter.getFilter().filter(s.toString());
                } else if (!mGoogleApiClient.isConnected()) {
                    Log.e(TAG, "API  NOT CONNECTED");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString().equals("")){
                    Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                          mAdapter.getFilter().filter(null);
                        }
                    },500);
                }
            }
        });

        mCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isGpsOn();
            }
        });

        mSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Sending the data back to main activiy via onActivity Result
                if (mLongitude != null && mLongitude != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("address1", mPrimaryAddress.getText().toString());
                    resultIntent.putExtra("address2", mSecondaryAddress.getText().toString());
                    resultIntent.putExtra("latitude", mLatitude);
                    resultIntent.putExtra("longitude", mLongitude);
                    setResult(Activity.RESULT_OK, resultIntent);
                    MapsActivity.this.finish();
                } else {
                    Toast.makeText(getBaseContext(), "Select a location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Method to initialize LocationRequest
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY | LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    // Function to build the Google Api Client..
    protected synchronized void buildGoogleAPiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(20.5937, 78.9629), 4.0f));
    }


    @Override
    public void onPlaceClick(ArrayList<AutoCompleteAdapter.PlaceAutoComplete> mResultList, int position) {
        if (mResultList != null) {
            try {
                final String placeId = String.valueOf(mResultList.get(position).getPlaceId());

                mSearchText.setText("");
                mAdapter.clearList();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
                mSearchText.setCursorVisible(false);
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(@NonNull PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                            places.release();
                            return;
                        }
                        Place place = places.get(0);
                        sCameraMoved = false;
                        mLatitude = place.getLatLng().latitude;
                        mLongitude = place.getLatLng().longitude;
                        mPrimaryAddress.setText(place.getName());
                        mSecondaryAddress.setText(place.getAddress());
                        mAddressLayout.setVisibility(View.VISIBLE);
                        loadMap();
                        places.release();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }
    }


    /**
     * Method to display the location on Map
     */
    public void loadMap() {
        if (mLatitude != null && mLongitude != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(mLatitude, mLongitude))      // Sets the center of the map to location user
                    .zoom(17)                   // Sets the zoom// Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mCustomMarker.setVisibility(View.VISIBLE);

            mMap.getUiSettings().setCompassEnabled(false);
            mMap.getUiSettings().setIndoorLevelPickerEnabled(false);

            mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    Location location = new Location("");
                    if (sCameraMoved) {
                        mLatitude = mMap.getCameraPosition().target.latitude;
                        mLongitude = mMap.getCameraPosition().target.longitude;
                        location.setLatitude(mLatitude);
                        location.setLongitude(mLongitude);
                        convertLocationToAddress(location);
                    }
                    sCameraMoved = true;
                }
            });
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mCurrentLocation.setVisibility(View.VISIBLE);
                } else {
                    mCurrentLocation.setVisibility(View.GONE);
                }
                return;
            }
        }
    }

    protected void startLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_ACCESS_FINE_LOCATION);
                Log.d(TAG, "Permission Not Granted");
            }

        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
    }

    /**
     * Method to stop the regular location updates
     */
    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocation = location;
    }


    /**
     * Method to get current location and do reverse geocoding of the location
     */
    private void getCurrentLocationAddress() {
        if (mLocation != null) {
            mLatitude = mLocation.getLatitude();
            mLongitude = mLocation.getLongitude();
            convertLocationToAddress(mLocation);
            loadMap();
        }
    }

    /**
     * Method to check if google play services are enabled or not
     *
     * @return boolean status
     */
    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(this, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    /**
     * Method to check if GPS is on or not
     */
    private void isGpsOn() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        getCurrentLocationAddress();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(
                                    MapsActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Exception : " + e);
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e(TAG, "Location settings are not satisfied.");
                        break;
                }
            }
        });

    }

    /**
     * Method to convert latitude and longitude to  address via reverse-geocoding
     */
    private void convertLocationToAddress(Location location) {
        Intent intent = new Intent(this, GeoCoderIntentService.class);
        intent.putExtra(Constants.RECEIVER, mAddressResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, location);
        startService(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if(mLocation!=null){
                            getCurrentLocationAddress();
                        }else{
                            Handler handler=new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    getCurrentLocationAddress();
                                }
                            },2000);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                    default:
                        break;
                }
                break;
        }
    }


    private class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        protected void onReceiveResult(final int resultCode, Bundle resultData) {

            final String address1 = resultData.getString(Constants.ADDRESS_DATA_KEY1);
            final String address2 = resultData.getString(Constants.ADDRESS_DATA_KEY2);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (resultCode == Constants.SUCCESS_RESULT) {
                        mPrimaryAddress.setText(address1);
                        mSecondaryAddress.setText(address2);
                        mAddressLayout.setVisibility(View.VISIBLE);
                    } else {
                        Log.e(TAG, "Error while fetching data");
                        mLatitude = mLongitude = null;
                        mPrimaryAddress.setText(address1);
                        mSecondaryAddress.setText("");
                    }
                }
            });
        }
    }

}
