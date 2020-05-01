package com.example.blog.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.blog.R;
import com.google.android.gms.tasks.Task;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;

    //Views
    EditText titleEt, descriptionEt;
    ImageView postIv;
    Button uploadBtn;

    //progress dialog
    ProgressDialog progressDialog;

    //info of post to be edited
    String editTitle, editDescription, editImage;

    //user info
    String name, email, uid, dp;

    //Permission constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    //arrays of permissions to be requested
    String[] cameraPermissions;
    String[] galleryPermissions;

    //Uri of picked image
    Uri image_uri = null;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);


        //Action bar
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Add New Post");
        actionBar.setDisplayHomeAsUpEnabled(true);

        //init views
        titleEt = findViewById(R.id.post_title_Et);
        descriptionEt = findViewById(R.id.post_description_Et);
        postIv = findViewById(R.id.post_image);
        uploadBtn = findViewById(R.id.post_upload_btn);

        //init ProgressDialog;
        progressDialog = new ProgressDialog(this);

        //get data from through intent from AdapterPosts
        Intent intent = getIntent();

        //get data and it's type from intent
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (("text/plains").equals(type)) {

                //text type data
                handleSendText(intent);
            } else if (type.startsWith("image")) {
                //image type data
                handleSendImage(intent);
            }
        }
        final String isUpdateKey = intent.getStringExtra("key");
        final String editPostId = intent.getStringExtra("editPostId");

        //validate if we come here to update post i.e. came from AdapterPost
        if (isUpdateKey != null) {
            if (isUpdateKey.equals("editPost")) {
                //update
                actionBar.setTitle("Update Post");
                uploadBtn.setText("Update");
                loadPostData(editPostId);

            }
        } else {
            actionBar.setTitle("Add New Post");
            uploadBtn.setText("Upload");
        }

        //init firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        checkUserStatus();

        //int arrays of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        galleryPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        //get some information of currently signed in user to include into the post
        userDbRef = FirebaseDatabase.getInstance().getReference("Users");
        //search user to get that user info
        Query userQuery = userDbRef.orderByChild("email").equalTo(email);
        //get user name and picture
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //check until required info is received
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    //get data
                    name = "" + snapshot.child("name").getValue();
                    email = "" + snapshot.child("email").getValue();
                    dp = "" + snapshot.child("profile").getValue();

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        actionBar.setSubtitle(email);


        //handle getting post image from gallery or camera click listener
        postIv.setOnClickListener(view -> {
            //show image pick dialog
            showImagePickDialog();

        });

        //handle upload post button click listener
        uploadBtn.setOnClickListener(view -> {

            String title = titleEt.getText().toString().trim();
            String description = descriptionEt.getText().toString().trim();
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(description)) {
                Toast.makeText(getApplicationContext(), "All Fields Are Required!", Toast.LENGTH_SHORT).show();
            } else {

                if (isUpdateKey != null) {
                    if (isUpdateKey.equals("editPost")) {
                        beginUpdate(title, description, editPostId);
                    }
                } else {
                    uploadData(title, description);

                }
            }


        });

    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();  //Go to previous activity
        return super.onSupportNavigateUp();
    }

    private void handleSendText(Intent intent) {
        //handle the received text
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            //set to description edit text
            descriptionEt.setText(sharedText);
        }
    }

    private void handleSendImage(Intent intent) {
        //handle the received image(uri)
        Uri imageURI = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageURI != null) {
            image_uri = imageURI;
            //set to post imageView
            postIv.setImageURI(image_uri);
        }
    }

    private void checkUserStatus() {

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            //User is signed in so stay here
            uid = firebaseUser.getUid();
            email = firebaseUser.getEmail();
        } else {
            //User not signed in, so go to mainActivity to sign
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    private void showImagePickDialog() {

        //show dialog containing camera and gallery options to pick the image

        //Options to be shown in the dialog
        String[] options = {"Camera", "Gallery"};

        //Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);

        return storageResult && cameraResult;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestCameraPermissions() {
        //request runtime camera permission
        //If user press deny and then press on the button, and if press don't show again he won't receive this dialog any more
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {

            new AlertDialog.Builder(this)
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
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestGalleryPermissions() {
        //request runtime storage permission
        //If user press deny and then press on the button, and if press don't show again he won't receive this dialog any more
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed or your app will crash")
                    .setPositiveButton("OK", (dialog, which) -> requestPermissions(galleryPermissions, GALLERY_REQUEST_CODE))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();

        }
        requestPermissions(galleryPermissions, GALLERY_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                        Toast.makeText(getApplicationContext(), "Camera and storage both permissions are necessary", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getApplicationContext(), "Please enable storage permission", Toast.LENGTH_SHORT).show();
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
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

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
                //set to image view
                postIv.setImageURI(image_uri);
            }
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //image is picked from gallery, get and set uri of image
                assert data != null;
                image_uri = data.getData();
                //set to image view
                postIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void loadPostData(String editPostId) {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = reference.orderByChild("pId").equalTo(editPostId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    //get data
                    editTitle = "" + snapshot.child("pTitle").getValue();
                    editDescription = "" + snapshot.child("pDescription").getValue();
                    editImage = "" + snapshot.child("pImage").getValue();

                    //set data to views
                    titleEt.setText(editTitle);
                    descriptionEt.setText(editDescription);
                    if (!editImage.equals("noImage")) {
                        try {
                            Picasso.get().load(editImage).placeholder(R.drawable.ic_default_users).into(postIv);

                        } catch (Exception e) {
                            Picasso.get().load(R.drawable.ic_default_users).into(postIv);

                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void uploadData(final String title, final String description) {

        progressDialog.setMessage("Publishing post...");
        progressDialog.show();

        //we use timeStamp for post-image name, post-id and post-publish-time
        final String timeStamp = String.valueOf(System.currentTimeMillis());
        //path where images of user post will be stored
        String storagePath = "Posts/post_" + timeStamp;
        if (postIv.getDrawable() != null) {

            //get image from imageview
            Bitmap bitmap = ((BitmapDrawable) postIv.getDrawable()).getBitmap();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();


            //post with image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(storagePath);
            storageReference.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {

                        //image is uploaded to the storage, now gets it's url and all post info and store it in user's database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        Uri downloadUri = uriTask.getResult();

                        //check if image is uploaded or not and url is received
                        if (uriTask.isSuccessful()) {
                            //image uploaded
                            //add post image url in post's database and also add some another info
                            HashMap<String, Object> results = new HashMap<>();
                            results.put("uid", uid);
                            results.put("uName", name);
                            results.put("uEmail", email);
                            results.put("uDp", dp);
                            results.put("pId", timeStamp);
                            results.put("pTitle", title);
                            results.put("pDescription", description);
                            assert downloadUri != null;
                            results.put("pImage", downloadUri.toString());
                            results.put("pTime", timeStamp);
                            results.put("pLikes", "0");
                            results.put("pComments", "0");

                            //path to store posts data
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                            //put data in this reference
                            reference.child(timeStamp).setValue(results)
                                    .addOnSuccessListener(aVoid -> {
                                        //post data is added successfully in posts's database
                                        //dismiss progress dialog
                                        progressDialog.dismiss();
                                        Toast.makeText(getApplicationContext(), "Post Published Successfully...", Toast.LENGTH_SHORT).show();
                                        //reset views
                                        titleEt.setText("");
                                        descriptionEt.setText("");
                                        postIv.setImageURI(null);
                                        image_uri = null;

                                        //send post notification
                                        prepareNotification(
                                                "" + timeStamp, //since we use timestamp for post id
                                                "" + name + " added new post",
                                                "" + title + "\n" + description
                                        );
                                    }).addOnFailureListener(e -> {
                                //post data isn't added successfully in posts's database
                                //dismiss progress dialog
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Error Publishing Post...", Toast.LENGTH_SHORT).show();
                            });


                        } else {
                            //error
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {

                //there is some error(s) when uploading the post image,get and show error message, dismiss the progress dialog
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            });

        } else {
            //post without image
            HashMap<String, Object> results = new HashMap<>();
            results.put("uid", uid);
            results.put("uName", name);
            results.put("uEmail", email);
            results.put("uDp", dp);
            results.put("pId", timeStamp);
            results.put("pTitle", title);
            results.put("pDescription", description);
            results.put("pImage", "noImage");
            results.put("pTime", timeStamp);
            results.put("pLikes", "0");
            results.put("pComments", "0");


            //path to store posts data
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
            //put data in this reference
            reference.child(timeStamp).setValue(results)
                    .addOnSuccessListener(aVoid -> {
                        //post data is added successfully in posts's database
                        //dismiss progress dialog
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Post Published Successfully...", Toast.LENGTH_SHORT).show();
                        //reset views
                        titleEt.setText("");
                        descriptionEt.setText("");
                        postIv.setImageURI(null);
                        image_uri = null;

                        //send post notification
                        prepareNotification(
                                "" + timeStamp, //since we use timestamp for post id
                                "" + name + " added new post",
                                "" + title + "\n" + description
                        );

                    }).addOnFailureListener(e -> {
                //post data isn't added successfully in posts's database
                //dismiss progress dialog
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Error Publishing Post...", Toast.LENGTH_SHORT).show();
            });
        }


    }

    private void prepareNotification(String pId, String title, String description) {

        //prepare data for notification
        String NOTIFICATION_TOPIC = "/topics/" + "POST"; //topic must match with what the receiver subscribed to
        String NOTIFICATION_TYPE = "PostNotification"; //now there are two notification type chat & post, so to differentiate in FirebaseMessaging class

        //prepare json what to send and where to send
        JSONObject notificationJO = new JSONObject();
        JSONObject notificationBodyJO = new JSONObject();

        //what to send
        try {
            notificationBodyJO.put("notificationType", NOTIFICATION_TYPE);
            notificationBodyJO.put("sender", uid);  //uid of currently signed in user
            notificationBodyJO.put("pId", pId); //post id
            notificationBodyJO.put("pTitle", title);  //e.g. mohamed added new post
            notificationBodyJO.put("pDescription", description); //content of  post

            /*the parameter (data) is constant and should be written as i wrote it*/
            //where to send
            notificationJO.put("to", NOTIFICATION_TOPIC);
            notificationJO.put("data", notificationBodyJO);
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        sendPostNotification(notificationJO);


    }

    private void sendPostNotification(JSONObject notificationJO) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJO, response -> {

            //response of the request
            Log.d("JSON_RESPONSE_SUCCESS", "onResponse: " + response.toString());


        }, error -> {
            //error occurred
            Log.d("JSON_RESPONSE_ERROR", "onResponse: " + error.toString());
        }) {
            @Override
            public Map<String, String> getHeaders() {

                //put parameters (required headers)
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Key=AAAAdCPk_tI:APA91bHYLayKX6rW9isOlZZSuk53pAFhDGiG5BLMtIPnP7zrMm2l5_tyYLtFfEEneTFf1ZW__z6B_JRgwhx6V9lOEzIDJVvuvOKyzMdnBdxQnWbZoIBJQ7ZBEOQtB0gxdGo5beJTMQvN");
                return headers;
            }
        };

        //add this request to the queue (enqueue the volley request)
        Volley.newRequestQueue(this).add(jsonObjectRequest);
    }

    private void beginUpdate(String title, String description, String editPostId) {

        //progress dialog
        progressDialog.setMessage("Updating Post...");
        progressDialog.show();

        if (!editImage.equals("noImage")) {
            //was with image
            updatePostWasWithImage(title, description, editPostId);
        } else if (postIv.getDrawable() != null) {
            //was without image, but now has image in image view
            updatePostWithNowImage(title, description, editPostId);
        } else {
            //was without image, but now still no image in image view
            updatePostWithoutImage(title, description, editPostId);

        }


    }

    private void updatePostWasWithImage(final String title, final String description, final String editPostId) {

        /*Steps:
         * 1) Delete image using its url from storage
         * 2) Upload the new image and update other post info like title and description*/

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        picRef.delete().addOnSuccessListener(aVoid -> {

            //image deleted, now upload the new image
            //for post-image name,post-id and publish time
            final String timeStamp = String.valueOf(System.currentTimeMillis());
            String filePathAndName = "Posts/post_" + timeStamp;

            //get image from imageview
            Bitmap bitmap = ((BitmapDrawable) postIv.getDrawable()).getBitmap();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();

            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            storageReference.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {

                        //image is uploaded to the storage, now gets it's url and all post info and store it in user's database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful()) ;
                        Uri downloadUri = uriTask.getResult();

                        //check if image is uploaded or not and url is received
                        if (uriTask.isSuccessful()) {
                            //image uploaded
                            //add post image url in post's database and also add some another info
                            HashMap<String, Object> results = new HashMap<>();
                            results.put("pTitle", title);
                            results.put("pDescription", description);
                            assert downloadUri != null;
                            results.put("pImage", downloadUri.toString());

                            //path to store posts data
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                            //put data in this reference
                            reference.child(editPostId).updateChildren(results)
                                    .addOnSuccessListener(aVoid1 -> {
                                        //post data is added successfully in posts's database
                                        //dismiss progress dialog
                                        progressDialog.dismiss();
                                        Toast.makeText(getApplicationContext(), "Post Updated Successfully...", Toast.LENGTH_SHORT).show();
                                        //reset views
                                        titleEt.setText("");
                                        descriptionEt.setText("");
                                        postIv.setImageURI(null);
                                        image_uri = null;
                                    }).addOnFailureListener(e -> {
                                //post data isn't added successfully in posts's database
                                //dismiss progress dialog
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Error Updating Post...", Toast.LENGTH_SHORT).show();
                            });


                        } else {
                            //error
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(e -> {

                //there is some error(s) when uploading the post image,get and show error message, dismiss the progress dialog
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

            });


        }).addOnFailureListener(e -> {

            //failed, can't go further
            progressDialog.dismiss();
            Toast.makeText(AddPostActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

        });
    }

    private void updatePostWithNowImage(final String title, final String description, final String editPostId) {

        //for post-image name,post-id and publish time
        final String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/post_" + timeStamp;

        //get image from imageview
        Bitmap bitmap = ((BitmapDrawable) postIv.getDrawable()).getBitmap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();

        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        storageReference.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {

                    //image is uploaded to the storage, now gets it's url and all post info and store it in user's database
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful()) ;
                    Uri downloadUri = uriTask.getResult();

                    //check if image is uploaded or not and url is received
                    if (uriTask.isSuccessful()) {
                        //image uploaded
                        //add post image url in post's database and also add some another info
                        HashMap<String, Object> results = new HashMap<>();
                        results.put("pTitle", title);
                        results.put("pDescription", description);
                        assert downloadUri != null;
                        results.put("pImage", downloadUri.toString());

                        //path to store posts data
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
                        //put data in this reference
                        reference.child(editPostId).updateChildren(results)
                                .addOnSuccessListener(aVoid -> {
                                    //post data is added successfully in posts's database
                                    //dismiss progress dialog
                                    progressDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Post Updated Successfully...", Toast.LENGTH_SHORT).show();
                                    //reset views
                                    titleEt.setText("");
                                    descriptionEt.setText("");
                                    postIv.setImageURI(null);
                                    image_uri = null;
                                }).addOnFailureListener(e -> {
                            //post data isn't added successfully in posts's database
                            //dismiss progress dialog
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Error Updating Post...", Toast.LENGTH_SHORT).show();
                        });


                    } else {
                        //error
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {

            //there is some error(s) when uploading the post image,get and show error message, dismiss the progress dialog
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

        });


    }

    private void updatePostWithoutImage(String title, String description, String editPostId) {
        HashMap<String, Object> results = new HashMap<>();
        results.put("pTitle", title);
        results.put("pDescription", description);
        results.put("pImage", "noImage");

        //path to store posts data
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //put data in this reference
        reference.child(editPostId).updateChildren(results)
                .addOnSuccessListener(aVoid -> {
                    //post data is added successfully in posts's database
                    //dismiss progress dialog
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Post Updated Successfully...", Toast.LENGTH_SHORT).show();
                    //reset views
                    titleEt.setText("");
                    descriptionEt.setText("");
                    postIv.setImageURI(null);
                    image_uri = null;
                }).addOnFailureListener(e -> {
            //post data isn't added successfully in posts's database
            //dismiss progress dialog
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), "Error Updating Post...", Toast.LENGTH_SHORT).show();
        });


    }

}


