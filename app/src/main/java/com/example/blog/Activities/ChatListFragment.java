package com.example.blog.Activities;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.example.blog.Adapters.AdapterChatlist;
import com.example.blog.Models.ModelChat;
import com.example.blog.Models.ModelChatlist;
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


public class ChatListFragment extends Fragment {

    //Firebase authentication
    private FirebaseAuth firebaseAuth;
    private DatabaseReference reference;
    private FirebaseUser currentUser;

    private RecyclerView recyclerView;
    private List<ModelChatlist> modelChatlistList;
    private AdapterChatlist adapterChatlist;
    private List<ModelUsers> usersList;

    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        //init firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //init recyclerview
        recyclerView = view.findViewById(R.id.chatlistRecyclerView);

        modelChatlistList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                modelChatlistList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelChatlist modelChatlist = snapshot.getValue(ModelChatlist.class);
                    modelChatlistList.add(modelChatlist);
                }
                loadChats();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return view;
    }

    private void loadChats() {
        usersList = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelUsers user = snapshot.getValue(ModelUsers.class);
                    for (ModelChatlist modelChatlist : modelChatlistList) {
                        assert user != null;
                        if (user.getUid() != null && user.getUid().equals(modelChatlist.getId())) {
                            usersList.add(user);
                            break;
                        }

                    }

                    //Adapter
                    adapterChatlist = new AdapterChatlist(getContext(), usersList);
                    //set adapter
                    recyclerView.setAdapter(adapterChatlist);
                    //set last message
                    for (int i = 0; i < usersList.size(); i++) {
                        lastMessage(usersList.get(i).getUid());
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void lastMessage(final String userId) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String theLastMessage = "default";
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelChat chat = snapshot.getValue(ModelChat.class);
                    assert chat != null;
                    String sender = chat.getSender();
                    String receiver = chat.getReceiver();

                    /*if (chat == null) {
                        continue;
                    }
                    String sender = chat.getSender();
                    String receiver = chat.getReceiver();

                    if (sender == null || receiver == null) {
                        continue;
                    }*/
                    if (receiver.equals(currentUser.getUid())
                            && sender.equals(userId) || receiver.equals(userId) && sender.equals(currentUser.getUid())) {

                        if (chat.getType().equals("image")) {
                            if (chat.getMessage().equals("This message was deleted")) {
                                theLastMessage = "This message was deleted";
                            } else {
                                //instead of display an url of image show "Sent a photo" message
                                theLastMessage = "Sent a photo";
                            }
                        } else {
                            theLastMessage = chat.getMessage();
                        }
                    }

                }

                adapterChatlist.setLastMessage(userId, theLastMessage);
                adapterChatlist.notifyDataSetChanged();

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
                    loadChats();
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
                    loadChats();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void searchUsers(final String query) {
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
                    for (ModelChatlist modelChatlist : modelChatlistList) {
                        assert modelUsers != null;
                        if (modelUsers.getUid().equals(modelChatlist.getId())) {
                            //contains is used to check if a TextView contains a certain string
                            if (modelUsers.getName().toLowerCase().contains(query.toLowerCase())) {
                                usersList.add(modelUsers);

                            }
                        }

                    }


                    //adapter
                    adapterChatlist = new AdapterChatlist(getContext(), usersList);
                    //refresh adapter
                    adapterChatlist.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterChatlist);
                    //set last message
                    for (int i = 0; i < usersList.size(); i++) {
                        lastMessage(usersList.get(i).getUid());
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
            //User is signed in so stay here and show email of the user
        } else {
            //User not signed in, so go to mainActivity to sign
            startActivity(new Intent(getActivity(), MainActivity.class));
            Objects.requireNonNull(getActivity()).finish();
        }

    }

}
