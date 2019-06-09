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

import static com.example.colma.testapp.MainActivity.LOCATION;

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
        location = intent.getParcelableExtra(LOCATION);
        if(location != null)
            loc = new Loc(location.getLatitude(), location.getLongitude());
        city = intent.getStringExtra(MainActivity.CITY);

    }

    public void addMessage(View view) {
        Log.i(TAG, "addMessage: " + city);
        int maxLength = 100;
        DatabaseReference databaseMessages = FirebaseDatabase.getInstance().getReference("notes/" + city);
        String message = editTextMessage.getText().toString().trim();

        // Make sure message has text that is less than 100 characters and has a valid location
        if (!TextUtils.isEmpty(message) && message.length() <= maxLength && location != null) {
            String id = databaseMessages.push().getKey();


            Message userMessage = new Message(id, message, loc, 0, 0);
            databaseMessages.child(id).setValue(userMessage);

            Toast.makeText(this, "Message Added", Toast.LENGTH_LONG).show();
        } else if(TextUtils.isEmpty(message)){
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_LONG).show();
        } else if(message.length() > maxLength){
            Toast.makeText(this, "Message must be less than 150 characters", Toast.LENGTH_LONG).show();
        } else if (location == null){
            Toast.makeText(this, "Cannot get current location, try again later", Toast.LENGTH_LONG).show();
        }

        finish();
    }
}
