package com.community.user.geo.impl;

import com.community.user.geo.GeocodingProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class AMapGeocodingProvider implements GeocodingProvider {
    private final RestClient rest = RestClient.create();

    private String key() {
        String v = System.getenv("AMAP_KEY");
        if (v == null || v.isBlank()) throw new IllegalStateException("缺少环境变量 AMAP_KEY");
        return v;
    }

    @Override
    public GeoPoint forward(String address, String city) {
        String addr = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String c = (city == null || city.isBlank()) ? "" : "&city=" + URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://restapi.amap.com/v3/geocode/geo?address=" + addr + c + "&key=" + key();
        Map body = rest.get().uri(url).accept(MediaType.APPLICATION_JSON).retrieve().body(Map.class);
        var geocodes = (java.util.List<Map<String, Object>>) body.get("geocodes");
        if (geocodes == null || geocodes.isEmpty()) throw new RuntimeException("地理编码失败");
        String[] ll = String.valueOf(geocodes.get(0).get("location")).split(",");
        return new GeoPoint(Double.parseDouble(ll[1]), Double.parseDouble(ll[0]));
    }

    @Override
    public String reverse(double lat, double lng) {
        String url = "https://restapi.amap.com/v3/geocode/regeo?location=" + lng + "," + lat + "&key=" + key();
        Map body = rest.get().uri(url).accept(MediaType.APPLICATION_JSON).retrieve().body(Map.class);
        Map regeocode = (Map) body.get("regeocode");
        return regeocode == null ? null : String.valueOf(regeocode.get("formatted_address"));
    }
}

