package com.community.user.geo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/geo")
@RequiredArgsConstructor
public class GeoController {
    private final GeocodingProvider provider;

    @GetMapping("/forward")
    public Object forward(@RequestParam String address, @RequestParam(required = false) String city) throws Exception {
        var p = provider.forward(address, city);
        return java.util.Map.of("lat", p.lat(), "lng", p.lng());
    }

    @GetMapping("/reverse")
    public Object reverse(@RequestParam double lat, @RequestParam double lng) throws Exception {
        return java.util.Map.of("address", provider.reverse(lat, lng));
    }
}

