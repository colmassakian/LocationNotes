package com.example.colma.testapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;


public class BackgroundService extends Service {
    final static String MY_ACTION = "MY_ACTION";
    private final LocationServiceBinder binder = new LocationServiceBinder();
    private final String TAG = "BackgroundService";
    private LocationListener mLocationListener;
    private LocationManager mLocationManager;
    private NotificationManager notificationManager;

    private final int LOCATION_INTERVAL = 500;
    private final int LOCATION_DISTANCE = 10;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private class LocationListener implements android.location.LocationListener
    {
        private Location lastLocation = null;
        private final String TAG = "LocationListener";
        private Location mLastLocation;

        public LocationListener(String provider)
        {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.i(TAG, "onLocationChanged: " + location.getLatitude() + ", " + location.getLongitude());
            mLastLocation = location;

            Intent intent = new Intent();
            intent.setAction(MY_ACTION);
            intent.putExtra("LOCATION", location);
            sendBroadcast(intent);
            sendNote(Double.toString(location.getLatitude()));

        }

        @Override
        public void onProviderDisabled(String provider)
        {

        }

        @Override
        public void onProviderEnabled(String provider)
        {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }
    }

    public void sendNote(String note){
        Toast.makeText(BackgroundService.this, "Notif called", Toast.LENGTH_SHORT).show();
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel("ID", "Name", importance);
            notificationManager.createNotificationChannel(notificationChannel);
            builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannel.getId());
        } else {
            builder = new NotificationCompat.Builder(getApplicationContext());
        }

        builder = (NotificationCompat.Builder) builder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Test")
                .setContentText(note)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true);
        notificationManager.notify(001, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate()
    {
        startForeground(12345678, getNotification());
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception ex) {

            }
        }
    }

    private void initializeLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    public void startTracking() {
        initializeLocationManager();
        mLocationListener = new LocationListener(LocationManager.GPS_PROVIDER);

        try {
            mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListener );

        } catch (java.lang.SecurityException ex) {
        } catch (IllegalArgumentException ex) {
        }

    }

    public void stopTracking() {
        this.onDestroy();
    }

    private Notification getNotification() {

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = null;

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
