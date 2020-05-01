package com.example.blog.Notifications;

public class Token {

    /*An FCM token or much commonly known as registration token, is an ID issued by FCM connection servers to the client app
    that allows it to receive messages */

    private String token;

    public Token(String token) {
        this.token = token;
    }

    public Token() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
