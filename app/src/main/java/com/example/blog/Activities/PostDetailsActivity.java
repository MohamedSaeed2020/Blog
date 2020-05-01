package com.example.blog.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blog.Adapters.AdapterComments;
import com.example.blog.Models.ModelComment;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PostDetailsActivity extends AppCompatActivity {

    //views
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
    ImageButton moreBtn;
    Button likeBtn, shareBtn;

    //add comment views
    EditText commentEt;
    ImageView cAvatarIv;
    ImageButton sendBtn;

    //to get detail of user and post
    String myUid, hisUid, myEmail, myName, myDp,
            postId, pImage, pLikes, pComments, hisDp, hisName;

    //progress dialog
    ProgressDialog progressDialog;

    boolean mProcessComment = false;
    boolean mProcessLike = false;

    RecyclerView recyclerView;
    List<ModelComment> commentList;
    AdapterComments adapterComments;
    DatabaseReference likesRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        //Action bar and it's properties
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Post Details");
        actionBar.setDisplayHomeAsUpEnabled(true);

        progressDialog = new ProgressDialog(this);

        //init views
        uPictureIv = findViewById(R.id.uPictureIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTv = findViewById(R.id.pTimeTv);
        moreBtn = findViewById(R.id.moreBtn);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pImageIv = findViewById(R.id.pImageIv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);
        recyclerView = findViewById(R.id.commentsRecyclerView);
        commentEt = findViewById(R.id.commentEt);
        cAvatarIv = findViewById(R.id.cAvatarIv);
        sendBtn = findViewById(R.id.sendBtn);
        likesRef = FirebaseDatabase.getInstance().getReference("Likes");


        //get id of post using intent from Adapter post  when click of comment or show more buttons
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        loadPostInfo();

        checkUserStatus();

        loadUserInfo();

        setLikes();

        //set subtitle for the actionbar
        actionBar.setSubtitle("SignedIn as: " + myEmail);

        loadComments();

        //handle send comment button click listener
        sendBtn.setOnClickListener(view -> postComment());

        //handle like post button click listener
        likeBtn.setOnClickListener(view -> likePost());

        //handle share button clicks listener
        shareBtn.setOnClickListener(view -> Toast.makeText(PostDetailsActivity.this, "Share", Toast.LENGTH_SHORT).show());

        //handle show more option button click listener
        moreBtn.setOnClickListener(view -> showMoreOptions());

        //handle post likes count click listener
        pLikesTv.setOnClickListener(view -> {
            /*Go to PostLikedBy activity */
            Intent intent1 = new Intent(PostDetailsActivity.this, PostLikedByActivity.class);
            intent1.putExtra("postId", postId);
            startActivity(intent1);
        });

        //handle when currently signed in user click on user name on the post
        uNameTv.setOnClickListener(view -> {
            /*Go to UsersProfileActivity based on his uid to show its data*/
            Intent intent2 = new Intent(this, UsersProfileActivity.class);
            intent2.putExtra("uid", hisUid);
            startActivity(intent2);
        });


    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();  //Go to previous activity
        return super.onSupportNavigateUp();
    }

    private void loadPostInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = reference.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //get checking the posts until get the required post
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    //get data
                    String pTitle = "" + snapshot.child("pTitle").getValue();
                    String pDescription = "" + snapshot.child("pDescription").getValue();
                    pLikes = "" + snapshot.child("pLikes").getValue();
                    String pTimeStamp = "" + snapshot.child("pTime").getValue();
                    pImage = "" + snapshot.child("pImage").getValue();
                    hisDp = "" + snapshot.child("uDp").getValue();
                    hisUid = "" + snapshot.child("uid").getValue();
                    String uEmail = "" + snapshot.child("uEmail").getValue();
                    hisName = "" + snapshot.child("uName").getValue();
                    pComments = "" + snapshot.child("pComments").getValue();

                    //convert time stamp to dd/mm/yyyy hh:mm: AM/PM
                    Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));  //should be Long
                    //MM should be capital to be different from mm for minute
                    String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

                    //set data to views
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescription);
                    pLikesTv.setText(pLikes + " Likes");
                    pCommentsTv.setText(pComments + " Comments");
                    pTimeTv.setText(pTime);
                    uNameTv.setText(hisName);

                    //set the image for the post
                    //if there is no image i.e. pImage.equals("noImage") then hide image view
                    if (pImage.equals("noImage")) {
                        pImageIv.setVisibility(View.GONE);
                    } else {
                        pImageIv.setVisibility(View.VISIBLE);

                        try {
                            //image received, set it to post image in the post
                            Picasso.get().load(pImage).placeholder(R.drawable.ic_default_users).into(pImageIv);

                        } catch (Exception ex) {
                            //there exception getting picture,set default picture
                            Picasso.get().load(R.drawable.ic_default_users).into(pImageIv);
                        }
                    }

                    //set user image in comment part
                    try {
                        //image received, set it to post image in the post
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_default_users).into(uPictureIv);

                    } catch (Exception ex) {
                        //there exception getting picture,set default picture
                        Picasso.get().load(R.drawable.ic_default_users).into(uPictureIv);
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkUserStatus() {

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            //User is signed in so stay here
            myUid = firebaseUser.getUid();
            myEmail = firebaseUser.getEmail();
        } else {
            //User not signed in, so go to mainActivity to sign
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    private void loadUserInfo() {

        //get current user info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        Query query = reference.orderByChild("uid").equalTo(myUid);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    myName = "" + snapshot.child("name").getValue();
                    myDp = "" + snapshot.child("profile").getValue();


                    //set data before enter comment edittext
                    try {
                        //image received, set it to post image in the post
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_users).into(cAvatarIv);

                    } catch (Exception ex) {
                        //there exception getting picture,set default picture
                        Picasso.get().load(R.drawable.ic_default_users).into(cAvatarIv);
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void setLikes() {

        //when the details of post is loading,also check of current user has liked it or not
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference("Likes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child(postId).hasChild(myUid)) {
                    //user has liked this post
                    /*to indicate that the post is liked by this currently signed in user,
                     * change drawable left icon of the button, change text of like button
                     * from "like" to "liked"*/

                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    likeBtn.setText("Liked");

                } else {
                    //user hasn't liked this post
                    /*to indicate that the post isn't liked by this currently signed in user,
                     * change drawable left icon of the button, change text of like button
                     * from "liked" to "like"*/

                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                    likeBtn.setText("Like");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadComments() {
        //layout(Linear) for recyclerview
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        //set layout to recyclerview
        recyclerView.setLayoutManager(linearLayoutManager);

        //init comments list
        commentList = new ArrayList<>();

        //path of the post to get it's comments
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        //get all data from the path
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelComment modelComment = snapshot.getValue(ModelComment.class);

                    commentList.add(modelComment);


                    //adapter
                    adapterComments = new AdapterComments(PostDetailsActivity.this, commentList, myUid, postId);
                    //refresh adapter
                    adapterComments.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterComments);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void postComment() {

        //get data from comment edittext
        String comment = commentEt.getText().toString().trim();
        //Validate
        if (TextUtils.isEmpty(comment)) {
            //no value is entered
            Toast.makeText(this, "Comment is empty", Toast.LENGTH_SHORT).show();
        } else {
            progressDialog.setMessage("Adding Comment");
            progressDialog.show();
            String timeStamp = String.valueOf(System.currentTimeMillis());
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("cId", timeStamp);
            hashMap.put("comment", comment);
            hashMap.put("timeStamp", timeStamp);
            hashMap.put("uid", myUid);
            hashMap.put("uEmail", myEmail);
            hashMap.put("uDp", myDp);
            hashMap.put("uName", myName);

            //put this data in database
            reference.child(timeStamp).setValue(hashMap).addOnSuccessListener(aVoid -> {
                //added successfully
                progressDialog.dismiss();
                Toast.makeText(PostDetailsActivity.this, "Comment added successfully...", Toast.LENGTH_SHORT).show();
                commentEt.setText("");
                updateCommentCount();
                addToHisNotifications("" + hisUid, "" + postId, "" + "Commented on your post", "comment");

            }).addOnFailureListener(e -> {
                //failed, not added successfully
                progressDialog.dismiss();
                Toast.makeText(PostDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                commentEt.setText("");
            });
        }


    }

    private void updateCommentCount() {

        //whenever user adds comment increase the comment count
        mProcessComment = true;
        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (mProcessComment) {

                    String comments = "" + dataSnapshot.child("pComments").getValue();
                    int newCommentValue = Integer.parseInt(comments) + 1;
                    reference.child("pComments").setValue("" + newCommentValue);
                    mProcessComment = false;

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void likePost() {

        /*get total number of likes for the post, whose like button clicked.
         * if currently signed in user hasn't liked it before.
         * increase the value by 1, otherwise decrease the value by 1*/
        mProcessLike = true;
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (mProcessLike) {
                    if (dataSnapshot.child(postId).hasChild(myUid)) {
                        //already liked so remove like
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) - 1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;
                    } else {
                        //not liked, like it
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) + 1));
                        likesRef.child(postId).child(myUid).setValue("Liked");
                        mProcessLike = false;
                        addToHisNotifications("" + hisUid, "" + postId, "" + "Liked your post", "like");
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void addToHisNotifications(String hisUid, String pId, String notifications, String likeOrComment) {
        //timestamp for time and notifications id
        String timestamp = String.valueOf(System.currentTimeMillis());
        //or String timestamp=""+System.currentTimeMillis();

        //data to put in notifications in firebase database
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timeStamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notifications", notifications);
        hashMap.put("sUid", myUid);

        if (likeOrComment.equals("like")) {
            if (!hisUid.equals(myUid)) {

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                reference.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                        .addOnSuccessListener(aVoid -> {

                            //added successfully

                        }).addOnFailureListener(e -> {

                    //failed

                });
            }
        } else if (likeOrComment.equals("comment")) {
            if (!hisUid.equals(myUid)) {

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                reference.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                        .addOnSuccessListener(aVoid -> {

                            //added successfully

                        }).addOnFailureListener(e -> {

                    //failed

                });
            }
        }

    }

    private void showMoreOptions() {

        //create popupMenu
        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        //show delete option in posts of currently signed in user
        if (hisUid.equals(myUid)) {
            //Add items in the popupMenu. i1 is item id, i2 is order
            //only currently signed user can do this on his post
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 1, "Edit");
        }

        //item ClickListener
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                //delete is clicked
                beginDelete();
            } else if (id == 1) {
                //edit is clicked
                //start AddPostActivity with "editPost" key and the clicked post id
                Intent intent = new Intent(PostDetailsActivity.this, AddPostActivity.class);
                intent.putExtra("key", "editPost");
                intent.putExtra("editPostId", postId);
                startActivity(intent);

            }
            return false;
        });

        //show popupMenu
        popupMenu.show();
    }

    private void beginDelete() {

        //progress dialog
        progressDialog.setMessage("Deleting...");
        progressDialog.show();

        //posts can be with or without image
        if (pImage.equals("noImage")) {
            //post without image
            deletePostWithoutImage();
        } else {
            //post with image
            deletePostWithImage();
        }
    }

    private void deletePostWithImage() {

        /*Steps:
         * 1) Delete image using its url from storage
         * 2) Delete post from database using post id (pid) */

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(aVoid -> {

            //image deleted, now delete the post from the database
            Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshot.getRef().removeValue();
                    }
                    progressDialog.dismiss();
                    Toast.makeText(PostDetailsActivity.this, "Post Deleted Successfully...", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            //after deleting the post, delete the related notifications for this post
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(myUid).child("Notifications").orderByChild("pId").equalTo(postId)
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

                    if (dataSnapshot.hasChild(postId)) {
                        //already liked so remove like
                        likesRef.child(postId).removeValue();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }).addOnFailureListener(e -> {

            //failed, can't go further
            progressDialog.dismiss();
            Toast.makeText(PostDetailsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        });
    }

    private void deletePostWithoutImage() {

        Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue();
                }
                progressDialog.dismiss();
                Toast.makeText(PostDetailsActivity.this, "Post Deleted Successfully...", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //after deleting the post, delete the related notifications for this post
        FirebaseDatabase.getInstance().getReference("Users")
                .child(myUid).child("Notifications").orderByChild("pId").equalTo(postId)
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

                if (dataSnapshot.hasChild(postId)) {
                    //already liked so remove like
                    likesRef.child(postId).removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }
}
