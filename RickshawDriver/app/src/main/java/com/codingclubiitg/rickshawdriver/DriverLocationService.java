package com.codingclubiitg.rickshawdriver;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import android.app.PendingIntent;
import android.app.Service;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.os.Build;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.Manifest;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class DriverLocationService extends Service {
    private static final String TAG = DriverLocationService.class.getSimpleName();

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("location", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        buildNotification();
        loginToFirebase();

    }

    private void buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "location")
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_location);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

//    protected BroadcastReceiver stopReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG, "received stop broadcast");
//            // Stop the service when the notification is tapped
//            unregisterReceiver(stopReceiver);
//            stopSelf();
//        }
//    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroying service");
        stopSelf();
    }

    private void loginToFirebase() {
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email!=null) {
            requestLocationUpdates(email);
        }
    }

    private void requestLocationUpdates(String email) {
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        final String path = getString(R.string.firebase_path) + "/" + email.split("@")[0];
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            // Request location updates and when an update is
            // received, store the location in Firebase
            client.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        Log.d(TAG, "location update " + location);
                        ref.setValue(location);
                    }
                }
            }, null);
        }
    }
}
