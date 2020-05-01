package com.example.blog.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import com.example.blog.Notifications.Token;
import com.example.blog.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import java.util.Objects;

public class DashboardActivity extends AppCompatActivity {


    ActionBar actionBar;

    //Firebase authentication
    FirebaseAuth firebaseAuth;

    String myUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //Actionbar
        actionBar = getSupportActionBar();

        //init firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //Bottom Navigation
        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        //Home fragment transaction, Default on start
        actionBar.setTitle("Home"); //change action bar title
        HomeFragment homeFragment = new HomeFragment();
        FragmentTransaction homeFragmentTransaction = getSupportFragmentManager().beginTransaction();
        homeFragmentTransaction.replace(R.id.content, homeFragment, "Home");
        homeFragmentTransaction.commit();


        checkUserStatus();


    }

    private void updateToken(String token) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        reference.child(myUID).setValue(mToken);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

            //Handle items clicks
            switch (item.getItemId()) {
                case R.id.nav_home:

                    //Home fragment transaction
                    actionBar.setTitle("Home"); //change action bar title
                    HomeFragment homeFragment = new HomeFragment();
                    FragmentTransaction homeFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    homeFragmentTransaction.replace(R.id.content, homeFragment, "HOME");
                    homeFragmentTransaction.commit();
                    return true;

                case R.id.nav_profile:
                    //Profile fragment transaction
                    actionBar.setTitle("Profile"); //change action bar title
                    ProfileFragment profileFragment = new ProfileFragment();
                    FragmentTransaction profileFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    profileFragmentTransaction.replace(R.id.content, profileFragment, "PROFILE");
                    profileFragmentTransaction.commit();
                    return true;

                case R.id.nav_users:
                    //Users fragment transaction
                    actionBar.setTitle("Users"); //change action bar title
                    UsersFragment usersFragment = new UsersFragment();
                    FragmentTransaction usersFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    usersFragmentTransaction.replace(R.id.content, usersFragment, "USERS");
                    usersFragmentTransaction.commit();
                    return true;

                case R.id.nav_chats:
                    //Chats fragment transaction
                    actionBar.setTitle("Chats"); //change action bar title
                    ChatListFragment chatListFragment = new ChatListFragment();
                    FragmentTransaction chatListFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    chatListFragmentTransaction.replace(R.id.content, chatListFragment, "CHATS");
                    chatListFragmentTransaction.commit();
                    return true;
                case R.id.nav_notifications:
                    //Notifications fragment transaction
                    actionBar.setTitle("Notifications"); //change action bar title
                    NotificationsFragment notificationsFragment = new NotificationsFragment();
                    FragmentTransaction notificationsFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    notificationsFragmentTransaction.replace(R.id.content, notificationsFragment, "NOTIFICATIONS");
                    notificationsFragmentTransaction.commit();
                    return true;
            }
            return false;
        }
    };

    private void checkUserStatus() {

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            //User is signed in so stay here and show email of the user
            myUID = firebaseUser.getUid();
            //save UID of currently signed in user in shared preferences
            SharedPreferences sharedPreferences = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("Current_USERID", myUID);
            editor.apply();

            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w("Error", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = Objects.requireNonNull(task.getResult()).getToken();
                        // Log
                        Log.d("Token", token);
                        //update token
                        updateToken(token);
                    });


        } else {
            //User not signed in, so go to mainActivity to sign
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();

        }

    }

}
