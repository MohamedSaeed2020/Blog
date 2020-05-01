package com.example.blog.Activities;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
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
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class HomeFragment extends Fragment {

    //Firebase authentication
    private FirebaseAuth firebaseAuth;

    private RecyclerView recyclerView;
    private List<ModelPost> postList;
    private AdapterPosts adapterPosts;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        //init firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //recyclerView and it's properties
        recyclerView = view.findViewById(R.id.postsRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        //show newest posts first,for this load from last
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        /* use this setting to improve performance if you know that changes
         in content do not change the layout size of the RecyclerView*/
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        //init post list
        postList = new ArrayList<>();

        loadPosts();


        return view;
    }

    private void loadPosts() {

        //get path of database named "Posts" that containing all posts info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //get all data from the path
        reference.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelPost modelPost = snapshot.getValue(ModelPost.class);

                    //get all posts
                    postList.add(modelPost);
                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //refresh adapter
                    adapterPosts.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterPosts);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                //in case of error
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);  //to show menu options in fragment
        super.onCreate(savedInstanceState);
    }

    //initialize option menu
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        //inflating menu
        inflater.inflate(R.menu.menu_main, menu);

        //We implement it here not on onOptionsItemSelected because we need menu variable on MenuItem
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
                    searchPosts(s);
                } else {
                    //search text is empty, get all posts
                    loadPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user press any single letter
                //if search query is not empty then start search
                if (!TextUtils.isEmpty(s.trim())) {
                    //search text contain text, search it
                    searchPosts(s);
                } else {
                    //search text is empty, get all posts
                    loadPosts();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
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
        } else if (id == R.id.action_add_post) {

            //Go to add post activity
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        } else if (id == R.id.action_settings) {
            //go to setting activity
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkUserStatus() {

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            //User is signed in so stay here
        } else {
            //User not signed in, so go to mainActivity to sign
            startActivity(new Intent(getActivity(), MainActivity.class));
            Objects.requireNonNull(getActivity()).finish();
        }

    }

    private void searchPosts(final String searchQuery) {

        //get path of database named "Posts" that containing all posts info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //get all data from the path
        reference.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelPost modelPost = snapshot.getValue(ModelPost.class);

                    assert modelPost != null;
                    if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                        //get all posts
                        postList.add(modelPost);
                    }

                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //refresh adapter
                    adapterPosts.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterPosts);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                //in case of error
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }


}
