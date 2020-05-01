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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blog.Adapters.AdapterPosts;
import com.example.blog.Models.ModelPost;
import com.example.blog.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class UsersProfileActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    DatabaseReference reference;
    RecyclerView usersPostsRecyclerView;
    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    //Views from XML
    ImageView profileIv, coverIv;
    TextView nameTv, emailTv, phoneTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_profile);

        //Action bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Profile");
        actionBar.setDisplayHomeAsUpEnabled(true);

        //init views
        profileIv = findViewById(R.id.profileIv);
        coverIv = findViewById(R.id.coverIv);
        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        phoneTv = findViewById(R.id.phoneTv);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Users");
        //recyclerView and it's properties
        usersPostsRecyclerView = findViewById(R.id.usersPostsRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        //show newest posts first,for this load from last
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        /* use this setting to improve performance if you know that changes
         in content do not change the layout size of the RecyclerView*/
        usersPostsRecyclerView.setHasFixedSize(true);
        usersPostsRecyclerView.setLayoutManager(linearLayoutManager);

        //init post list
        postList = new ArrayList<>();


        //get uid of clicked user to retrieve his posts
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");

        Query query = reference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //check until required data is get
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    //get data
                    String name = "" + snapshot.child("name").getValue();
                    String email = "" + snapshot.child("email").getValue();
                    String phone = "" + snapshot.child("phone").getValue();
                    String profile = "" + snapshot.child("profile").getValue();
                    String cover = "" + snapshot.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    //profile image
                    try {
                        //if image is received, than set
                        Picasso.get().load(profile).placeholder(R.drawable.ic_default_image_white).into(profileIv);
                    } catch (Exception ex) {
                        //if there is any exception while getting image, then set default
                        Picasso.get().load(R.drawable.ic_default_image_white).into(profileIv);
                    }
                    //cover image
                    try {
                        //if image is received, than set
                        Picasso.get().load(cover).into(coverIv);
                    } catch (Exception ex) {
                        //if there is any exception while getting image, then set default
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        loadHisPosts();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();  //Go to previous activity
        return super.onSupportNavigateUp();
    }


    private void loadHisPosts() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

        Query query = reference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                postList.clear();
                //check until required data is get
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelPost modelPost = snapshot.getValue(ModelPost.class);

                    //get my posts
                    postList.add(modelPost);
                    //adapter
                    adapterPosts = new AdapterPosts(UsersProfileActivity.this, postList);
                    //refresh adapter
                    adapterPosts.notifyDataSetChanged();
                    //set adapter to recycler view
                    usersPostsRecyclerView.setAdapter(adapterPosts);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //in case of error
                Toast.makeText(getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void searchHisPost(final String searchQuery) {

        //get path of database named "Posts" that containing all posts info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //get all data from the path
        Query query = reference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                postList.clear();
                //check until required data is get
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelPost modelPost = snapshot.getValue(ModelPost.class);
                    assert modelPost != null;
                    if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                        //get all posts
                        postList.add(modelPost);
                    }
                    //adapter
                    adapterPosts = new AdapterPosts(UsersProfileActivity.this, postList);
                    //refresh adapter
                    adapterPosts.notifyDataSetChanged();
                    //set adapter to recycler view
                    usersPostsRecyclerView.setAdapter(adapterPosts);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //in case of error
                Toast.makeText(getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);

        //Search view to search posts by post title or description
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) item.getActionView();
        //SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //search view listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called when user press search button from keyboard
                //if search query is not empty then start search
                if (!TextUtils.isEmpty(s.trim())) {
                    //search text contain text, search it
                    searchHisPost(s);
                } else {
                    //search text is empty, get all posts
                    loadHisPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user press any single letter
                //if search query is not empty then start search
                if (!TextUtils.isEmpty(s.trim())) {
                    //search text contain text, search it
                    searchHisPost(s);
                } else {
                    //search text is empty, get all posts
                    loadHisPosts();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    //handle option menu item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout) {

            //signing out and go to main activity
            firebaseAuth.signOut();
            checkUserStatus();
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkUserStatus() {

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            //User is signed in so stay here
        } else {
            //User not signed in, so go to mainActivity to sign
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }


}
