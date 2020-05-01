package com.example.blog.Adapters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blog.Models.ModelChat;
import com.example.blog.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {


    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    private Context context;
    private List<ModelChat> chatList;
    private String imageUrl;
    private ProgressDialog progressDialog;


    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public AdapterChat.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //inflate layouts: row_chat_left.xml for receiver and row_chat_right.xml for sender

        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false);
            return new MyHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false);
            return new MyHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull AdapterChat.MyHolder holder, final int position) {


        //int progressDialog
        progressDialog = new ProgressDialog(context);
        //get data
        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimestamp();
        String type = chatList.get(position).getType();

        //convert time stamp to dd/mm/yyyy hh:mm: AM/PM
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(Long.parseLong(timeStamp));  //should be Long
        //MM should be capital to be different from mm for minute
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa ", calendar).toString();


        //set data
        holder.timeTv.setText(dateTime);
        try {
            Picasso.get().load(imageUrl).placeholder(R.drawable.ic_default_users).into(holder.profileIv);
        } catch (Exception ex) {

            holder.profileIv.setImageResource(R.drawable.ic_default_users);
        }

        if (type.equals("text")) {
            //text message
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);
            holder.messageTv.setText(message);

        } else {
            //image message
            if (message.equals("This message was deleted")) {
                holder.messageTv.setVisibility(View.VISIBLE);
                holder.messageIv.setVisibility(View.GONE);
                holder.messageTv.setText(message);
            } else {
                holder.messageTv.setVisibility(View.GONE);
                holder.messageIv.setVisibility(View.VISIBLE);
                Picasso.get().load(message).placeholder(R.drawable.ic_image_black).into(holder.messageIv);
            }
        }


        //click to show delete dialog
        holder.messageLayout.setOnClickListener(view -> {

            //show delete message confirm dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete");
            builder.setMessage("Are you sure to delete this message?");
            //Delete button
            builder.setPositiveButton("Delete", (dialogInterface, i) -> deleteMessage(position, type, message));

            //cancel Delete button
            builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
                //dismiss dialog
                dialogInterface.dismiss();
            });

            //create and show the dialog
            builder.create().show();

        });

        //set seen/delivered status of message
        if (position == chatList.size() - 1) {
            if (chatList.get(position).isWatched()) {
                holder.isSeenTv.setText("Seen");
            } else {
                holder.isSeenTv.setText("Delivered");
            }
        } else {
            holder.isSeenTv.setVisibility(View.GONE);
        }

    }

    private void deleteMessage(int position, String type, String message) {

        /*Logic:
         * 1) Get timestamp of clicked message
         * 2) Compare the timestamp of the clicked message with all messages in chats
         * 3) Where both values matches delete that message*/
        //progress dialog
        progressDialog.setMessage("Deleting...");
        progressDialog.show();
        String msgTimeStamp = chatList.get(position).getTimestamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    /*if you want to allow sender to delete only his message then compare sender value
                     * with current user's uid, if they match means it's the message of sender that it is trying to delete */
                    final String myUID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                    if (Objects.equals(snapshot.child("sender").getValue(), myUID)) {
                        /*We can do those things:
                         * 1) Remove the message from chats
                         * 2) set the value of message "This message was deleted"
                         */

                        if (message.equals("This message was deleted")) {
                            //1) Remove the message from chats
                            snapshot.getRef().removeValue();
                            progressDialog.dismiss();
                        } else {
                            // 2) set the value of message "This message was deleted" and if the message is an image follow the following steps
                            /*Steps:
                             * 1) Delete image using its url from storage
                             * 2) Setting the message with  the value of message "This message was deleted"*/
                            if (type.equals("image")) {
                                StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(message);
                                picRef.delete().addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    //image deleted, now delete the post from the database
                                    HashMap<String, Object> hashMap = new HashMap<>();
                                    hashMap.put("message", "This message was deleted");
                                    snapshot.getRef().updateChildren(hashMap);
                                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();

                                }).addOnFailureListener(e -> {

                                    //failed, can't go further
                                    progressDialog.dismiss();
                                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();

                                });
                            } else {
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("message", "This message was deleted");
                                snapshot.getRef().updateChildren(hashMap);
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }

                        }

                    } else {
                        Toast.makeText(context, "You can delete only your message!", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        //get currently signed in user
        //Firebase user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        assert firebaseUser != null;
        if (chatList.get(position).getSender().equals(firebaseUser.getUid())) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    //ViewHolder Class ,,NOTE ..Write it first when you start to code
    static class MyHolder extends RecyclerView.ViewHolder {

        //Views
        ImageView profileIv, messageIv;
        TextView messageTv, timeTv, isSeenTv;
        LinearLayout messageLayout;

        MyHolder(@NonNull View itemView) {

            super(itemView);

            //init views
            profileIv = itemView.findViewById(R.id.profileIvChat);
            messageIv = itemView.findViewById(R.id.messageIv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            isSeenTv = itemView.findViewById(R.id.isSeenTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);


        }
    }
}
