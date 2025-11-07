package com.community.user.geo;

public interface GeocodingProvider {
    record GeoPoint(double lat, double lng) {
    }

    GeoPoint forward(String address, String city) throws Exception;

    String reverse(double lat, double lng) throws Exception;
}

