package com.community.leader.controller;

import com.community.common.util.ApiResponse;
import com.community.leader.entity.Leader;
import com.community.leader.service.LeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/leaders")
@RequiredArgsConstructor
public class LeaderController {
    private final LeaderService leaderService;

    @PostMapping("/apply")
    public ApiResponse apply(@RequestParam Long userId, @RequestParam String communityName, @RequestParam String communityAddr) {
        Leader l = leaderService.applyLeader(userId, communityName, communityAddr);
        return ApiResponse.ok("团长申请已提交").data("leaderId", l.getId());
    }

    @GetMapping("/pending")
    public ApiResponse pending() {
        List<Leader> list = leaderService.listPendingApplications();
        return ApiResponse.ok().data("pendingLeaders", list);
    }

    @PutMapping("/{leaderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','INTERNAL')")
    public ApiResponse updateStatus(@PathVariable Long leaderId, @RequestParam String status) {
        leaderService.updateLeaderStatus(leaderId, status);
        return ApiResponse.ok("状态已更新为 " + status);
    }
}
