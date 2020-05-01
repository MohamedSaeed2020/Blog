package com.example.blog.Adapters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blog.Activities.AddPostActivity;
import com.example.blog.Models.ModelPost;
import com.example.blog.Activities.PostDetailsActivity;
import com.example.blog.Activities.PostLikedByActivity;
import com.example.blog.R;
import com.example.blog.Activities.UsersProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
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

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder> {

    private Context context;
    private List<ModelPost> postList;
    private String myUID;
    private ProgressDialog progressDialog;
    private DatabaseReference likesRef; //for likes reference
    private DatabaseReference postsRef; //for posts reference
    private boolean mProcessLike = false;


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public AdapterPosts(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        this.myUID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");

    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, parent, false);
        return new MyHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, final int position) {

        //get data
        final String uid = postList.get(position).getUid();
        String uEmail = postList.get(position).getuEmail();
        String uName = postList.get(position).getuName();
        String uDp = postList.get(position).getuDp();
        final String pId = postList.get(position).getpId();
        String pTitle = postList.get(position).getpTitle();
        String pDescription = postList.get(position).getpDescription();
        final String pImage = postList.get(position).getpImage();
        String pTimeStamp = postList.get(position).getpTime();
        String pLikes = postList.get(position).getpLikes();  //contains total number of likes for a post
        String pComments = postList.get(position).getpComments();  //contains total number of comments for a post

        //convert time stamp to dd/mm/yyyy hh:mm: AM/PM
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));  //should be Long
        //MM should be capital to be different from mm for minute
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        //int progressDialog
        progressDialog = new ProgressDialog(context);


        //set data
        holder.uNameTv.setText(uName);
        holder.pTimeTv.setText(pTime);
        holder.pTitleTv.setText(pTitle);
        holder.pDescriptionTv.setText(pDescription);
        holder.pLikesTv.setText(pLikes + " Likes"); //e.g. 100 Likes
        holder.pCommentsTv.setText(pComments + " Comments"); //e.g. 100 Comments

        //set likes for each post
        setLikes(holder, pId);


        //set user dp
        try {
            //image received, set it to user image in the post
            Picasso.get().load(uDp).placeholder(R.drawable.ic_default_users).into(holder.uPictureIv);

        } catch (Exception ex) {
            //there exception getting picture,set default picture
            Picasso.get().load(R.drawable.ic_default_users).into(holder.uPictureIv);
        }

        //set post image
        //if there is no image i.e. pImage.equals("noImage") then hide image view
        if (pImage.equals("noImage")) {
            holder.pImageIv.setVisibility(View.GONE);
        } else {
            holder.pImageIv.setVisibility(View.VISIBLE);

            try {
                //image received, set it to post image in the post
                Picasso.get().load(pImage).placeholder(R.drawable.ic_default_users).into(holder.pImageIv);

            } catch (Exception ex) {
                //there exception getting picture,set default picture
                Picasso.get().load(R.drawable.ic_default_users).into(holder.pImageIv);
            }
        }

        //handle more options button clicks listener
        holder.moreBtn.setOnClickListener(view -> showMoreOptions(holder.moreBtn, uid, myUID, pId, pImage));

        //handle like button clicks listener
        holder.likeBtn.setOnClickListener(view -> {
            /*get total number of likes for the post, whose like button clicked.
             * if currently signed in user hasn't liked it before.
             * increase the value by 1, otherwise decrease the value by 1*/
            final int pLikes1 = Integer.parseInt(postList.get(position).getpLikes());
            mProcessLike = true;
            //get id of the post clicked
            final String postId = postList.get(position).getpId();
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (mProcessLike) {
                        if (dataSnapshot.child(postId).hasChild(myUID)) {
                            //already liked so remove like
                            postsRef.child(postId).child("pLikes").setValue("" + (pLikes1 - 1));
                            likesRef.child(postId).child(myUID).removeValue();
                            mProcessLike = false;
                        } else {
                            //not liked, like it
                            postsRef.child(postId).child("pLikes").setValue("" + (pLikes1 + 1));
                            likesRef.child(postId).child(myUID).setValue("Liked");
                            mProcessLike = false;
                            addToHisNotifications("" + uid, "" + pId);
                        }
                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        });
        //handle comment button clicks listener
        holder.commentBtn.setOnClickListener(view -> {
            //start post detail activity
            Intent intent = new Intent(context, PostDetailsActivity.class);
            intent.putExtra("postId", pId);//we will get detail of post using this id,it is id of the clicked post
            context.startActivity(intent);
        });
        //handle share button clicks listener
        holder.shareBtn.setOnClickListener(view -> Toast.makeText(context, "Share", Toast.LENGTH_SHORT).show());

        //handle when currently signed in user click on user name on the post
        holder.uNameTv.setOnClickListener(view -> {
            /*Go to UsersProfileActivity based on his uid to show its data*/
            Intent intent = new Intent(context, UsersProfileActivity.class);
            intent.putExtra("uid", uid);
            context.startActivity(intent);
        });
        //handle post likes count click listener
        holder.pLikesTv.setOnClickListener(view -> {
            /*Go to PostLikedBy activity */
            Intent intent = new Intent(context, PostLikedByActivity.class);
            intent.putExtra("postId", pId);
            context.startActivity(intent);
        });


    }

    private void addToHisNotifications(String hisUid, String pId) {
        //timestamp for time and notifications id
        String timestamp = String.valueOf(System.currentTimeMillis());
        //or String timestamp=""+System.currentTimeMillis();

        //data to put in notifications in firebase database
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timeStamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notifications", "Liked your post");
        hashMap.put("sUid", myUID);

        //check if the post is mine then no send a notification in the notification fragment
        if (!hisUid.equals(myUID)) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                    .addOnSuccessListener(aVoid -> {

                        //added successfully

                    }).addOnFailureListener(e -> {

                //failed

            });
        }
    }

    private void setLikes(final MyHolder holder, final String pId) {

        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(pId).hasChild(myUID)) {
                    //user has liked this post
                    /*to indicate that the post is liked by this currently signed in user,
                     * change drawable left icon of the button, change text of like button
                     * from "like" to "liked"*/

                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    holder.likeBtn.setText("Liked");

                } else {
                    //user hasn't liked this post
                    /*to indicate that the post isn't liked by this currently signed in user,
                     * change drawable left icon of the button, change text of like button
                     * from "liked" to "like"*/

                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                    holder.likeBtn.setText("Like");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void showMoreOptions(ImageButton moreBtn, String uid, String myUID, final String pId, final String pImage) {

        //create popupMenu
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);

        //show delete option in posts of currently signed in user
        if (uid.equals(myUID)) {
            //Add items in the popupMenu. i1 is item id, i2 is order
            //only currently signed user can do this on his post
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 1, "Edit");
        }

        //any one can do this to any post
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "View Post Details");

        //item ClickListener
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                //delete is clicked
                beginDelete(pId, pImage);
            } else if (id == 1) {
                //edit is clicked
                //start AddPostActivity with "editPost" key and the clicked post id
                Intent intent = new Intent(context, AddPostActivity.class);
                intent.putExtra("key", "editPost");
                intent.putExtra("editPostId", pId);
                context.startActivity(intent);

            } else if (id == 2) {
                //view post details is clicked
                //start post detail activity
                Intent intent = new Intent(context, PostDetailsActivity.class);
                intent.putExtra("postId", pId);//we will get detail of post using this id,it is id of the clicked post
                context.startActivity(intent);
            }
            return false;
        });

        //show popupMenu
        popupMenu.show();
    }

    private void beginDelete(String pId, String pImage) {

        //progress dialog
        progressDialog.setMessage("Deleting...");
        progressDialog.show();

        //posts can be with or without image
        if (pImage.equals("noImage")) {
            //post without image
            deletePostWithoutImage(pId);
        } else {
            //post with image
            deletePostWithImage(pId, pImage);
        }
    }

    private void deletePostWithImage(final String pId, String pImage) {

        /*Steps:
         * 1) Delete image using its url from storage
         * 2) Delete post from database using post id (pid) */

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(aVoid -> {

            //image deleted, now delete the post from the database
            Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshot.getRef().removeValue();
                    }
                    progressDialog.dismiss();
                    Toast.makeText(context, "Post Deleted Successfully...", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            //after deleting the post, delete the related notifications for this post
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(myUID).child("Notifications").orderByChild("pId").equalTo(pId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                //if we use [dataSnapshot.getRef().removeValue();] the all data will be deleted
                                snapshot.getRef().removeValue();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

            //after deleting the post, delete the related likes for this post
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.hasChild(pId)) {
                        //already liked so remove like
                        likesRef.child(pId).removeValue();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }).addOnFailureListener(e -> {

            //failed, can't go further
            progressDialog.dismiss();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();

        });
    }

    private void deletePostWithoutImage(String pId) {

        Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    //if we use [dataSnapshot.getRef().removeValue();] the all data will be deleted
                    snapshot.getRef().removeValue();
                }
                progressDialog.dismiss();
                Toast.makeText(context, "Post Deleted Successfully...", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //after deleting the post, delete the related notifications for this post
        FirebaseDatabase.getInstance().getReference("Users")
                .child(myUID).child("Notifications").orderByChild("pId").equalTo(pId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            //if we use [dataSnapshot.getRef().removeValue();] the all data will be deleted
                            snapshot.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        //after deleting the post, delete the related likes for this post
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChild(pId)) {
                    //already liked so remove like
                    likesRef.child(pId).removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {

        //Views from row_posts.XML
        ImageView uPictureIv, pImageIv;
        TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn, commentBtn, shareBtn;

        MyHolder(@NonNull View itemView) {

            super(itemView);

            //init views
            uPictureIv = itemView.findViewById(R.id.uPictureIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pImageIv = itemView.findViewById(R.id.pImageIv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);


        }
    }
}
