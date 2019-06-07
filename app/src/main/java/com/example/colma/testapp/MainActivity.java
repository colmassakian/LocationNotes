package com.example.colma.testapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String EXTRA_MESSAGE = "com.example.colma.testapp.MESSAGE";
    public final static String LOCATION = "com.example.colma.testapp.LOCATION";
    public final static String CITY = "com.example.colma.testapp.CITY";
    EditText editTextMessage;
    List<Message> messageList;
    ArrayList<Message> distMessageList;
    Loc location;

    String city;

    static DatabaseReference databaseMessages;
    ListView listViewMessages;

    String CHANNEL_ID = "notification_channel";
    MyReceiver myReceiver;
    public BackgroundService gpsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageList = new ArrayList<>();
        distMessageList = new ArrayList<>();

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Database and location
        listViewMessages = (ListView) findViewById(R.id.listViewMessages);

        Location loc = getCurrLoc();
        location = new Loc(loc.getLatitude(), loc.getLongitude());
        city = getCity(location);

        // Background service and receiver to communicate with service
        final Intent intent = new Intent(this.getApplication(), BackgroundService.class);
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackgroundService.MY_ACTION);
        registerReceiver(myReceiver, intentFilter);

        this.getApplication().startService(intent);
        this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        getPermission();
        databaseMessages = FirebaseDatabase.getInstance().getReference("notes/" + city);
        final Context mContext = this;
        databaseMessages.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                messageList.clear();
                for(DataSnapshot messageSnapshot : dataSnapshot.getChildren())
                {
                    Message message = messageSnapshot.getValue(Message.class);

                    messageList.add(message);
                }

                distMessageList = getNotesInRadius(messageList, 50);
                MessageList adapter = new MessageList(MainActivity.this, distMessageList);
                listViewMessages.setAdapter(new MessageListAdapter(distMessageList, mContext) );
//                listViewMessages.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private ArrayList<Message> getNotesInRadius(List<Message> messageList, float radius) {
        Message currMessage;
        float distanceInMeters;

        Location currLocation = getCurrLoc();
        Location noteLocation = new Location("");//provider name is unnecessary
        ArrayList<Message> notesInRadius = new ArrayList<Message>();

        for(int i = 0; i < messageList.size(); i ++)
        {
            currMessage = messageList.get(i);
            noteLocation.setLatitude(currMessage.location.getLatitude());
            noteLocation.setLongitude(currMessage.location.getLongitude());

            distanceInMeters =  noteLocation.distanceTo(currLocation);

            if(distanceInMeters < radius)
                notesInRadius.add(currMessage);
        }

        return notesInRadius;
    }

    public void makeNote(View view)
    {
        Intent intent = new Intent(this, MakeNoteActivity.class);
        intent.putExtra(LOCATION, location);
        intent.putExtra(CITY, city);
        Log.i(TAG, "makeNote: " + city);
        startActivity(intent);
    }

    private void getPermission()
    {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse response) {
                    gpsService.startTracking();
                }

                @Override
                public void onPermissionDenied(PermissionDeniedResponse response) {
                    if (response.isPermanentlyDenied()) {
                        openSettings();
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                    token.continuePermissionRequest();
                }
            }).check();
    }

    private Location getCurrLoc()
    {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        /* Loop over the array backwards, and if you get an accurate location, then break out the loop*/
        Location l = null;
        try
        {
            for (int i = providers.size() - 1; i >= 0; i --)
            {
                l = lm.getLastKnownLocation(providers.get(i));
                if (l != null)
                {
                    break;
                }
            }
        } catch (java.lang.SecurityException ex) {
            // Log.i(TAG, "fail to request location update, ignore", ex);
        }
//        Log.i(TAG, "Location: "+l);


        return l;
    }

    private String getCity(Loc l)
    {
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

    private void openSettings() {
        Intent intent = new Intent();
        intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
        intent.setData(uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        unregisterReceiver(myReceiver);
        super.onStop();
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            Bundle bund = arg1.getExtras();
            Location loc = (Location)bund.get("LOCATION");

        }

    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("BackgroundService")) {
                gpsService = ((BackgroundService.LocationServiceBinder) service).getService();
                getPermission();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("BackgroundService")) {
                gpsService = null;
            }
        }
    };
}
