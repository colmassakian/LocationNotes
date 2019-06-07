package com.example.colma.testapp;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MakeNoteActivity extends AppCompatActivity {

    private static final String TAG = "MakeNoteActivity";
    EditText editTextMessage;
    Loc loc;
    Location location;
    String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_note);
        editTextMessage = findViewById(R.id.editTextMessage);

        Intent intent = getIntent();
        location = intent.getParcelableExtra(MainActivity.LOCATION);
        if(location != null)
            loc = new Loc(location.getLatitude(), location.getLongitude());
        city = intent.getStringExtra(MainActivity.CITY);

    }

    public void addMessage(View view) {
        Log.i(TAG, "addMessage: " + city);
        DatabaseReference databaseMessages = FirebaseDatabase.getInstance().getReference("notes/" + city);
        String message = editTextMessage.getText().toString().trim();
//        String message = "Testing";
        if (!TextUtils.isEmpty(message) && location != null) {
            String id = databaseMessages.push().getKey();


            Message userMessage = new Message(id, message, loc, 0, 0);
            databaseMessages.child(id).setValue(userMessage);

            Toast.makeText(this, "Message Added", Toast.LENGTH_LONG).show();
        } else if(TextUtils.isEmpty(message)){
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_LONG).show();
        } else if (location == null){
            Toast.makeText(this, "Cannot get current location, try again later", Toast.LENGTH_LONG).show();
        }

        finish();
    }
}
