package com.example.mpv.bb8;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;


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
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Location
        requestLocationPermissions();

        // Bluetooth
        if (!mBluetoothAdapter.isEnabled()){
            enableBluetooth();
        }else{
            btIsOn = true;
        }

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
            buildAlertMessageNoGps();
        }else{
            lcStatus.setText("Location: ON");
            lcIsOn = true;
        }
    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,  final int id) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), LOCATION_ENABLE_RCODE);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void checkToContinue(){
        if(btIsOn && lcIsOn){
            startActivity(new Intent(BluetoothActivity.this, BB8ConnectionActivity.class));
        }
    }
}


