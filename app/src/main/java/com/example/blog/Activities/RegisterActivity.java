package com.example.blog.Activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blog.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //views
    EditText mNameEt, mEmailEt, mPasswordEt;
    Button mRegisterBtn;
    TextView mHaveAccountTV;

    //ProgressDialog to display while registering user
    ProgressDialog progressDialog;

    //Declare an instance of FirebaseAuth
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //Actionbar and it's title
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Create Account");
        //Enable back button
        //use this function or use parentActivityName in the XML
        actionBar.setDisplayHomeAsUpEnabled(true); //to show the back arrow but should write the onSupportNavigateUp method to work not only as a design
        //actionBar.setHomeAsUpIndicator(R.drawable.common_google_signin_btn_icon_light_normal_background);

       /* //This method just controls whether to show the Activity icon/logo or not
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setIcon(R.drawable.common_google_signin_btn_icon_light_normal_background);*/


        //init views
        mNameEt = findViewById(R.id.nameET);
        mEmailEt = findViewById(R.id.emailET);
        mPasswordEt = findViewById(R.id.passwordET);
        mRegisterBtn = findViewById(R.id.registerBtn);
        progressDialog = new ProgressDialog(this);
        mHaveAccountTV = findViewById(R.id.have_account_TV);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        //handle register button click
        mRegisterBtn.setOnClickListener(view -> {

            //Input name, email and password
            String name = mNameEt.getText().toString();
            String email = mEmailEt.getText().toString();
            String password = mPasswordEt.getText().toString().trim();

            //Validate
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                //Show error and focus to email edtitext
                mEmailEt.setError("Invalid Email");
                //mEmailEt.setFocusable(true);  ???
            } else if (password.length() < 6) {
                //Show error and focus to password edtitext
                mPasswordEt.setError("Password length must be at least 6 characters");
            } else {
                registerUser(name, email, password); //Register the user
            }

        });

        //handle have account textview click listener
        mHaveAccountTV.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });


    }

    @Override
    public boolean onSupportNavigateUp() {

        //this function call finish() for that activity
        onBackPressed();  //Go previous activity
        return super.onSupportNavigateUp();

    }

    private void registerUser(String name, String email, String password) {

        //Email and password pattern are valid, show progress dialog and start registering the user
        progressDialog.setTitle("Registering User...");
        progressDialog.show();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registering success, dismiss progress dialog and start profile activity
                        progressDialog.dismiss();

                        FirebaseUser user = mAuth.getCurrentUser();
                        //Get user email and id from Authentication
                        assert user != null;
                        String email1 = user.getEmail();
                        String uid = user.getUid();

                        //When user is registered store user info in firebase realtime database using hashMap
                        HashMap<String, Object> hashMap = new HashMap<>();
                        //put info into the hashMap
                        hashMap.put("email", email1);
                        hashMap.put("uid", uid);
                        hashMap.put("name", name);
                        hashMap.put("onlineStatus", "online");
                        hashMap.put("typingTo", "noOne");
                        hashMap.put("phone", "");
                        hashMap.put("profile", "");
                        hashMap.put("cover", "");

                        //Firebase realtime database instance
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        //Path to store user data called "Users"
                        DatabaseReference reference = database.getReference("Users");
                        //Put the data within hashMap into the database
                        reference.child(uid).setValue(hashMap);


                        Toast.makeText(RegisterActivity.this, "Registered...\n" + user.getEmail(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                        finish();

                    } else {
                        // If registering fails, dismiss progress dialog and display a message to the user.
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }

                }).addOnFailureListener(e -> {
            //error, dismiss the progress dialog, get and show the the error message
            progressDialog.dismiss();
            Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        });
    }


}
