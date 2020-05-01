package com.example.blog.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blog.Activities.ChatActivity;
import com.example.blog.Models.ModelUsers;
import com.example.blog.R;
import com.example.blog.Activities.UsersProfileActivity;
import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyHolder> {

    private Context context;
    private List<ModelUsers> usersList;
    private String activityPlace;

    //Constructor
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public AdapterUsers(Context context, List<ModelUsers> usersList, String place) {
        this.context = context;
        this.usersList = usersList;
        this.activityPlace = place;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //inflate layout (row_users.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, final int position) {

        //get data
        final String hisUID = usersList.get(position).getUid();
        String userImage = usersList.get(position).getProfile();
        String userName = usersList.get(position).getName();
        final String userEmail = usersList.get(position).getEmail();

        //set data
        holder.mNameTv.setText(userName);
        holder.mEmailTv.setText(userEmail);
        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_default_users).into(holder.avatarIv);
        } catch (Exception ex) {

            holder.avatarIv.setImageResource(R.drawable.ic_default_users);
        }

        //Handle item clicked
        holder.itemView.setOnClickListener(view -> {

            if (activityPlace.equals("users")) {
                //show dialog containing profile and chat options to go
                //Options to be shown in the dialog
                String[] options = {"Profile", "Chat"};

                //Alert Dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                //Set title
                builder.setTitle("Go To");
                //Set items to the dialog
                builder.setItems(options, (dialogInterface, i) -> {

                    //Handle dialog items clicks

                    if (i == 0) {
                        //Profile clicked
                        /*Go to UsersProfileActivity based on his uid to show its data */
                        Intent intent = new Intent(context, UsersProfileActivity.class);
                        intent.putExtra("uid", hisUID);
                        context.startActivity(intent);
                    }
                    if (i == 1) {
                        //Chat clicked
                        /*Click user from user list to start chatting/messaging
                         * Start activity by putting UID of receiver
                         * we will user that UID to identify the user we are gonna to chat*/
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("hisUid", hisUID);
                        context.startActivity(intent);
                    }
                });
                //create and show the dialog
                builder.create().show();
            } else if (activityPlace.equals("peopleLiked")) {
                /*Go to UsersProfileActivity based on his uid to show its data */
                Intent intent = new Intent(context, UsersProfileActivity.class);
                intent.putExtra("uid", hisUID);
                context.startActivity(intent);
            }


        });

    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }


    //ViewHolder Class ,,NOTE ..Write it first when you start to code
    static class MyHolder extends RecyclerView.ViewHolder {

        //Views
        ImageView avatarIv;
        TextView mNameTv, mEmailTv;

        MyHolder(@NonNull View itemView) {

            super(itemView);

            //init views
            avatarIv = itemView.findViewById(R.id.avatarCIM);
            mNameTv = itemView.findViewById(R.id.row_nameTv);
            mEmailTv = itemView.findViewById(R.id.row_emailTv);

        }
    }

}
