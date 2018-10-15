package com.codingclubiitg.rickshawdriver;

import android.Manifest;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LocationActivity extends AppCompatActivity {
    private static final String TAG = LocationActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Check GPS is enabled
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
            finish();
        }

        Button mStartDrivingButton = (Button) findViewById(R.id.start_driving_button);
        mStartDrivingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTrackerService();
            }
        });

        Button mStopDrivingButton = (Button) findViewById(R.id.stop_driving_button);
        mStopDrivingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopTrackerService();
            }
        });

        final TextView mPassengerCount = (TextView) findViewById(R.id.num_passengers);

        SeekBar mPassengerSeekBar = (SeekBar) findViewById(R.id.num_passengers_seekbar);


        mPassengerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mPassengerCount.setText(String.valueOf(i));
                String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                final String path = "Drivers" + "/" + email.split("@")[0]+"/passengers";
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                ref.setValue(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // Check location permission is granted - if it is, start
        // the service, otherwise request the permission
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST);
        }
    }

    private void startTrackerService() {
        startService(new Intent(this, DriverLocationService.class));
    }

    private void stopTrackerService() {
        stopService(new Intent(this, DriverLocationService.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (!(requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            // Start the service when the permission is granted
            finish();
        }
    }
}
