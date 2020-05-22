package com.boringowl.rpgchat.models;

public class Contacts {
    private String name, image, status, uid;

    public Contacts() {
    }

    public void setStatus(String name, String status, String image, String uid) {
        this.name = name;
        this.image = image;
        this.status = status;
        this.uid = uid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
