package com.community.leader.controller;

import com.community.common.util.ApiResponse;
import com.community.leader.entity.Community;
import com.community.leader.entity.Leader;
import com.community.leader.service.LeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/communities")
@RequiredArgsConstructor
public class CommunityController {
    private final LeaderService leaderService;

    @GetMapping
    public ApiResponse list() {
        var all = leaderService.listAllCommunities();
        for (var c : all) {
            var leader = leaderService.lambdaQuery()
                    .eq(com.community.leader.entity.Leader::getCommunityId, c.getId())
                    .eq(com.community.leader.entity.Leader::getStatus, "APPROVED")
                    .last("limit 1")
                    .one();
            if (leader != null) {
                c.setLeaderId(leader.getId());
                c.setLeaderUserId(leader.getUserId());
            }
        }
        return ApiResponse.ok().data("communities", all);
    }

    /**
     * 新增：按 ID 获取社区（订单根据 communityId 取 leaderId 用）
     */
    @GetMapping("/{id}")
    public ApiResponse getById(@PathVariable Long id) {
        var all = leaderService.listAllCommunities();
        var c = all.stream().filter(x -> id.equals(x.getId())).findFirst().orElse(null);
        if (c == null) return ApiResponse.error("社区不存在");

        var leader = leaderService.lambdaQuery()
                .eq(Leader::getCommunityId, id)
                .eq(Leader::getStatus, "APPROVED")
                .last("limit 1")
                .one();
        if (leader != null) {
            c.setLeaderId(leader.getId());
            c.setLeaderUserId(leader.getUserId());
        }
        return ApiResponse.ok()
                .data("community", c)
                .data("leaderId", leader == null ? null : leader.getId())
                .data("leaderUserId", leader == null ? null : leader.getUserId());
    }

    /**
     * 新增：根据经纬度返回最近的社区（默认 5 条）
     */
    @GetMapping("/nearby")
    public ApiResponse nearby(@RequestParam double lat,
                              @RequestParam double lng,
                              @RequestParam(defaultValue = "5") int limit) {
        List<Community> all = leaderService.listAllCommunities();
        List<Community> result = all.stream()
                .filter(c -> c.getLat() != null && c.getLng() != null)
                .sorted(Comparator.comparingDouble(c -> haversine(lat, lng, c.getLat(), c.getLng())))
                .limit(Math.max(1, limit))
                .toList();
        return ApiResponse.ok().data("communities", result);
    }

    // 简易 Haversine 距离（km）
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // 地球半径（km）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    // 示例占位：团长统计
    @GetMapping("/summary/{leaderId}")
    public ApiResponse summary(@PathVariable Long leaderId) {
        return ApiResponse.ok().data("summary", List.of(
                java.util.Map.of("productName", "示例苹果", "totalQuantity", 120),
                java.util.Map.of("productName", "土豆", "totalQuantity", 86)
        ));
    }
}
