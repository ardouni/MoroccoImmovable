package com.morocco.immovable.Entities;

public class House {
    private int id;
    private String description;
    private int owner;
    private float price;
    private String type;
    private boolean status;
    private String photo;

    public House() {
    }

    public House(int id, String description, int owner, float price, String type, boolean status, String photo) {
        this.id = id;
        this.description = description;
        this.owner = owner;
        this.price = price;
        this.type = type;
        this.status = status;
        this.photo = photo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
