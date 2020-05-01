package com.example.blog.Activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import com.example.blog.R;

public class MainActivity extends AppCompatActivity {

    //views
    Button mRegisterBtn, mLoginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init views
        mRegisterBtn = findViewById(R.id.register_btn);
        mLoginBtn = findViewById(R.id.login_btn);

        //handle register button click
        mRegisterBtn.setOnClickListener(view -> {
            //Start RegisterActivity
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));

        });

        //handle login button click
        mLoginBtn.setOnClickListener(view -> {
            //Start LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));


        });
    }
}
