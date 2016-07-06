package com.example.mpv.bb8;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class BluetoothActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private TextView btStatus;
    private TextView lcStatus;

    private int BLUETOOTH_ENABLE_RCODE = 1;
    private int LOCATION_PERMISSIONS_RCODE = 2;
    private int LOCATION_ENABLE_RCODE = 3;


    private Boolean btIsOn = false;
    private Boolean lcIsOn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        btStatus = (TextView) findViewById(R.id.btstatus);
        lcStatus = (TextView) findViewById(R.id.lcstatus);

        // Location
        requestLocationPermissions();

        // Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()){
            enableBluetooth();
        }else{
            btIsOn = true;
        }

        checkToContinue();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Location
        /*requestLocationPermissions();

        // Bluetooth
        if (!mBluetoothAdapter.isEnabled()){
            enableBluetooth();
        }else{
            btIsOn = true;
        }*/

        checkToContinue();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BLUETOOTH_ENABLE_RCODE){
            if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_LONG).show();
                btIsOn = false;
            }else{
                btStatus.setText("Bluetooth: ON");
                btIsOn = true;
                checkToContinue();
            }
        } else if (requestCode == LOCATION_ENABLE_RCODE){
            final LocationManager mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
            if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lcStatus.setText("Location: ON");
                lcIsOn = true;
                checkToContinue();
            }else{
                Toast.makeText(getApplicationContext(), "Location must be enabled to continue", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == LOCATION_PERMISSIONS_RCODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                enableLocation();
            } else {
                Toast.makeText(getApplicationContext(), "Location services permissions required to use this app", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void enableBluetooth(){
        if(mBluetoothAdapter == null){
            btStatus.setText(R.string.bluetooth_not_found);
        } else { //check the status and set the button text accordingly
            if (!mBluetoothAdapter.isEnabled()) {
                btStatus.setText(R.string.bluetooth_is_currently_switched_off);
                Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(bluetoothIntent, BLUETOOTH_ENABLE_RCODE);
            }
        }
    }

    private void requestLocationPermissions(){
        if (Build.VERSION.SDK_INT >= 23){
            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                enableLocation();
            } else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                        && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)){
                    lcStatus.setText("Location services permissions required to use this app");
                    lcIsOn = false;
                }
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSIONS_RCODE);
            }
        } else {
            enableLocation();
        }
    }

    private void enableLocation()
    {
        final LocationManager mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            displayLocationSettingsRequest(getApplicationContext());
        }else{
            lcStatus.setText("Location: ON");
            lcIsOn = true;
        }
    }

    private void checkToContinue(){
        if(btIsOn && lcIsOn){
            startActivity(new Intent(BluetoothActivity.this, BB8ConnectionActivity.class));
        }
    }

    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //Toast.makeText(getApplicationContext(), "All location settings are satisfied.", Toast.LENGTH_SHORT).show();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //Toast.makeText(getApplicationContext(), "Location settings are not satisfied. Show the user a dialog to upgrade location settings", Toast.LENGTH_SHORT).show();

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(BluetoothActivity.this, LOCATION_ENABLE_RCODE);
                        } catch (IntentSender.SendIntentException e) {
                            //Toast.makeText(getApplicationContext(), "PendingIntent unable to execute request.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        //Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }
}


