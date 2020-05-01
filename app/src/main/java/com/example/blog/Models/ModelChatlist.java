package com.example.blog.Models;

public class ModelChatlist {

    private String id;  //we will use this id to get chat list,sender and receiver uid

    public ModelChatlist() {
    }

    public ModelChatlist(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
