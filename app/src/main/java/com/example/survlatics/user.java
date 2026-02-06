package com.example.survlatics;

public class user {

    private String email;
    private String role;
    private String uid;

    public user() {}

    public user(String email, String role, String uid) {
        this.email = email;
        this.role = role;
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getUid() {
        return uid;
    }
}
