package com.miniflo.femcare;

public class Clinic {

    private String placeId;
    private String name;
    private float rating;
    private int reviewCount;
    private String address;
    private double latitude;
    private double longitude;
    private boolean openNow;
    private float distanceKm;
    private boolean wellKnown;

    public Clinic() {
    }

    public Clinic(String placeId, String name, String address, float rating, int reviewCount, float distanceKm, boolean openNow, double latitude, double longitude) {
        this.placeId = placeId;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.distanceKm = distanceKm;
        this.openNow = openNow;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wellKnown = reviewCount >= 50;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isOpenNow() {
        return openNow;
    }

    public void setOpenNow(boolean openNow) {
        this.openNow = openNow;
    }

    public float getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(float distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean isWellKnown() {
        return wellKnown;
    }

    public void setWellKnown(boolean wellKnown) {
        this.wellKnown = wellKnown;
    }
}