package com.example.colma.testapp;

import android.content.Intent;
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
    Loc location;
    String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_note);
        editTextMessage = (EditText) findViewById(R.id.editTextMessage);

        Intent intent = getIntent();
        location = (Loc) intent.getParcelableExtra(MainActivity.LOCATION);
        city = intent.getStringExtra(MainActivity.CITY);

    }

    public void addMessage(View view) {
        Log.i(TAG, "addMessage: " + city);
        DatabaseReference databaseMessages = FirebaseDatabase.getInstance().getReference("notes/" + city);
        String message = editTextMessage.getText().toString().trim();
//        String message = "Testing";
        if (!TextUtils.isEmpty(message)) {
            String id = databaseMessages.push().getKey();


            Message userMessage = new Message(id, message, location);
            databaseMessages.child(id).setValue(userMessage);

            Toast.makeText(this, "Message Added", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
