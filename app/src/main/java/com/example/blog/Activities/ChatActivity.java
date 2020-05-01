package com.example.blog.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.blog.Adapters.AdapterChat;
import com.example.blog.Models.ModelChat;
import com.example.blog.Models.ModelUsers;
import com.example.blog.Notifications.APIService;
import com.example.blog.Notifications.Data;
import com.example.blog.Notifications.Sender;
import com.example.blog.Notifications.Token;
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
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {

    //Views
    Toolbar toolbar;
    RecyclerView recyclerView;
    ImageView profileIv;
    TextView userNameTv, userStatusTv;
    EditText messageEt;
    ImageButton sendBtn, attachBtn;

    //Firebase authentication
    FirebaseAuth firebaseAuth;
    DatabaseReference reference;

    //For checking if user has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userReferenceForSeen;


    List<ModelChat> chatList;
    AdapterChat adapterChat;

    String hisUid, myUid, hisImage;

    //For notification
    APIService apiService;
    boolean notify = false;

    //For volley
    RequestQueue requestQueue;

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

    //progress dialog
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //init views
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView = findViewById(R.id.chatRecyclerView);
        profileIv = findViewById(R.id.chatProfileIcon);
        userNameTv = findViewById(R.id.userNameTv);
        userStatusTv = findViewById(R.id.userStatusTv);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.sendMsgBtn);
        attachBtn = findViewById(R.id.attachBtn);

        //init ProgressDialog;
        progressDialog = new ProgressDialog(this);

        //Layout (Linear Layout) for recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        //show newest messages first,for this load from last
        linearLayoutManager.setStackFromEnd(true);
        //recycler view properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        //int arrays of permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        galleryPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        //create api service
        //apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        // Instantiate the RequestQueue.
        requestQueue = Volley.newRequestQueue(this);


        /*get user id from AdapterUsers when we press on any user from users list
         to get the profile picture, name and start chat with that*/

        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        //init firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        reference = FirebaseDatabase.getInstance().getReference("Users");

        //search user to get that user info
        Query userQuery = reference.orderByChild("uid").equalTo(hisUid);
        //get user name and picture
        userQuery.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //check until required info is received
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    //get data
                    String name = "" + snapshot.child("name").getValue();
                    hisImage = "" + snapshot.child("profile").getValue();
                    String onlineStatus = "" + snapshot.child("onlineStatus").getValue();
                    String typingStatus = "" + snapshot.child("typingTo").getValue();


                    //set data

                    //set name
                    userNameTv.setText(name);

                    //set typing status
                    if (typingStatus.equals(myUid)) {
                        userStatusTv.setText("typing...");
                    } else {
                        //set online status
                        if (onlineStatus.equals("online")) {
                            userStatusTv.setText(onlineStatus);
                        } else {
                            //convert timestamp to proper date
                            //convert time stamp to dd/mm/yyyy hh:mm: AM/PM
                            Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
                            calendar.setTimeInMillis(Long.parseLong(onlineStatus));  //should be Long
                            //MM should be capital to be different from mm for minute
                            String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa ", calendar).toString();
                            userStatusTv.setText("Last seen at: " + dateTime);
                        }
                    }

                    //set image
                    try {
                        //image received, set it to imageView in toolbar
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_image_white).into(profileIv);

                    } catch (Exception ex) {
                        //there exception getting picture,set default picture
                        Picasso.get().load(R.drawable.ic_default_image_white).into(profileIv);
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //handle sendMessage button clicks
        sendBtn.setOnClickListener(view -> {
            notify = true;
            //get text from edit text
            String message = messageEt.getText().toString().trim();
            //check if text is empty or not
            if (TextUtils.isEmpty(message)) {
                //Text is empty
                Toast.makeText(getApplicationContext(), "Can't send empty message", Toast.LENGTH_SHORT).show();
            } else {
                //text isn't empty
                sendMessage(message);
                //reset edit text after sending the message
                messageEt.setText("");
            }
        });

        //handle attach image button clicks
        attachBtn.setOnClickListener(view -> {
            //show image pick dialog
            showImagePickDialog();
        });


        //check edit text change listener
        messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (charSequence.toString().trim().length() == 0) {
                    checkTypingStatus("noOne");
                } else {
                    checkTypingStatus(hisUid); //uid of receiver
                }

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        readMessages();
        seenMessage();
        checkUserStatus(hisUid);

    }


    private void seenMessage() {
        userReferenceForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userReferenceForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelChat chat = snapshot.getValue(ModelChat.class);

                    assert chat != null;
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) {

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("watched", true);
                        snapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages() {

        chatList = new ArrayList<>();
        //get path of database named "Chats" containing chat info
        DatabaseReference chatReference = FirebaseDatabase.getInstance().getReference("Chats");
        //get all data from the path
        chatReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ModelChat chat = snapshot.getValue(ModelChat.class);

                    if (chat != null) {
                        if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                                chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)) {
                            chatList.add(chat);
                        }
                    }

                    //adapter
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    //refresh adapter
                    adapterChat.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterChat);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
                //image is picked from camera, use this image uri to upload to firebase database
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //image is picked from gallery, get and use this image uri to upload to firebase database
                assert data != null;
                image_uri = data.getData();
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void sendImageMessage(Uri image_uri) throws IOException {
        notify = true;

        progressDialog.setMessage("Sending Image...");
        progressDialog.show();

        final String timeStamp = String.valueOf(System.currentTimeMillis());
        String fileNameAndPath = "ChatImages/post_" + timeStamp;

        //get bitmap from image_uri
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] data = byteArrayOutputStream.toByteArray();  //convert image to byte

        //post with image
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        storageReference.putBytes(data).addOnSuccessListener(taskSnapshot -> {
            //image is uploaded to the storage, now gets it's url and all message info and store it in chats's database
            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
            while (!uriTask.isSuccessful()) ;
            final String downloadUri = Objects.requireNonNull(uriTask.getResult()).toString();

            //check if image is uploaded or not and url is received
            if (uriTask.isSuccessful()) {
                //image uploaded
                //add message image url in chats's database and also add some another info
                HashMap<String, Object> results = new HashMap<>();
                results.put("sender", myUid);
                results.put("receiver", hisUid);
                results.put("message", downloadUri);
                results.put("timestamp", timeStamp);
                results.put("type", "image");
                results.put("watched", false);

                //path to store message data
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
                //put data in this reference
                reference.push().setValue(results)
                        .addOnSuccessListener(aVoid -> {
                            //message data is added successfully in chat's database
                            //dismiss progress dialog
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Image is sent Successfully...", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                    //message data isn't added successfully in chats's database
                    //dismiss progress dialog
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error Sending Image...", Toast.LENGTH_SHORT).show();
                });

                // sending notification
                DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
                database.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ModelUsers user = dataSnapshot.getValue(ModelUsers.class);
                        assert user != null;
                        if (notify) {
                            sendNotification(hisUid, user.getName(), "Sent you a photo...");
                        }
                        notify = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                //create chatlist node/child in firebase database
                final DatabaseReference hatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                        .child(myUid)
                        .child(hisUid);
                hatRef1.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            hatRef1.child("id").setValue(hisUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                final DatabaseReference hatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                        .child(hisUid)
                        .child(myUid);
                hatRef2.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            hatRef2.child("id").setValue(myUid);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            } else {
                //error
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            //there is some error(s) when uploading the message image,get and show error message, dismiss the progress dialog
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        });


    }

    private void sendMessage(final String message) {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("type", "text");
        hashMap.put("watched", false);

        //.push() to put it inside random key
        databaseReference.child("Chats").push().setValue(hashMap);

        // sending notification
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelUsers user = dataSnapshot.getValue(ModelUsers.class);
                assert user != null;
                if (notify) {
                    sendNotification(hisUid, user.getName(), message);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //create chatlist node/child in firebase database
        final DatabaseReference hatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(myUid)
                .child(hisUid);
        hatRef1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    hatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference hatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(hisUid)
                .child(myUid);
        hatRef2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    hatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void sendNotification(final String hisUid, final String name, final String message) {

        final DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Token token = snapshot.getValue(Token.class);
                    Data data = new Data(
                            "" + myUid,
                            "" + name + ": " + message,
                            "" + "New Message",
                            "" + hisUid,
                            "ChatNotification",
                            R.drawable.launchericon
                    );
                    assert token != null;
                    Sender sender = new Sender(data, token.getToken());
/*
                    //USING RETROFIT
                   apiService.sendNotification(sender)
                            .enqueue(new Callback<com.example.blog.Notifications.Response>() {
                                @Override
                                public void onResponse(Call<com.example.blog.Notifications.Response> call, Response<com.example.blog.Notifications.Response> response) {
                                    *//*if (response.code() == 200) {
                                        assert response.body() != null;
                                        if (response.body().success != 1) {
                                            Toast.makeText(getApplicationContext(), "Failed!", Toast.LENGTH_SHORT).show();
                                        }
                                    }*//*

                                    // Toast.makeText(ChatActivity.this, response.message(), Toast.LENGTH_SHORT).show();


                                }

                                @Override
                                public void onFailure(Call<com.example.blog.Notifications.Response> call, Throwable t) {

                                }
                            });*/
                    //fcm json object request USING VOLLEY
                    try {
                        JSONObject senderJSONObject = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest =
                                new JsonObjectRequest("https://fcm.googleapis.com/fcm/send",
                                        senderJSONObject,
                                        response -> {

                                            //response of the request
                                            Log.d("JSON_RESPONSE", "onResponse: " + response.toString());


                                        }, error -> {
                                    //error occurred
                                    Log.d("JSON_RESPONSE", "onResponse: " + error.toString());
                                }) {
                                    @Override
                                    public Map<String, String> getHeaders() {

                                        //put parameters
                                        HashMap<String, String> headers = new HashMap<>();
                                        headers.put("Content-Type", "application/json");
                                        headers.put("Authorization", "Key=AAAAdCPk_tI:APA91bHYLayKX6rW9isOlZZSuk53pAFhDGiG5BLMtIPnP7zrMm2l5_tyYLtFfEEneTFf1ZW__z6B_JRgwhx6V9lOEzIDJVvuvOKyzMdnBdxQnWbZoIBJQ7ZBEOQtB0gxdGo5beJTMQvN");
                                        return headers;
                                    }
                                };

                        //add this request to the queue
                        requestQueue.add(jsonObjectRequest);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkOnlineStatus(String status) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

    private void checkTypingStatus(String typing) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

    //should be written to  initialize myUid
    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus(hisUid);
        //set online
        checkOnlineStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkUserStatus("none");
        //get timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());

        //set offline with last seen timestamp
        checkOnlineStatus(timestamp);
        checkTypingStatus("noOne");
        userReferenceForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        checkUserStatus(hisUid);
        //set online
        checkOnlineStatus("online");
        super.onResume();
    }

    private void checkUserStatus(String userId) {

        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            //User is signed in so stay here and show email of the user
            myUid = firebaseUser.getUid();  //currently signed in user's uid
            //save UID of the receiver in shared preferences
            SharedPreferences sharedPreferences = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("Receiver_USERID", userId);
            editor.apply();

        } else {
            //User not signed in, so go to mainActivity to sign
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }
}
