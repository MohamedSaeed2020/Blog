package com.example.blog.Notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.blog.Activities.ChatActivity;
import com.example.blog.Activities.PostDetailsActivity;
import com.example.blog.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

//FirebaseMessagingService instead of firebaseInstanceIdService because it is deprecated
public class FirebaseService extends FirebaseMessagingService {


    private static final String ADMIN_CHANNEL_ID = "admin_channel";

    //onNewToken instead of onTokenRefresh because it is deprecated
    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.d("NEW_TOKEN", s);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            updateToken(s);
        } else {
            Log.d("THE_TOKEN", "Will change token when you login for security reason");

        }
    }

    private void updateToken(String refreshToken) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token = new Token(refreshToken);
        assert firebaseUser != null;
        reference.child(firebaseUser.getUid()).setValue(token);


    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        //get savedReceiverUser and savedCurrentUser from shared preferences
        SharedPreferences currentSP = getSharedPreferences("SP_USER", MODE_PRIVATE);
        //SharedPreferences receiverSP = getSharedPreferences("SP_RECEIVER_USER", MODE_PRIVATE);
        String savedReceiverUser = currentSP.getString("Receiver_USERID", "NONE");
        String savedCurrentUser = currentSP.getString("Current_USERID", "NONE");

        /*Now there are tow types of notifications
         *              > notificationType="PostNotification"
         *              > notificationType="ChatNotification"  */

        //notificationType is an Object in the way of key and value
        String notificationType = remoteMessage.getData().get("notificationType");
        assert notificationType != null;
        if (notificationType.equals("PostNotification")) {
            //Post Notification
            String sender = remoteMessage.getData().get("sender");
            String pId = remoteMessage.getData().get("pId");
            String pTitle = remoteMessage.getData().get("pTitle");
            String pDescription = remoteMessage.getData().get("pDescription");


            //if user is same that has posted don't show notification
            assert sender != null;
            if (!sender.equals(savedCurrentUser)) {
                showPostNotification("" + pId, "" + pTitle, "" + pDescription);
            }


        } else if (notificationType.equals("ChatNotification")) {
            //Chat Notification
            String sent = remoteMessage.getData().get("sent");
            String user = remoteMessage.getData().get("user");
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            assert sent != null;
            if (firebaseUser != null && sent.equals(firebaseUser.getUid())) {
                if (!savedReceiverUser.equals(user)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        sendOreoAndAboveNotification(remoteMessage);
                    } else {
                        sendNormalNotification(remoteMessage);
                    }
                }
            }
        }

    }

    private void showPostNotification(String pId, String pTitle, String pDescription) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationID = new Random().nextInt(1000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupPostNotificationChannel(notificationManager);
        }

        //show post details activity using post id when notification clicked
        Intent intent = new Intent(this, PostDetailsActivity.class);
        intent.putExtra("postId", pId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);

        //Large icon
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.launchericon);
        Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ADMIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.launchericon)
                .setLargeIcon(largeIcon)
                .setContentTitle(pTitle)
                .setAutoCancel(true)
                .setContentText(pDescription)
                .setSound(notificationSoundUri)
                .setContentIntent(pendingIntent);

        //show notification
        assert notificationManager != null;
        notificationManager.notify(notificationID, builder.build());


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupPostNotificationChannel(NotificationManager notificationManager) {

        String channelName = "New Notification";
        String channelDescription = "Device to device post notification";
        NotificationChannel adminChannel = new NotificationChannel(ADMIN_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        adminChannel.setDescription(channelDescription);
        adminChannel.enableLights(true);
        adminChannel.setLightColor(Color.RED);
        adminChannel.enableVibration(true);

        if (notificationManager != null) {
            notificationManager.createNotificationChannel(adminChannel);
        }


    }

    private void sendNormalNotification(RemoteMessage remoteMessage) {
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        assert user != null;
        int j = Integer.parseInt(user.replaceAll("[\\D]", ""));  //Request code
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("hisUid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), j, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        assert icon != null;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), ADMIN_CHANNEL_ID)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int i = 0;
        if (j > i) {
            i = j;
        }
        assert notificationManager != null;
        notificationManager.notify(i, builder.build());

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendOreoAndAboveNotification(RemoteMessage remoteMessage) {

        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        assert user != null;
        int j = Integer.parseInt(user.replaceAll("[\\D]", ""));  //Request code
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("hisUid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), j, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        OreoAndAboveNotification oreoAndAboveNotification = new OreoAndAboveNotification(this);
        Notification.Builder builder = oreoAndAboveNotification.getOreoAndAboveNotification(title, body, pendingIntent, defaultSound, icon);

        int i = 0;
        if (j > i) {
            i = j;
        }
        oreoAndAboveNotification.getManager().notify(i, builder.build());
    }


}
