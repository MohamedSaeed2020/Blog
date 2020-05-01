package com.example.blog.Adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blog.Models.ModelComment;
import com.example.blog.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.MyHolder> {

    private Context context;
    private List<ModelComment> commentList;
    private String muUid, postId;

    public AdapterComments(Context context, List<ModelComment> commentList, String muUid, String postId) {
        this.context = context;
        this.commentList = commentList;
        this.muUid = muUid;
        this.postId = postId;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_comments, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {

        //get data
        final String uid = commentList.get(position).getUid();
        String name = commentList.get(position).getuName();
        String email = commentList.get(position).getuEmail();
        String image = commentList.get(position).getuDp();
        final String cid = commentList.get(position).getcId();
        String comment = commentList.get(position).getComment();
        String timestamp = commentList.get(position).getTimeStamp();

        //convert time stamp to dd/mm/yyyy hh:mm: AM/PM
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(Long.parseLong(timestamp));  //should be Long
        //MM should be capital to be different from mm for minute
        String cTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        //set data
        holder.uNameTv.setText(name);
        holder.commentTv.setText(comment);
        holder.cTimeTv.setText(cTime);
        //set user dp
        try {
            //image received, set it to user image in the post
            Picasso.get().load(image).placeholder(R.drawable.ic_default_users).into(holder.commentAvatarIV);

        } catch (Exception ex) {
            //there exception getting picture,set default picture
            Picasso.get().load(R.drawable.ic_default_users).into(holder.commentAvatarIV);
        }

        //handle comment click listener
        holder.itemView.setOnClickListener(view -> {

            //check if this comment is by currently signed in user or no
            if (muUid.equals(uid)) {

                //my comment
                //show delete comment confirm dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure to delete this comment?");
                //Delete button
                builder.setPositiveButton("Delete", (dialogInterface, i) -> {
                    //delete comment
                    deleteComment(cid);
                });

                //cancel Delete button
                builder.setNegativeButton("Cancel", (dialogInterface, i) -> {
                    //dismiss dialog
                    dialogInterface.dismiss();
                });

                //create and show the dialog
                builder.create().show();

            } else {
                //not my comment
                Toast.makeText(context, "Can't delete other's comments...", Toast.LENGTH_SHORT).show();

            }

        });


    }

    private void deleteComment(String cid) {

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        reference.child("Comments").child(cid).removeValue();

        //now update the comments count
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                String comments = "" + dataSnapshot.child("pComments").getValue();
                int newCommentValue = Integer.parseInt(comments) - 1;
                reference.child("pComments").setValue("" + newCommentValue);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }


    static class MyHolder extends RecyclerView.ViewHolder {

        //Views from row_posts.XML
        ImageView commentAvatarIV;
        TextView uNameTv, cTimeTv, commentTv;

        MyHolder(@NonNull View itemView) {

            super(itemView);

            //init views
            commentAvatarIV = itemView.findViewById(R.id.commentAvatarIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            commentTv = itemView.findViewById(R.id.commentTv);
            cTimeTv = itemView.findViewById(R.id.timeTv);


        }
    }

}
