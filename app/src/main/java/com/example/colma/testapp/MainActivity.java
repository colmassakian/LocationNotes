package com.example.colma.testapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public final static String LOCATION = "com.example.colma.testapp.LOCATION";
    public final static String CITY = "com.example.colma.testapp.CITY";
    String userRadius, userThreshold;
    ProgressDialog dialog;

    EditText editTextMessage;
    List<Message> messageList;
    ArrayList<Message> distMessageList;
    Map<String, List<Integer>> voteMap = new HashMap<>();

    Loc location;
    Location backgroundLocation;
    String city;

    ListView listViewMessages;
    MessageListAdapter mainMLA;

    MyReceiver myReceiver;
    public BackgroundService gpsService;

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageList = new ArrayList<>();
        distMessageList = new ArrayList<>();

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Database and location
        listViewMessages = findViewById(R.id.listViewMessages);

//        Location loc = getCurrLoc();
//        if(loc != null)
//        {
//            location = new Loc(loc.getLatitude(), loc.getLongitude());
//            city = getCity(location);
//            Log.i(TAG, "onCreate: City" + city);
//        }

        final Intent intent = new Intent(this.getApplication(), BackgroundService.class);
        myReceiver = new MyReceiver(new Handler());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackgroundService.MY_ACTION);
        registerReceiver(myReceiver, intentFilter);
        Log.i(TAG, "onCreate: reached");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.getApplication().startForegroundService(intent);
            this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            this.getApplication().startService(intent);
            this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        dialog = new ProgressDialog(this);
        dialog.setMessage("Waiting for location");
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        pref = getApplicationContext().getSharedPreferences("UserPrefs", 0);
        editor = pref.edit();

        String radiusString = pref.getString("Radius", "50");
        String thresholdString = pref.getString("Threshold", "-50");

        switch (item.getItemId()) {
            case R.id.action_radius:
                AlertDialog.Builder builderRadius = new AlertDialog.Builder(this);

                final EditText settingRadius = new EditText(this);

                builderRadius.setTitle("Enter the new radius (Default is 50, Current is " + radiusString + ")").setView(settingRadius).setView(settingRadius);

                builderRadius.setPositiveButton("OK",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userRadius = settingRadius.getText().toString();
                        editor.putString("Radius", userRadius);
                        editor.commit();
                        gpsService.updateDatabase(backgroundLocation);
                    }
                });
                builderRadius.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });



                builderRadius.show();
                return true;
            case R.id.action_threshold:
                AlertDialog.Builder builderThreshold = new AlertDialog.Builder(this);

                final EditText settingThreshold = new EditText(this);

                builderThreshold.setTitle("Enter the new vote threshold (Default is -50, Current is " + thresholdString + ")").setView(settingThreshold).setView(settingThreshold);

                builderThreshold.setPositiveButton("OK",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userThreshold = settingThreshold.getText().toString();
                        editor.putString("Threshold", userThreshold);
                        editor.commit();
                        gpsService.updateDatabase(backgroundLocation);
                    }
                });
                builderThreshold.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });



                builderThreshold.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private class MyReceiver extends BroadcastReceiver {

        private final Handler handler; // Handler used to execute code on the UI thread

        public MyReceiver(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void onReceive(final Context arg0, Intent arg1) {
            Bundle bundle = arg1.getExtras();
            if(bundle != null)
            {
                dialog.dismiss();

                distMessageList = bundle.getParcelableArrayList("messagelist");
                voteMap = (HashMap<String, List<Integer>>) bundle.getSerializable("votelist");
                backgroundLocation = bundle.getParcelable("location");
                if (backgroundLocation != null) {
                    city = getCity(new Loc(backgroundLocation.getLatitude(), backgroundLocation.getLongitude()));
                }
                Log.i(TAG, "onReceive: " + backgroundLocation.getLatitude() + ", " + backgroundLocation.getLongitude());
                // Use handler to do UI
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainMLA = new MessageListAdapter(distMessageList, arg0, getApplicationContext(), "notes/" + city, voteMap);
                        listViewMessages.setAdapter(mainMLA);
                    }
                });
//
            }
        }

    }

    public void makeNote(View view)
    {
        Intent intent = new Intent(this, MakeNoteActivity.class);
        intent.putExtra(LOCATION, backgroundLocation);
        intent.putExtra(CITY, city);
//        Log.i(TAG, "makeNote: " + city);
        startActivity(intent);
    }

    private void getPermission()
    {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse response) {
                    gpsService.startTracking(getApplicationContext());
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
        List<String> providers = null;
        if (lm != null) {
            providers = lm.getProviders(true);
        }

        /* Loop over the array backwards, and if you get an accurate location, then break out the loop*/
        Location l = null;
        try
        {
            for (int i = providers.size() - 1; i >= 0; i --)
            {
                if (lm != null) {
                    l = lm.getLastKnownLocation(providers.get(i));
                }
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
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        unregisterReceiver(myReceiver);
        gpsService.stopTracking();
        super.onDestroy();
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
