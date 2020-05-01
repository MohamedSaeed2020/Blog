package com.example.blog.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blog.Activities.ChatActivity;
import com.example.blog.Models.ModelUsers;
import com.example.blog.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;


public class AdapterChatlist extends RecyclerView.Adapter<AdapterChatlist.ViewHolder> {

    private Context context;
    private List<ModelUsers> usersList;
    private HashMap<String, String> lastMessageHashMap;

    public AdapterChatlist(Context context, List<ModelUsers> usersList) {
        this.context = context;
        this.usersList = usersList;
        lastMessageHashMap = new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_chatlist, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        //get data

        final String hisUid = usersList.get(position).getUid();
        String userImage = usersList.get(position).getProfile();
        String userName = usersList.get(position).getName();
        String lastMessage = lastMessageHashMap.get(hisUid);

        //set data
        holder.nameTv.setText(userName);
        if (lastMessage == null || lastMessage.equals("default")) {
            //holder.lastMessageTv.setVisibility(View.GONE);
            holder.lastMessageTv.setVisibility(View.VISIBLE);
            holder.lastMessageTv.setText("Write Your First Message");
        } else {
            holder.lastMessageTv.setVisibility(View.VISIBLE);
            holder.lastMessageTv.setText(lastMessage);
        }

        //set user profile in chatlist
        try {
            //image received, set it to post image in the post
            Picasso.get().load(userImage).placeholder(R.drawable.ic_default_users).into(holder.profileIv);

        } catch (Exception ex) {
            //there exception getting picture,set default picture
            Picasso.get().load(R.drawable.ic_default_users).into(holder.profileIv);
        }

        //set online status of other users in chatlist
        if (usersList.get(position).getOnlineStatus().equals("online")) {
            //online
            holder.onlineStatusIv.setImageResource(R.drawable.circle_online);

        } else {
            holder.onlineStatusIv.setImageResource(R.drawable.circle_offline);
        }

        //handle click of user in chatlist
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("hisUid", hisUid);
            context.startActivity(intent);
        });


    }

    public void setLastMessage(String userId, String lastMessage) {
        lastMessageHashMap.put(userId, lastMessage);
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView profileIv, onlineStatusIv;
        TextView nameTv, lastMessageTv;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            lastMessageTv = itemView.findViewById(R.id.lastMessageTv);
        }
    }
}