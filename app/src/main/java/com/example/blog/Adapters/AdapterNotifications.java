package com.example.blog.Adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blog.Models.ModelNotifications;
import com.example.blog.Activities.PostDetailsActivity;
import com.example.blog.R;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdapterNotifications extends RecyclerView.Adapter<AdapterNotifications.MyHolder> {

    private Context context;
    private List<ModelNotifications> notificationsList;
    private FirebaseAuth firebaseAuth;

    public AdapterNotifications(Context context, List<ModelNotifications> notificationsList) {
        this.context = context;
        this.notificationsList = notificationsList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_notifications, parent, false);
        return new MyHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, int position) {


        //get and set data

        //get data
        final ModelNotifications modelNotifications = notificationsList.get(position);
        String notifications = modelNotifications.getNotifications();
        final String timestamp = modelNotifications.getTimeStamp();
        String senderUid = modelNotifications.getsUid();
        final String pId = modelNotifications.getpId();

        //convert time stamp to dd/mm/yyyy hh:mm: AM/PM
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(Long.parseLong(timestamp));  //should be Long
        //MM should be capital to be different from mm for minute
        String nTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        //we will get the name, email and image of the user of notifications from his uid
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(senderUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot Snapshot : dataSnapshot.getChildren()) {
                            String name = "" + Snapshot.child("name").getValue();
                            String image = "" + Snapshot.child("profile").getValue();

                            holder.uNameTv.setText(name);
                            //set user image
                            try {
                                //image received, set it to user image in the notifications screen
                                Picasso.get().load(image).placeholder(R.drawable.ic_default_users).into(holder.notificationsAvatarIV);

                            } catch (Exception ex) {
                                //there exception getting picture,set default picture
                                Picasso.get().load(R.drawable.ic_default_users).into(holder.notificationsAvatarIV);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        //set data
        holder.notificationsTv.setText(notifications);
        holder.nTimeTv.setText(nTime);

        //handle notifications click listener to open post details class
        holder.itemView.setOnClickListener(view -> {
            //view post details is clicked
            //start post detail activity
            Intent intent = new Intent(context, PostDetailsActivity.class);
            intent.putExtra("postId", pId);//we will get detail of post using this id,it is id of the clicked post
            context.startActivity(intent);
        });

        //handle notifications long click listener to delete this notification
        holder.itemView.setOnLongClickListener(view -> {
            //show delete notification confirm dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete");
            builder.setMessage("Are you sure to delete this notification?");
            //Delete button
            builder.setPositiveButton("Delete", (dialogInterface, i) -> {
                //delete comment
                deleteNotification(timestamp);
            });

            //cancel Delete button
            builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
                //dismiss dialog
                dialogInterface.dismiss();
            });

            //create and show the dialog
            builder.create().show();
            return false;
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void deleteNotification(String timestamp) {

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid());
        reference.child("Notifications").child(timestamp)
                .removeValue()
                .addOnSuccessListener(aVoid -> {

                    //deleted successfully
                    Toast.makeText(context, "Notification deleted successfully...", Toast.LENGTH_SHORT).show();

                }).addOnFailureListener(e -> {

            //failed
            Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });


    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        //Views from row_posts.XML
        ImageView notificationsAvatarIV;
        TextView uNameTv, nTimeTv, notificationsTv;

        MyHolder(@NonNull View itemView) {

            super(itemView);

            //init views
            notificationsAvatarIV = itemView.findViewById(R.id.notificationAvatarIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            notificationsTv = itemView.findViewById(R.id.notificationsTv);
            nTimeTv = itemView.findViewById(R.id.timeTv);


        }
    }

}
