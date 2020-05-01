package com.example.blog.Activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.example.blog.R;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends AppCompatActivity {

    SwitchCompat switchCompat;

    //use shared preferences to save the value of the switch
    SharedPreferences sharedPreferences;
    // to edit the value of shared preferences
    SharedPreferences.Editor editor;

    //constant for topic, assign any value but use the same for this kind of notifications
    private static final String TOPIC_POST_NOTIFICATION = "POST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Actionbar and it's title
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Settings");
        //Enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);

        //init views
        switchCompat = findViewById(R.id.postSwitch);

        //init sharedPreferences
        sharedPreferences = getSharedPreferences("NOTIFICATION_SP", MODE_PRIVATE);
        boolean isPostNotificationEnabled = sharedPreferences.getBoolean("" + TOPIC_POST_NOTIFICATION, false);

        //if enabled check switch, otherwise uncheck switch. by default unchecked/false
        if (isPostNotificationEnabled) {
            switchCompat.setChecked(true);
        } else {
            switchCompat.setChecked(false);
        }


        //implement switch change listener
        switchCompat.setOnCheckedChangeListener((compoundButton, isChecked) -> {

            //edit switch state
            editor = sharedPreferences.edit();
            editor.putBoolean("" + TOPIC_POST_NOTIFICATION, isChecked);
            editor.apply();

            if (isChecked) {

                subscribePostNotification();  //call to subscribe
            } else {
                unsubscribePostNotification();  //call to unsubscribe
            }
        });
    }

    private void unsubscribePostNotification() {
        //unsubscribe to a topic (POST) to disable it's notification
        FirebaseMessaging.getInstance().unsubscribeFromTopic("" + TOPIC_POST_NOTIFICATION)
                .addOnCompleteListener(task -> {
                    String message = "You won't receive post notification";
                    if (!task.isSuccessful()) {
                        message = "Un-subscription failed!";
                    }
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                });
    }

    private void subscribePostNotification() {

        //subscribe to a topic (POST) to enable it's notification
        FirebaseMessaging.getInstance().subscribeToTopic("" + TOPIC_POST_NOTIFICATION)
                .addOnCompleteListener(task -> {
                    String message = "You will receive post notification";
                    if (!task.isSuccessful()) {
                        message = "Subscription failed!";
                    }
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                });

    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();  //Go to previous activity
        return super.onSupportNavigateUp();
    }
}
