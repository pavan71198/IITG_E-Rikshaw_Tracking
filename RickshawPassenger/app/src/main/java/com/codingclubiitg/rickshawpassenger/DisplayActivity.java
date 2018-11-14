package com.codingclubiitg.rickshawpassenger;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.Manifest;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.text.InputType;
import android.widget.LinearLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.app.ProgressDialog;
import android.widget.TextView;
import android.widget.Toast;
// android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
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
import com.google.android.gms.maps.model.BitmapDescriptor;


import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.sql.Timestamp;

public class DisplayActivity extends AppCompatActivity implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = DisplayActivity.class.getSimpleName();
    private HashMap<String, Marker> mMarkers = new HashMap<>();
    private HashMap<String, Object> driver_data_map = new HashMap<>();
    private HashMap<String, Object> location_data_map = new HashMap<>();
    private GoogleMap mMap;
    private DataSnapshot driver_data;
    private DataSnapshot location_data;
    private FirebaseFirestore db;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Snackbar mSnackbarGps;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Construct a FusedLocationProviderClient.
//        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        db = FirebaseFirestore.getInstance();
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            checkGpsEnabled();
            mMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Third and final validation check - ensures GPS is enabled, and if not, prompts to
     * enable it, otherwise all checks pass so start the location tracking service.
     */
    private void checkGpsEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            reportGpsError();
        } else {
            resolveGpsError();
        }
    }

    private void reportGpsError() {

        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.map),"GPS is required for tracking", Snackbar.LENGTH_INDEFINITE)
                .setAction("Enable", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });

        // Changing message text color
        snackbar.setActionTextColor(Color.RED);

        // Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id
                .snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();

    }

    private void resolveGpsError() {
        if (mSnackbarGps != null) {
            mSnackbarGps.dismiss();
            mSnackbarGps = null;
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
//        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
//        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fare_info:
                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(DisplayActivity.this);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {

                    }
                });
                builder.setMessage(getString(R.string.info_line_1)+"\n\n"+getString(R.string.info_line_2));
                builder.setTitle("Fare Info");
                android.support.v7.app.AlertDialog d = builder.create();
                d.show();
                return true;

            case R.id.rules_info:
                android.support.v7.app.AlertDialog.Builder builder2 = new android.support.v7.app.AlertDialog.Builder(DisplayActivity.this);
                builder2.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {

                    }
                });
                builder2.setMessage(getString(R.string.info_line_3));
                builder2.setTitle("Rules");
                android.support.v7.app.AlertDialog d2 = builder2.create();
                d2.show();
                return true;

            case R.id.helpline_info:
                android.support.v7.app.AlertDialog.Builder builder3 = new android.support.v7.app.AlertDialog.Builder(DisplayActivity.this);
                builder3.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {

                    }
                });
                builder3.setMessage(getString(R.string.info_line_4)+"\n\n"+getString(R.string.info_line_5));
                builder3.setTitle("Helpline");
                android.support.v7.app.AlertDialog d3 = builder3.create();
                d3.show();
                return true;

            case R.id.action_refresh:
                Intent refresh = new Intent(this, DisplayActivity.class);
                startActivity(refresh);
                finish();
                return true;

            case R.id.action_feedback:
                Intent feedback = new Intent(this, FeedbackActivity.class);
                startActivity(feedback);
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

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
        mMap.setMaxZoomPreference(16);
        LatLng iitg = new LatLng(26.191683, 91.683292);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(iitg));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
//        loginToFirebase();
        subscribeToUpdates();
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);
        enableMyLocation();
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

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
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
                Drawable rickshawDrawable = getResources().getDrawable(R.drawable.ic_rickshaw);
                BitmapDescriptor markerIcon = getMarkerIconFromDrawable(rickshawDrawable);
                Log.d(TAG, "rendering");
                int pssg = Integer.parseInt(value_driver.get("passengers").toString());//Integer.parseInt(value.get("passengers").toString());
                String vehicle_no = value_driver.get("Vehicle Number").toString();

                LatLng location = new LatLng(lat, lng);
                MarkerOptions opts = new MarkerOptions().title(value_driver.get("Mobile Number").toString()).position(location);

                if (pssg == 0 || pssg == 1) {
                    opts.icon(markerIcon);
                }

                if (pssg == 2 || pssg == 3) {
                    opts.icon(markerIcon);
                }

                if (pssg == 4) {
                    opts.icon(markerIcon);
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