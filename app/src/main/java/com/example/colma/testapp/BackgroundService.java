package com.example.colma.testapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class BackgroundService extends Service {
    final static String MY_ACTION = "MY_ACTION";
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private final String TAG = "BackgroundService";
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;

    static DatabaseReference databaseMessages;
    List<Message> messageList = new ArrayList<>();
    ArrayList<Message> distMessageList = new ArrayList<>();
    Map<String, List<Integer>> voteMap = new HashMap<>();
    Context applicationContext;


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private class LocationListener implements android.location.LocationListener {
        private final String TAG = "LocationListener";
        private Location mLastLocation;

        LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.i(TAG, "onLocationChanged: " + location.getLatitude() + ", " + location.getLongitude());
            mLastLocation = location;
            // Update list when location changes
            updateDatabase(location);
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }

    public void updateDatabase(final Location location) {
        String city = getCity(new Loc(location.getLatitude(), location.getLongitude()));

        databaseMessages = FirebaseDatabase.getInstance().getReference("notes/" + city);

        databaseMessages.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                messageList.clear();
                // Store curr city in pref, check if user is still in the same city
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);

                    messageList.add(message);
                }
                if (messageList.size() > 0) {
                    distMessageList = getNotesInRadius(messageList, location);
                    Collections.sort(distMessageList);
                    // Hash map with id as key and list of size two as value (upvote, downvotes),
                    voteMap.clear();
                    getVotes();
                }

                // Send lists to MainActivity
                Intent intent = new Intent();
                intent.setAction(MY_ACTION);

                Bundle databaseInfo = new Bundle();
                databaseInfo.putParcelableArrayList("messagelist", distMessageList);
                databaseInfo.putSerializable("votelist", (Serializable) voteMap);
                databaseInfo.putParcelable("location", location);
                intent.putExtras(databaseInfo);

                sendBroadcast(intent);

                // Send notification if top message changes
                String topMessage;
                SharedPreferences pref = getApplicationContext().getSharedPreferences("NotificationMessage", 0);
                SharedPreferences.Editor editor = pref.edit();


                String prefString = pref.getString("Notification", "nullValMessage");

                if(distMessageList.size() == 0)
                    topMessage = "There are no messages near you currently";
                else
                    topMessage = distMessageList.get(0).getMessageText();

                // Don't spam user with notifications constantly, only if highest voted message changes
                if (!topMessage.equals(prefString))
                {
                    editor.putString("Notification", topMessage);
                    editor.commit();
                    sendNote(topMessage);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // Get vote information from list of nearby messages
    private void getVotes() {
        for (int i = 0; i < distMessageList.size(); i++) {
            List<Integer> votesList = new ArrayList<>();
            votesList.add(distMessageList.get(i).getUpVotes());
            votesList.add(distMessageList.get(i).getDownVotes());
            voteMap.put(distMessageList.get(i).getId(), votesList);
        }
    }

    private ArrayList<Message> getNotesInRadius(List<Message> tempMessageList, Location currLocation) {
        Message currMessage;
        float distanceInMeters;
        int radius, voteThreshold;

        SharedPreferences pref = getApplicationContext().getSharedPreferences("UserPrefs", 0);

        String radiusString = pref.getString("Radius", "50");
        String thresholdString = pref.getString("Threshold", "-50");
        if(radiusString != null && !radiusString.isEmpty())
            radius = Integer.parseInt(radiusString);
        else
            radius = 50;

        if(thresholdString != null && !thresholdString.isEmpty())
            voteThreshold = Integer.parseInt(thresholdString);
        else
            voteThreshold = -50;

        Location noteLocation = new Location("");//provider name is unnecessary
        ArrayList<Message> notesInRadius = new ArrayList<>();

        // add message object to list if it is closer than radius and above vote threshold
        for (int i = 0; i < tempMessageList.size(); i++) {
            currMessage = tempMessageList.get(i);
            noteLocation.setLatitude(currMessage.location.getLatitude());
            noteLocation.setLongitude(currMessage.location.getLongitude());

            distanceInMeters = noteLocation.distanceTo(currLocation);

            if (distanceInMeters < radius && (currMessage.getUpVotes() - currMessage.getDownVotes()) >= voteThreshold)
                notesInRadius.add(currMessage);
        }

        return notesInRadius;
    }

    private String getCity(Loc l) {
        String cityName = "";
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            if(l != null)
            {
                List<Address> addresses = geocoder.getFromLocation(l.getLatitude(), l.getLongitude(), 1);
                cityName = addresses.get(0).getLocality();
            }
            Log.i(TAG, "City: " + cityName);
        } catch (IOException e){

        }

        return cityName;
    }

    public void sendNote(String note) {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel("ID", "Name", importance);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannel.getId());
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        builder = builder
                .setSmallIcon(R.drawable.ic_baseline_create_24px)
                .setContentTitle("Location Notes")
                .setContentText(note)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(contentIntent);

        notificationManager.notify(1, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

//        if (intent.getAction().equals( MainActivity.STOPFOREGROUND_ACTION)) {
//            //your end servce code
//            Log.i(TAG, "onStartCommand: ENDED SERVICE");
//            stopForeground(true);
//            stopSelf();
//        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        startForeground(12345678, getNotification());


        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean network_enabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Location location;

        if (network_enabled) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            updateDatabase(location);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ignored) {

            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking(Context appContext) throws SecurityException, IllegalArgumentException {
        applicationContext = appContext;
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        int LOCATION_INTERVAL = 1000;
        int LOCATION_DISTANCE = 10;
        mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListener );
        mLocationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListener );

    }

    public void stopTracking() {
        this.onDestroy();
    }

    private Notification getNotification() {

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
                builder = new NotificationCompat.Builder(getApplicationContext(), channel.getId()).setAutoCancel(true);
            } else {
                builder = new NotificationCompat.Builder(getApplicationContext()).setAutoCancel(true);
            }
        return builder.build();
    }


    public class LocationServiceBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

}
