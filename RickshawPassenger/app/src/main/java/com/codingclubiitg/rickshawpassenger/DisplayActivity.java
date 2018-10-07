package com.codingclubiitg.rickshawpassenger;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.text.InputType;
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

public class DisplayActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private GoogleMap mMap;
    private DataSnapshot driver_data;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        db = FirebaseFirestore.getInstance();
    }



    @Override
    public void onInfoWindowClick(Marker marker) {
        Intent dialIntent = new Intent(Intent.ACTION_DIAL);
        dialIntent.setData(Uri.parse("tel:"+"8802177690"));//change the number
        startActivity(dialIntent);

        final String driver_id = marker.getTag().toString();

        final String[] options = {"Positive", "Unavailable", "Did not pick up", "Other/Complaint"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How did the driver respond?");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 3){
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(DisplayActivity.this);
                    builder2.setTitle("Title");

                    // Set up the input
                    final EditText input = new EditText(DisplayActivity.this);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                    builder2.setView(input);builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String complaint_text = input.getText().toString();
                            Map<String, Object> complaint = new HashMap<>();
                            complaint.put("DriverID", driver_id);
                            complaint.put("Complaint", complaint_text);

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
                    return;
                }
                else {
                    for(DataSnapshot child : dataSnapshot.getChildren()) {
                        setMarker(child);
                    }
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                String key = dataSnapshot.getKey();
                if(!key.equals("Drivers")) {
                    for(DataSnapshot child : dataSnapshot.getChildren()) {
                        setMarker(child);
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

        HashMap<String, Object> value_location = (HashMap<String, Object>) dataSnapshot.getValue();
        HashMap<String, Object> value_driver = (HashMap<String, Object>) driver_data.child(key).getValue();
        double lat = Double.parseDouble(value_location.get("latitude").toString());
        double lng = Double.parseDouble(value_location.get("longitude").toString());
        int pssg = Integer.parseInt(value_driver.get("passengers").toString());//Integer.parseInt(value.get("passengers").toString());
        String vehicle_no = value_driver.get("Vehicle Number").toString();

        Log.d(TAG, "VEGETAL NUMBER " + vehicle_no);

        LatLng location = new LatLng(lat, lng);
        MarkerOptions opts = new MarkerOptions().title(key).position(location);
        if(pssg == 1){
            opts.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        }

        if(pssg == 2){
            opts.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        }

        if(pssg == 3){
            opts.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        }

        if(pssg == 4){
            opts.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }

        opts.snippet("Number Of Passengers : " + Integer.toString(pssg) + "\n" + "Tap to call on ");

        if (!mMarkers.containsKey(key)) {
            Marker mrkr = mMap.addMarker(opts);
            mMarkers.put(key, mrkr);
            mMarkers.get(key).setTag(key);
        } else {
            mMarkers.get(key).setPosition(location);
            mMarkers.get(key).setTag(key);
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : mMarkers.values()) {
            builder.include(marker.getPosition());
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300));

    }

}