package com.example.blog.Notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:Key=AAAAdCPk_tI:APA91bHYLayKX6rW9isOlZZSuk53pAFhDGiG5BLMtIPnP7zrMm2l5_tyYLtFfEEneTFf1ZW__z6B_JRgwhx6V9lOEzIDJVvuvOKyzMdnBdxQnWbZoIBJQ7ZBEOQtB0gxdGo5beJTMQvN"
            }
    )


    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
