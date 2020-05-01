package com.example.blog.Activities;


import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.blog.Adapters.AdapterNotifications;
import com.example.blog.Models.ModelNotifications;
import com.example.blog.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;


public class NotificationsFragment extends Fragment {

    //recycler view
    private RecyclerView notificationsRecyclerView;
    private FirebaseAuth firebaseAuth;
    private ArrayList<ModelNotifications> modelNotificationsArrayList;
    private AdapterNotifications adapterNotifications;

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        //init recycler view
        notificationsRecyclerView = view.findViewById(R.id.notificationsRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        //show newest notifications first,for this load from last
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        notificationsRecyclerView.setLayoutManager(linearLayoutManager);
        firebaseAuth = FirebaseAuth.getInstance();

        getAllNotifications();

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void getAllNotifications() {

        modelNotificationsArrayList = new ArrayList<>();
        //Database > CurrentUser > Notifications > all notifications list
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        //(Objects.requireNonNull(firebaseAuth.getUid()))

        //OR

        reference.child(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid()).child("Notifications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        modelNotificationsArrayList.clear();
                        for (DataSnapshot Snapshot : dataSnapshot.getChildren()) {
                            //get data
                            ModelNotifications modelNotifications = Snapshot.getValue(ModelNotifications.class);  //this line of code required the empty class on ModelNotifications class
                            //add to list
                            modelNotificationsArrayList.add(modelNotifications);
                        }

                        //adapter
                        adapterNotifications = new AdapterNotifications(getActivity(), modelNotificationsArrayList);
                        //set to recycler view
                        notificationsRecyclerView.setAdapter(adapterNotifications);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

}
