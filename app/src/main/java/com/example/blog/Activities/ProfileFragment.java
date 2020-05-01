package com.example.blog.Activities;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blog.Adapters.AdapterPosts;
import com.example.blog.Models.ModelPost;
import com.example.blog.R;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;


public class ProfileFragment extends Fragment {

    //Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private DatabaseReference reference;

    //Storage
    private StorageReference storageReference;

    //Views from XML
    private ImageView profileIv, coverIv;
    private TextView nameTv, emailTv, phoneTv;

    //Progress dialog
    private ProgressDialog progressDialog;

    /*For picking image from
     * 1) Camera:  [Camera and Storage permissions are required
     * 2) Gallery: [Storage permission is required*/

    //Permission constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    //arrays of permissions to be requested
    private String[] cameraPermissions;
    private String[] galleryPermissions;

    //Uri of picked image
    private Uri image_uri;

    //For checking profile or cover photo
    private String profileOrCoverPhoto;

    private RecyclerView myPostsRecyclerView;
    private List<ModelPost> postList;
    private AdapterPosts adapterPosts;
    private String uid;

    private boolean mProcessUpdate = false;
    private boolean mProcessUpdateForComments = false;


    public ProfileFragment() {
        // Required empty public constructor
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        reference = database.getReference("Users");

        //init storage
        storageReference = FirebaseStorage.getInstance().getReference();

        //init views
        profileIv = view.findViewById(R.id.profileIv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        FloatingActionButton fab = view.findViewById(R.id.fab);


        //recyclerView and it's properties
        myPostsRecyclerView = view.findViewById(R.id.myPostsRecyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        //show newest posts first,for this load from last
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        /* use this setting to improve performance if you know that changes
         in content do not change the layout size of the RecyclerView*/
        myPostsRecyclerView.setHasFixedSize(true);
        myPostsRecyclerView.setLayoutManager(linearLayoutManager);
        //init post list
        postList = new ArrayList<>();


        //init progress dialog
        progressDialog = new ProgressDialog(getActivity());

        //int arrays of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        galleryPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        /* We have to get info of currently signed in user. We can get it using users's email or uid
           i'm gonna retrieve user detail using user email.
           By using orderByChild query we will show a detail from a node whose key named email has value equal to
           currently signed in email. It will search all the nodes, where the key matches it will get its details
        */

        Query query = reference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //check until required data is get
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    //get data
                    String name = "" + snapshot.child("name").getValue();
                    String email = "" + snapshot.child("email").getValue();
                    String phone = "" + snapshot.child("phone").getValue();
                    String profile = "" + snapshot.child("profile").getValue();
                    String cover = "" + snapshot.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    //profile image
                    try {
                        //if image is received, than set
                        Picasso.get().load(profile).placeholder(R.drawable.ic_default_image_white).into(profileIv);
                    } catch (Exception ex) {
                        //if there is any exception while getting image, then set default
                        Picasso.get().load(R.drawable.ic_default_image_white).into(profileIv);
                    }
                    //cover image
                    try {
                        //if image is received, than set
                        Picasso.get().load(cover).into(coverIv);
                    } catch (Exception ex) {
                        //if there is any exception while getting image, then set default
                    }


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //handle fab clicks
        fab.setOnClickListener(view1 -> showEditMyProfileDialog());

        checkUserStatus();

        loadMyPosts();

        return view;
    }


    private void loadMyPosts() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");

        Query query = reference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                postList.clear();
                //check until required data is get
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelPost modelPost = snapshot.getValue(ModelPost.class);

                    //get my posts
                    postList.add(modelPost);
                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //refresh adapter
                    adapterPosts.notifyDataSetChanged();
                    //set adapter to recycler view
                    myPostsRecyclerView.setAdapter(adapterPosts);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //in case of error
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void searchMyPost(final String searchQuery) {

        //get path of database named "Posts" that containing all posts info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //get all data from the path
        Query query = reference.orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                postList.clear();
                //check until required data is get
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelPost modelPost = snapshot.getValue(ModelPost.class);
                    assert modelPost != null;
                    if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())) {
                        //get all posts
                        postList.add(modelPost);
                    }
                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(), postList);
                    //refresh adapter
                    adapterPosts.notifyDataSetChanged();
                    //set adapter to recycler view
                    myPostsRecyclerView.setAdapter(adapterPosts);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                //in case of error
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showEditMyProfileDialog() {

        /*Show dialog containing option
         * 1) Edit Profile Picture
         * 2) Edit Cover Photo
         * 3) Edit Name
         * 4) Edit Phone
         * 5) Change Password*/

        //Options to be shown in the dialog
        String[] options = {"Edit Profile Picture", "Edit Cover Photo", "Edit Name", "Edit Phone", "Change Password"};

        //Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //Set title
        builder.setTitle("Choose An Action");
        //Set items to the dialog
        builder.setItems(options, (dialogInterface, i) -> {

            //Handle dialog items clicks
            if (i == 0) {
                //Edit profile clicked
                progressDialog.setMessage("Updating Profile Picture");
                profileOrCoverPhoto = "profile";
                showProfileAndCoverUpdateDialog();

            } else if (i == 1) {
                //Edit cover clicked
                progressDialog.setMessage("Updating Cover Photo");
                profileOrCoverPhoto = "cover";
                showProfileAndCoverUpdateDialog();

            } else if (i == 2) {
                //Edit name clicked
                progressDialog.setMessage("Updating Name");
                //calling method and pass key "name" as parameter to update it's value in database
                showNameAndPhoneUpdateDialog("name");

            } else if (i == 3) {
                //Edit phone clicked
                progressDialog.setMessage("Updating Phone");
                //calling method and pass key "phone" as parameter to update it's value in database
                showNameAndPhoneUpdateDialog("phone");

            } else if (i == 4) {
                //Edit phone clicked
                progressDialog.setMessage("Changing Password");
                //calling method and pass key "phone" as parameter to update it's value in database
                showPasswordUpdateDialog();

            }

        });
        //create and show the dialog
        builder.create().show();

    }

    private void showProfileAndCoverUpdateDialog() {
        //show dialog containing camera and gallery options to pick the image

        //Options to be shown in the dialog
        String[] options = {"Camera", "Gallery"};

        //Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //Set title
        builder.setTitle("Pick Image From");
        //Set items to the dialog
        builder.setItems(options, (dialogInterface, i) -> {

            //Handle dialog items clicks

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (i == 0) {
                    //Camera clicked
                    if (!checkCameraPermissions()) {
                        requestCameraPermissions();
                    } else {
                        pickFromCamera();
                    }

                } else if (i == 1) {
                    //Gallery clicked
                    if (!checkGalleryPermissions()) {
                        requestGalleryPermissions();
                    } else {
                        pickFromGallery();
                    }
                }
            } else {
                if (i == 0) {
                    //Camera clicked
                    pickFromCamera();
                } else if (i == 1) {
                    //Gallery clicked
                    pickFromGallery();

                }
            }

        });
        //create and show the dialog
        builder.create().show();
    }

    private boolean checkCameraPermissions() {
        //check if camera and storage permissions are enabled or not
        //return true if enabled and false if not enabled
        boolean storageResult = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        boolean cameraResult = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        return storageResult && cameraResult;
    }

    private void requestCameraPermissions() {
        //request runtime camera permission
        //If user press deny and then press on the button, and if press don't show again he won't receive this dialog any more
        if (ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                Manifest.permission.CAMERA)) {

            new AlertDialog.Builder(getActivity())
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed or your app will crash")
                    .setPositiveButton("OK", (dialog, which) -> requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();

        } else {
            requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
        }
    }

    private boolean checkGalleryPermissions() {
        //check if storage permission is enabled or not
        //return true if enabled and false if not enabled
        boolean result = ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestGalleryPermissions() {
        //request runtime storage permission
        //If user press deny and then press on the button, and if press don't show again he won't receive this dialog any more
        if (ActivityCompat.shouldShowRequestPermissionRationale(Objects.requireNonNull(getActivity()),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            new AlertDialog.Builder(getActivity())
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed or your app will crash")
                    .setPositiveButton("OK", (dialog, which) -> requestPermissions(galleryPermissions, GALLERY_REQUEST_CODE))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();

        }
        requestPermissions(galleryPermissions, GALLERY_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        /*this method is called when user press allow or deny from permission request dialog,
        so here we will handle permissions cases*/
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                //Picking from camera, first check if camera and storage permissions are allowed or not
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        //both permissions are granted
                        pickFromCamera();
                    } else {
                        //camera or storage permissions or both were denied
                        Toast.makeText(getActivity(), "Camera and storage both permissions are necessary", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case GALLERY_REQUEST_CODE: {
                //Picking from gallery, first check if storage permission is allowed or not
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        //permissions enabled
                        pickFromGallery();
                    } else {
                        //permissions denied
                        Toast.makeText(getActivity(), "Please enable storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;

        }
    }

    private void pickFromGallery() {
        //Intent of picking image from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {

        //Intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");

        //put image uri
        image_uri = Objects.requireNonNull(getActivity()).getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //this method will be called after picking image from camera or gallery
        if (resultCode == RESULT_OK) {

            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //image is picked from camera, set uri of image
                uploadProfileCoverPhoto(image_uri);

            }
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //image is picked from gallery, get and set uri of image
                assert data != null;
                image_uri = data.getData();
                uploadProfileCoverPhoto(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void uploadProfileCoverPhoto(final Uri uri) {

        //show progress dialog
        progressDialog.show();

        /*instead of creating separate function for profile picture and cover photo
         * i will do work for both in the same this function, to make check for this i will add a string variable
         * and assign it with the value "profile" when user click "Edit Profile Picture",
         * and assign it with the value "cover" when user click "Edit Cover Photo" */

        /* The parameter "uri" containing the uri of the image picked either from the camera or from the gallery
         * we will use UID of the currently signed in user as name of the image so there will be
         * only one image for profile and one image for cover for each user*/

        //path and name of image to be stored in firebase storage
        //e.g Users_Profile_Cover_Images/profile_e12f3456f789.jpg
        //e.g Users_Profile_Cover_Images/cover_e12f3456f789.jpg

        //path where images of user profile and cover will be stored
        String storagePath = "Users_Profile_Cover_Images/";
        String filePathAndName = storagePath + profileOrCoverPhoto + "_" + user.getUid();
        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri).addOnSuccessListener(taskSnapshot -> {

            //image is uploaded to the storage, now gets it's url and store it in user's database
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            final Uri downloadUri = uriTask.getResult();

            //check if image is uploaded or not and url is received
            if (uriTask.isSuccessful()) {
                //image uploaded
                //add/update url in users's database
                HashMap<String, Object> results = new HashMap<>();
                assert downloadUri != null;
                results.put(profileOrCoverPhoto, downloadUri.toString());
                reference.child(user.getUid()).updateChildren(results).addOnSuccessListener(aVoid -> {
                    //url is added successfully in user's database
                    //dismiss progress dialog
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), "Image Updated Successfully...", Toast.LENGTH_SHORT).show();

                }).addOnFailureListener(e -> {
                    //url isn't added successfully in user's database
                    //dismiss progress dialog
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), "Error Updating Image...", Toast.LENGTH_SHORT).show();
                });


                //if the profileOrCoverPhoto is profile also update user profile at posts references
                if (profileOrCoverPhoto.equals("profile")) {
                    mProcessUpdate = true;
                    mProcessUpdateForComments = true;
                    //get path of database named "Posts" that containing all posts info
                    final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                    //get all data from the path
                    Query query = reference.orderByChild("uid").equalTo(uid);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (mProcessUpdate) {
                                //check until required data is get
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                    String child = snapshot.getKey();
                                    HashMap<String, Object> result = new HashMap<>();
                                    result.put("uDp", downloadUri.toString());
                                    assert child != null;
                                    //dataSnapshot.getRef().child(child).updateChildren(result);
                                    //dataSnapshot.child(child).getRef().updateChildren(result);
                                    dataSnapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                    mProcessUpdate = false;
                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            //in case of error
                            Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                    //update user image in current users comments in posts
                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if (mProcessUpdateForComments) {
                                //check until required data is get
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                    String child = snapshot.getKey();
                                    if (dataSnapshot.child(child).hasChild("Comments")) {
                                        String child1 = "" + dataSnapshot.child(child).getKey();
                                        Query child2 = reference.child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                        child2.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                    String child = snapshot.getKey();
                                                    HashMap<String, Object> result = new HashMap<>();
                                                    result.put("uDp", downloadUri.toString());
                                                    assert child != null;
                                                    //dataSnapshot.getRef().child(child).updateChildren(result);
                                                    //dataSnapshot.child(child).getRef().updateChildren(result);
                                                    dataSnapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                                    mProcessUpdateForComments = false;
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                //in case of error
                                                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            //in case of error
                            Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            } else {
                //error
                progressDialog.dismiss();
                Toast.makeText(getActivity(), "Some error occurred", Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(e -> {

            //there is some error(s),get and show error message, dismiss the progress dialog
            progressDialog.dismiss();
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();

        });

    }

    private void showNameAndPhoneUpdateDialog(final String key) {

        //Custom alertDialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update " + key);

        //Set layout linear layout
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10, 10, 10, 10);

        //Views to be set in dialog
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter " + key);
        /*sets the width of a TextView or (EditText) to fit a text of n 'M' letters
        regardless of the actual text extension and text size*/
        editText.setMinEms(16);
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //Button update
        builder.setPositiveButton("Update", (dialogInterface, i) -> {

            //input text from edit text
            final String value = editText.getText().toString().trim();
            //validate if user has entered some thing or not
            if (!TextUtils.isEmpty(value)) {
                progressDialog.show();
                HashMap<String, Object> result = new HashMap<>();
                result.put(key, value);
                reference.child(user.getUid()).updateChildren(result).addOnSuccessListener(aVoid -> {
                    //updated, dismiss progress dialog
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), "Updated...", Toast.LENGTH_SHORT).show();


                }).addOnFailureListener(e -> {
                    //failed, dismiss progress dialog, get and show error message
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();

                });

                //if the key is name also update user name at posts references
                if (key.equals("name")) {
                    mProcessUpdate = true;
                    mProcessUpdateForComments = true;
                    //get path of database named "Posts" that containing all posts info
                    final DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                    //get all data from the path
                    Query query = reference.orderByChild("uid").equalTo(uid);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if (mProcessUpdate) {
                                //check until required data is get
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                    String child = snapshot.getKey();
                                    HashMap<String, Object> result = new HashMap<>();
                                    result.put("uName", value);
                                    assert child != null;
                                    //dataSnapshot.getRef().child(child).updateChildren(result);
                                    //dataSnapshot.child(child).getRef().updateChildren(result);
                                    dataSnapshot.getRef().child(child).child("uName").setValue(value);
                                    mProcessUpdate = false;

                                }
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            //in case of error
                            Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });


                    //update name in current users comments in posts
                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (mProcessUpdateForComments) {
                                //check until required data is get
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                                    String child = snapshot.getKey();
                                    assert child != null;
                                    if (dataSnapshot.child(child).hasChild("Comments")) {
                                        Query query = reference.child(child).child("Comments").orderByChild("uid").equalTo(uid);
                                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                    String child = snapshot.getKey();
                                                    HashMap<String, Object> result = new HashMap<>();
                                                    result.put("uName", value);
                                                    assert child != null;
                                                    //dataSnapshot.getRef().child(child).updateChildren(result);
                                                    //dataSnapshot.child(child).getRef().updateChildren(result);
                                                    dataSnapshot.getRef().child(child).child("uName").setValue(value);
                                                    mProcessUpdateForComments = false;
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                //in case of error
                                                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            //in case of error
                            Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                Toast.makeText(getActivity(), "Please Enter " + key, Toast.LENGTH_SHORT).show();
            }
        });
        //Button cancel
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {

            //Dismiss dialog
            dialogInterface.dismiss();
        });

        //create and show dialog
        builder.create().show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showPasswordUpdateDialog() {

        //inflate layout for dialog
        @SuppressLint("InflateParams") View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_update_password, null);

        final EditText cPasswordET = view.findViewById(R.id.cPasswordET);
        final EditText nPasswordET = view.findViewById(R.id.nPasswordET);
        Button updatePasswordBtn = view.findViewById(R.id.updatePasswordBtn);

        //password change dialog with custom layout
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        //create dialog
        final AlertDialog dialog = builder.create();
        //show dialog
        dialog.show();

        updatePasswordBtn.setOnClickListener(view1 -> {
            //Validate data
            String oldPassword = cPasswordET.getText().toString().trim();
            String newPassword = nPasswordET.getText().toString().trim();

            if (TextUtils.isEmpty(oldPassword)) {
                Toast.makeText(getActivity(), "Please Enter Your Current Password..", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPassword.length() < 6) {
                Toast.makeText(getActivity(), "Password Length Must Be At Least 6 Character..", Toast.LENGTH_SHORT).show();
                return;
            }

            //dismiss dialog
            dialog.dismiss();
            updatePassword(oldPassword, newPassword);

        });


    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void updatePassword(String oldPassword, final String newPassword) {

        progressDialog.show();
        //get current user
        final FirebaseUser user = firebaseAuth.getCurrentUser();
        //before changing the password re-authenticate the user
        assert user != null;
        AuthCredential authCredential = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), oldPassword);
        user.reauthenticate(authCredential)
                .addOnSuccessListener(aVoid -> {
                    //successfully re-authenticated, begin update
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {

                                //updated successfully
                                progressDialog.dismiss();
                                Toast.makeText(getActivity(), "Password updated successfully...", Toast.LENGTH_SHORT).show();
                            }).addOnFailureListener(e -> {
                        //failed updating password, show reason
                        progressDialog.dismiss();
                        Toast.makeText(getActivity(), "Password updated successfully...", Toast.LENGTH_SHORT).show();

                    });

                }).addOnFailureListener(e -> {

            //failed re-authentication, show reason
            progressDialog.dismiss();
            Toast.makeText(getActivity(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();

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


        //Search view to search posts by post title or description
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
                    searchMyPost(s);
                } else {
                    //search text is empty, get all posts
                    loadMyPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user press any single letter
                //if search query is not empty then start search
                if (!TextUtils.isEmpty(s.trim())) {
                    //search text contain text, search it
                    searchMyPost(s);
                } else {
                    //search text is empty, get all posts
                    loadMyPosts();
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
        } else if (id == R.id.action_add_post) {

            //Go to add post activity
            startActivity(new Intent(getActivity(), AddPostActivity.class));
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
            uid = firebaseUser.getUid();
        } else {
            //User not signed in, so go to mainActivity to sign
            startActivity(new Intent(getActivity(), MainActivity.class));
            Objects.requireNonNull(getActivity()).finish();
        }

    }

}