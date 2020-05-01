package com.example.blog.Activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blog.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    //Sign in with google
    /*private static final int RC_SIGN_IN =100 ;
    GoogleSignInClient mGoogleSignInClient;*/

    //views
    EditText mEmailEt, mPasswordEt;
    Button mLoginBtn;
    TextView mNotHaveAccountTV, mRecoverPasswordTV;
    //SignInButton mGoogleLoginBtn;

    //ProgressDialog to display while registering user
    ProgressDialog progressDialog;

    //Declare an instance of FirebaseAuth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Actionbar and it's title
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Login");
        //Enable back button
        //use this function or use parentActivityName in the XML
        actionBar.setDisplayHomeAsUpEnabled(true); //to show the back arrow but should write the onSupportNavigateUp method to work not only as a design


        //init views
        mEmailEt = findViewById(R.id.emailET);
        mPasswordEt = findViewById(R.id.passwordET);
        mLoginBtn = findViewById(R.id.loginBtn);
        progressDialog = new ProgressDialog(this);
        mNotHaveAccountTV = findViewById(R.id.not_have_account_TV);
        mRecoverPasswordTV = findViewById(R.id.recover_pass_tv);
        //mGoogleLoginBtn = findViewById(R.id.google_login_btn);

        // Configure Google Sign In
        /*GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient= GoogleSignIn.getClient(this,gso);*/

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();


        //handle login button click
        mLoginBtn.setOnClickListener(view -> {

            //Input email and password
            String email = mEmailEt.getText().toString();
            String password = mPasswordEt.getText().toString();

            //Validate
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                //Show error and focus to email edtitext
                mEmailEt.setError("Invalid Email");
            }

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getApplicationContext(), "All Fields Are Required!", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password); //Signing the user
            }

        });

        //handle not have account textview click listener
        mNotHaveAccountTV.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
        //handle recover password textview click listener
        mRecoverPasswordTV.setOnClickListener(view -> showRecoverPasswordDialog());

        //handle google login button click listener
       /* mGoogleLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Begin google login process
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            }
        });*/
    }

    @Override
    public boolean onSupportNavigateUp() {

        //this function call finish() for that activity
        onBackPressed();  //Go to previous activity
        return super.onSupportNavigateUp();

    }

    private void loginUser(String email, String password) {
        //Email and password pattern are valid, show progress dialog and start signing in the user
        progressDialog.setTitle("Signing in...");
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, dismiss progress dialog and start profile activity
                        progressDialog.dismiss();
                        FirebaseUser user = mAuth.getCurrentUser();
                        assert user != null;
                        Toast.makeText(LoginActivity.this, "Signed in...\n" + user.getEmail(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        // If sign in fails, dismiss progress dialog and display a message to the user.
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }

                }).addOnFailureListener(e -> {
            //error, dismiss the progress dialog, get and show the the error message
            progressDialog.dismiss();
            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        });
    }

    private void showRecoverPasswordDialog() {

        //AlertDialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Password");

        //Set layout linear layout
        LinearLayout linearLayout = new LinearLayout(this);

        //Views to be set in dialog
        final EditText mEmailEt = new EditText(this);
        mEmailEt.setHint(getString(R.string.email));
        mEmailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        /*sets the width of a TextView or (EditText) to fit a text of n 'M' letters
        regardless of the actual text extension and text size*/
        mEmailEt.setMinEms(16);
        linearLayout.addView(mEmailEt);
        linearLayout.setPadding(10, 10, 10, 10);

        builder.setView(linearLayout);

        //Button recover
        builder.setPositiveButton("Recover", (dialogInterface, i) -> {

            //input email
            String email = mEmailEt.getText().toString().trim();
            beginRecovery(email);


        });
        //Button cancel
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {

            //Dismiss dialog
            dialogInterface.dismiss();
        });

        //show dialog
        builder.create().show();

    }

    private void beginRecovery(String email) {
        //show progress dialog
        progressDialog.setTitle("Sending Email...");
        progressDialog.show();
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Email sent", Toast.LENGTH_SHORT).show();
            } else {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Failed...", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            //error, dismiss the progress dialog, get and show the the error message
            progressDialog.dismiss();
            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    //Sign in with google

    /*@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        }
    }*/

    /* private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            //if user is signing in first time then get and show user info from google account
                            if(task.getResult().getAdditionalUserInfo().isNewUser()){
                                FirebaseUser user = mAuth.getCurrentUser();
                                //Get user email and id from Authentication
                                assert user != null;
                                String email=user.getEmail();
                                String uid=user.getUid();

                                //When user is registered store user info in firebase realtime database using hashMap
                                HashMap<String, Object> hashMap=new HashMap<>();
                                //put info into the hashMap
                                hashMap.put("email",email);
                                hashMap.put("uid",uid);
                                hashMap.put("name","");
                                hashMap.put("onlineStatus", "online");
                                hashMap.put("typingTo", "noOne");
                                hashMap.put("phone","");
                                hashMap.put("profile","");
                                hashMap.put("cover","");

                                //Firebase realtime database instance
                                FirebaseDatabase database=FirebaseDatabase.getInstance();
                                //Path to store user data called "Users"
                                DatabaseReference reference=database.getReference("Users");
                                //Put the data within hashMap into the database
                                reference.child(uid).setValue(hashMap);
                            }

                            //show user email in toast
                            Toast.makeText(getApplicationContext(), "" + user.getEmail(), Toast.LENGTH_SHORT).show();
                            // Sign in success, go to profile activity
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                        }
                        else{
                            // If sign in fails, display a message to the user.
                            Toast.makeText(getApplicationContext(), "Login Failed...", Toast.LENGTH_SHORT).show();
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //get and show error message
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }*/
}
