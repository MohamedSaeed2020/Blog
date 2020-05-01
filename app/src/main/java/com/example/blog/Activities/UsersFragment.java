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

import com.example.blog.Adapters.AdapterUsers;
import com.example.blog.Models.ModelUsers;
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


public class UsersFragment extends Fragment {

    //RecyclerView
    private RecyclerView recyclerView;
    //Adapter
    private AdapterUsers adapterUsers;
    //user Model
    private List<ModelUsers> usersList;

    //Firebase authentication
    private FirebaseAuth firebaseAuth;


    public UsersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        //init firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        //init RecyclerView
        recyclerView = view.findViewById(R.id.users_RecyclerView);

        //set it's properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //init user list
        usersList = new ArrayList<>();

        //get All users 
        getAllUsers();

        return view;
    }

    private void getAllUsers() {

        //get current user
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        //get path of database named "Users" containing user info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        //get all data from the path
        reference.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();  /*very important ..if i am online and there is at that time foe example 3 other users in the database
                 and suddenly one user register from another device. and without usersList.clear()
                 the number of users will be duplicated */
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelUsers modelUsers = snapshot.getValue(ModelUsers.class);

                    //get all users except currently signed in user
                    assert modelUsers != null;
                    assert firebaseUser != null;
                    if (!modelUsers.getUid().equals(firebaseUser.getUid())) {
                        usersList.add(modelUsers);
                    }

                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(), usersList,"users");
                    //refresh adapter
                    adapterUsers.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void searchUsers(final String query) {
        //get current user
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        //get path of database named "Users" containing user info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        //get all data from the path
        reference.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelUsers modelUsers = snapshot.getValue(ModelUsers.class);

                    /*Conditions to fulfil search
                     * 1) User not current user
                     * 2) The user name or email contains text entered in search view (case insensitive)*/

                    //get all searched users except currently signed in user
                    assert modelUsers != null;
                    assert firebaseUser != null;
                    if (!modelUsers.getUid().equals(firebaseUser.getUid())) {

                        //contains is used to check if a TextView contains a certain string
                        if (modelUsers.getName().toLowerCase().contains(query.toLowerCase()) ||
                                modelUsers.getEmail().toLowerCase().contains(query.toLowerCase())) {
                            usersList.add(modelUsers);

                        }
                    }

                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(), usersList,"users");
                    //refresh adapter
                    adapterUsers.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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


        //hide add post icon as we don't need it here
        menu.findItem(R.id.action_add_post).setVisible(false);

        //Search view
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
                    searchUsers(s);
                } else {
                    //search text is empty, get all users
                    getAllUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user press any single letter
                //if search query is not empty then start search
                if (!TextUtils.isEmpty(s.trim())) {
                    //search text contain text, search it
                    searchUsers(s);
                } else {
                    //search text is empty, get all users
                    getAllUsers();
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


}
