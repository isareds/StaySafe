package com.staysafe.staysafe.models;

/**
 * Created by User on 13/09/20161
 */

public class Alert {

    public String phone;
    public double latitude, longitude;
    public String type;
    public String status;
    public long timestamp;

    public Alert(){}

    public Alert(String phoneNumber, double latitude, double longitude, String alertType){
        this.phone = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = alertType;
        this.status = "queuing";
        this.timestamp = System.currentTimeMillis() / 1000;
    }

}
