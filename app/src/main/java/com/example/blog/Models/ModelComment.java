package com.example.blog.Models;

public class ModelComment {

    //same as comments in post database
    private String cId, comment, timeStamp, uid, uDp, uName, uEmail;

    public ModelComment() {
    }

    public ModelComment(String cId, String comment, String timeStamp, String uid, String uDp, String uName, String uEmail) {
        this.cId = cId;
        this.comment = comment;
        this.timeStamp = timeStamp;
        this.uid = uid;
        this.uDp = uDp;
        this.uName = uName;
        this.uEmail = uEmail;
    }

    public String getcId() {
        return cId;
    }

    public void setcId(String cId) {
        this.cId = cId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getuDp() {
        return uDp;
    }

    public void setuDp(String uDp) {
        this.uDp = uDp;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public String getuEmail() {
        return uEmail;
    }

    public void setuEmail(String uEmail) {
        this.uEmail = uEmail;
    }
}
