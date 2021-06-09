package com.example.assistant;

public class User {
    private String Uid, FirstName, LastName, Dob, Phone, Email, pwd;

    public User() {
    }

    public User(String uid, String firstName, String lastName, String dob, String phone, String email, String pwd) {
        Uid = uid;
        FirstName = firstName;
        LastName = lastName;
        Dob = dob;
        Phone = phone;
        Email = email;
        this.pwd = pwd;
    }

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    public String getFirstName() {
        return FirstName;
    }

    public void setFirstName(String firstName) {
        this.FirstName = firstName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lastName) {
        this.LastName = lastName;
    }

    public String getDob() {
        return Dob;
    }

    public void setDob(String dob) {
        this.Dob = dob;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        this.Phone = phone;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        this.Email = email;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String Pwd) {
        this.pwd = Pwd;
    }
}

