package com.community.user.controller;

import com.community.common.util.ApiResponse;
import com.community.user.entity.Address;
import com.community.user.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;
    private final StringRedisTemplate stringRedisTemplate;

    private String defaultKey(Long userId) {
        return "user:default_address:" + userId;
    }

    @GetMapping
    @PreAuthorize("#userId.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse list(@PathVariable Long userId) {
        String def = stringRedisTemplate.opsForValue().get(defaultKey(userId));
        Long defaultAddressId = null;
        try {
            if (def != null) defaultAddressId = Long.valueOf(def);
        } catch (Exception ignore) {
        }
        return ApiResponse.ok()
                .data("addresses", addressService.listByUserId(userId))
                .data("defaultAddressId", defaultAddressId);
    }

    @PostMapping
    @PreAuthorize("#userId.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse add(@PathVariable Long userId, @RequestBody Address address) {
        address.setUserId(userId);
        try {
            addressService.save(address);
        } catch (DataIntegrityViolationException ex) {
            return ApiResponse.error("社区不存在，请检查 ID");
        }
        return ApiResponse.ok("地址添加成功").data("id", address.getId());
    }

    /**
     * 按 id 获取单条地址（下单用）
     */
    @GetMapping("/{addrId}")
    @PreAuthorize("#userId.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse getOne(@PathVariable Long userId, @PathVariable Long addrId) {
        var addr = addressService.getById(addrId);
        if (addr == null || !addr.getUserId().equals(userId)) return ApiResponse.error("地址不存在");
        return ApiResponse.ok().data("address", addr);
    }

    /**
     * 更新地址（detail/communityId）
     */
    @PutMapping("/{addrId}")
    @PreAuthorize("#userId.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse update(@PathVariable Long userId, @PathVariable Long addrId, @RequestBody Address payload) {
        var addr = addressService.getById(addrId);
        if (addr == null || !addr.getUserId().equals(userId)) return ApiResponse.error("地址不存在");
        if (payload.getDetail() != null) addr.setDetail(payload.getDetail());
        if (payload.getCommunityId() != null) addr.setCommunityId(payload.getCommunityId());
        try {
            addressService.updateById(addr);
        } catch (DataIntegrityViolationException ex) {
            return ApiResponse.error("社区不存在，请检查 ID");
        }
        return ApiResponse.ok("地址已更新");
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/{addrId}")
    @PreAuthorize("#userId.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse delete(@PathVariable Long userId, @PathVariable Long addrId) {
        var addr = addressService.getById(addrId);
        if (addr == null || !addr.getUserId().equals(userId)) return ApiResponse.error("地址不存在");
        addressService.removeById(addrId);
        // 如果删的是默认地址，清空默认
        String key = defaultKey(userId);
        String v = stringRedisTemplate.opsForValue().get(key);
        if (v != null && v.equals(String.valueOf(addrId))) stringRedisTemplate.delete(key);
        return ApiResponse.ok("地址已删除");
    }

    /**
     * 设为默认（存入 Redis；无需表字段）
     */
    @PutMapping("/{addrId}/default")
    @PreAuthorize("#userId.toString() == authentication.name or hasRole('ADMIN')")
    public ApiResponse setDefault(@PathVariable Long userId, @PathVariable Long addrId) {
        var addr = addressService.getById(addrId);
        if (addr == null || !addr.getUserId().equals(userId)) return ApiResponse.error("地址不存在");
        stringRedisTemplate.opsForValue().set(defaultKey(userId), String.valueOf(addrId));
        return ApiResponse.ok("已设为默认地址");
    }
}
