package com.example.blog.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.example.blog.Adapters.AdapterUsers;
import com.example.blog.Models.ModelUsers;
import com.example.blog.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostLikedByActivity extends AppCompatActivity {

    String postId;
    private RecyclerView likedUsersRecyclerView;

    List<ModelUsers> usersList;
    AdapterUsers adapterUsers;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_liked_by);

        //Actionbar and it's title
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("People who reacted");
        //Enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        actionBar.setSubtitle(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getEmail());


        //init recycler view
        likedUsersRecyclerView = findViewById(R.id.likedUsersRecyclerView);
        //set it's properties
        likedUsersRecyclerView.setHasFixedSize(true);
        likedUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //get the post id
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        usersList = new ArrayList<>();
        //get the list of UIDs of users who liked the post
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Likes");
        reference.child(postId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    usersList.clear();
                    for (DataSnapshot Snapshot : dataSnapshot.getChildren()) {
                        String hisUid = ""+Snapshot.getRef().getKey();

                        //get user info from each id
                        getUsers(hisUid);
                    }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getUsers(String hisUid) {
        //get info of each user using user id
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot Snapshot : dataSnapshot.getChildren()) {
                            //get data
                            ModelUsers modelUsers = Snapshot.getValue(ModelUsers.class);
                            //add to list
                            usersList.add(modelUsers);
                        }
                        //adapter
                        adapterUsers = new AdapterUsers(PostLikedByActivity.this, usersList,"peopleLiked");

                        //set to recycler view
                        likedUsersRecyclerView.setAdapter(adapterUsers);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();  //Go to previous activity
        return super.onSupportNavigateUp();
    }
}
