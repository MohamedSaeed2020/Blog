package com.example.blog.Notifications;

public class Data {

    //user who will send , sent to whom will be sent
    private String user, body, title, sent, notificationType;
    private int icon;


    public Data(String user, String body, String title, String sent, String notificationType, int icon) {
        this.user = user;
        this.body = body;
        this.title = title;
        this.sent = sent;
        this.notificationType = notificationType;
        this.icon = icon;
    }

    public Data() {
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSent() {
        return sent;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
