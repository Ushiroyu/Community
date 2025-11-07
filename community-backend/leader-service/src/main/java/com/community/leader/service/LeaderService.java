package com.community.leader.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.leader.entity.Leader;
import com.community.leader.entity.Community;

import java.util.List;

public interface LeaderService extends IService<Leader> {
    Leader applyLeader(Long userId, String communityName, String communityAddr);

    List<Leader> listPendingApplications();

    List<Community> listAllCommunities();

    void setCommunityLocation(Long communityId, Double lat, Double lng);

    java.util.List<Community> nearbyCommunities(Double lat, Double lng, double radiusKm, int limit);

    void updateLeaderStatus(Long leaderId, String status);
}
