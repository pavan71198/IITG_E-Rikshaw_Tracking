package com.codingclubiitg.rickshawpassenger;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.text.InputType;
import android.widget.LinearLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.app.ProgressDialog;
// android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.sql.Timestamp;

public class DisplayActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private HashMap<String, Object> driver_data_map = new HashMap<>();
    private HashMap<String, Object> location_data_map = new HashMap<>();
    private GoogleMap mMap;
    private DataSnapshot driver_data;
    private DataSnapshot location_data;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        db = FirebaseFirestore.getInstance();
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_info:
                Intent intent = new Intent(this, InfoActivity.class);
                startActivity(intent);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onInfoWindowClick(Marker marker) {
        final String driver_id = marker.getTag().toString();

        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:" + "+91" + marker.getTag().toString()));//change the number
        startActivity(dialIntent);

        final String[] options = {"Positive", "Unavailable", "Did not pick up", "Other/Complaint"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How did the driver respond?");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 3){
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(DisplayActivity.this);
                    builder2.setTitle("File a complaint");

                    // Set up the input
                    final EditText webmail = new EditText(DisplayActivity.this);
                    final EditText input = new EditText(DisplayActivity.this);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    LinearLayout layout = new LinearLayout(DisplayActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    webmail.setHint("Your Webmail");
                    input.setHint("Complaint");
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    webmail.setInputType(InputType.TYPE_CLASS_TEXT);
                    layout.addView(webmail);
                    layout.addView(input);
                    builder2.setView(layout);
                    builder2.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String complaint_text = input.getText().toString();
                            String webmail_text = webmail.getText().toString();
                            Map<String, Object> complaint = new HashMap<>();
                            Date date = new Date();
                            long time = date.getTime();
                            Timestamp ts = new Timestamp(time);
                            complaint.put("DriverID", driver_id);
                            complaint.put("Complaint", complaint_text);
                            complaint.put("Webmail", webmail_text);
                            complaint.put("Timestamp", ts.toString());

                            db.collection("complaints").add(complaint);

                        }
                    });
                    builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder2.show();

                }

                else{
                    Map<String, Object> feedback = new HashMap<>();
                    feedback.put("DriverID", driver_id);
                    feedback.put("Feedback", options[which]);

                    db.collection("feedback").add(feedback);
                }
            }
        });
        builder.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Authenticate with Firebase when the Google map is loaded
        mMap = googleMap;
        mMap.setMaxZoomPreference(16);
        loginToFirebase();
        mMap.setOnInfoWindowClickListener(this);
    }

    private void loginToFirebase() {
        String email = getString(R.string.firebase_email);
        String password = getString(R.string.firebase_password);
        // Authenticate with Firebase and subscribe to updates
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    subscribeToUpdates();
                    Log.d(TAG, "firebase auth success");
                } else {
                    Log.d(TAG, "firebase auth failed");
                }
            }
        });
    }

    private void subscribeToUpdates() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(getString(R.string.firebase_path));
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                String key = dataSnapshot.getKey();
                if(key.equals("Drivers")){
                    driver_data = dataSnapshot;
                    driver_data_map = (HashMap<String, Object>) driver_data.getValue();
                    if (!location_data_map.isEmpty() && !driver_data_map.isEmpty()) {
                        for (DataSnapshot child : location_data.getChildren()) {
                            setMarker(child);
                        }
                    }
                }
                else {
                    location_data = dataSnapshot;
                    location_data_map = (HashMap<String, Object>) location_data.getValue();
                    if (!location_data_map.isEmpty() && !driver_data_map.isEmpty()) {
                        for (DataSnapshot child : location_data.getChildren()) {
                            setMarker(child);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                String key = dataSnapshot.getKey();
                if(key.equals("Drivers")){
                    driver_data = dataSnapshot;
                    driver_data_map = (HashMap<String, Object>) driver_data.getValue();
                    if (!location_data_map.isEmpty() && !driver_data_map.isEmpty()) {
                        for (DataSnapshot child : location_data.getChildren()) {
                            setMarker(child);
                        }
                    }
                }
                else {
                    location_data = dataSnapshot;
                    location_data_map = (HashMap<String, Object>) location_data.getValue();
                    if (!location_data_map.isEmpty() && !driver_data_map.isEmpty()) {
                        for (DataSnapshot child : location_data.getChildren()) {
                            setMarker(child);
                        }
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.d(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void setMarker(DataSnapshot dataSnapshot) {
        // When a location update is received, put or update
        // its value in mMarkers, which contains all the markers
        // for locations received, so that we can build the
        // boundaries required to show them all on the map at once
        String key = dataSnapshot.getKey();
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(DisplayActivity.this));
        double lat_top = 26.201951;
        double lat_bottom = 26.181968;
        double lng_right = 91.703635;
        double lng_left = 91.685964;

        HashMap<String, Object> value_location = (HashMap<String, Object>) location_data_map.get(key);
        HashMap<String, Object> value_driver = (HashMap<String, Object>) driver_data_map.get(key);
        Log.d(TAG, value_driver.toString());
        Log.d(TAG, value_location.toString());

        long time = Long.parseLong(value_location.get("time").toString());
        if (Calendar.getInstance().getTime().getTime() - time < 600000){
            double lat = Double.parseDouble(value_location.get("latitude").toString());
            double lng = Double.parseDouble(value_location.get("longitude").toString());
            if ((lat <= lat_top && lat >= lat_bottom) && (lng <= lng_right && lng >= lng_left)) {
                Log.d(TAG, "rendering");
                int pssg = Integer.parseInt(value_driver.get("passengers").toString());//Integer.parseInt(value.get("passengers").toString());
                String vehicle_no = value_driver.get("Vehicle Number").toString();

                LatLng location = new LatLng(lat, lng);
                MarkerOptions opts = new MarkerOptions().title(value_driver.get("Mobile Number").toString()).position(location);

                if (pssg == 0 || pssg == 1) {
                    opts.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                }

                if (pssg == 2 || pssg == 3) {
                    opts.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                }

                if (pssg == 4) {
                    opts.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                opts.snippet("Driver : " + value_driver.get("Name").toString() + "\n" + "Number Of Passengers : " + Integer.toString(pssg) + "\n" + "Tap to call");

                if (!mMarkers.containsKey(key)) {
                    Marker mrkr = mMap.addMarker(opts);
                    mMarkers.put(key, mrkr);
                    mMarkers.get(key).setTag(value_driver.get("Mobile Number"));
                } else {
                    mMarkers.get(key).setIcon(opts.getIcon());
                    mMarkers.get(key).setSnippet(opts.getSnippet());
                    mMarkers.get(key).setPosition(location);
                    mMarkers.get(key).setTag(value_driver.get("Mobile Number"));
                }
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (Marker marker : mMarkers.values()) {
                    builder.include(marker.getPosition());
                }

                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));
            }
        }

    }

}