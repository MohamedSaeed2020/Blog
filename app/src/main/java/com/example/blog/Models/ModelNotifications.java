package com.example.blog.Models;

public class ModelNotifications {

    //s means sender .. who make (comment or like) so sent a notification the post owner
    private String pId, timeStamp, pUid, notifications, sUid;

    public ModelNotifications() {

        //required
    }

    public ModelNotifications(String pId, String timeStamp, String pUid, String notifications, String sUid) {
        this.pId = pId;
        this.timeStamp = timeStamp;
        this.pUid = pUid;
        this.notifications = notifications;
        this.sUid = sUid;
    }

    public String getpId() {
        return pId;
    }

    public void setpId(String pId) {
        this.pId = pId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getpUid() {
        return pUid;
    }

    public void setpUid(String pUid) {
        this.pUid = pUid;
    }

    public String getNotifications() {
        return notifications;
    }

    public void setNotifications(String notifications) {
        this.notifications = notifications;
    }

    public String getsUid() {
        return sUid;
    }

    public void setsUid(String sUid) {
        this.sUid = sUid;
    }

}
