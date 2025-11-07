package com.community.leader.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.leader.entity.Community;
import com.community.leader.entity.Leader;
import com.community.leader.mapper.CommunityMapper;
import com.community.leader.mapper.LeaderMapper;
import com.community.leader.service.LeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderServiceImpl extends ServiceImpl<LeaderMapper, Leader> implements LeaderService {

    private final StringRedisTemplate redis;
    private final CommunityMapper communityMapper;
    private static final String GEO_KEY = "communities:geo";

    @Override
    public Leader applyLeader(Long userId, String communityName, String communityAddr) {
        // 防重复申请
        long exists = this.lambdaQuery().eq(Leader::getUserId, userId).count();
        if (exists > 0) {
            throw new RuntimeException("该用户已提交过团长申请");
        }
        // 先创建社区（未设地理位置，后续设置）
        Community c = new Community();
        c.setName(communityName);
        c.setAddress(communityAddr);
        c.setLeaderId(null);
        communityMapper.insert(c);

        // 建立团长申请
        Leader leader = new Leader();
        leader.setUserId(userId);
        leader.setCommunityId(c.getId());
        leader.setStatus("PENDING");
        leader.setCreateTime(new Date());
        leader.setUpdateTime(new Date());
        this.save(leader);

        return leader;
    }

    @Override
    public List<Leader> listPendingApplications() {
        return this.lambdaQuery().eq(Leader::getStatus, "PENDING").list();
    }

    @Override
    public List<Community> listAllCommunities() {
        return communityMapper.selectList(null);
    }

    @Override
    public void setCommunityLocation(Long communityId, Double lat, Double lng) {
        Community c = communityMapper.selectById(communityId);
        if (c == null) throw new RuntimeException("社区不存在");
        c.setLat(lat);
        c.setLng(lng);
        communityMapper.updateById(c);
        // 写入 GEO：name 用 id 字符串
        redis.opsForGeo().add(GEO_KEY, new org.springframework.data.geo.Point(lng, lat), String.valueOf(communityId));
    }

    @Override
    public List<Community> nearbyCommunities(Double lat, Double lng, double radiusKm, int limit) {
        var results = redis.opsForGeo().radius(
                GEO_KEY,
                new org.springframework.data.geo.Circle(
                        new org.springframework.data.geo.Point(lng, lat),
                        new org.springframework.data.geo.Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS)
                )
        );
        List<Community> list = new ArrayList<>();
        if (results == null) return list;
        for (var r : results) {
            Long id = Long.valueOf(r.getContent().getName());
            Community c = communityMapper.selectById(id);
            if (c != null) list.add(c);
            if (list.size() >= limit) break;
        }
        return list;
    }

    @Override
    public void updateLeaderStatus(Long leaderId, String status) {
        Leader l = this.getById(leaderId);
        if (l == null) throw new RuntimeException("团长申请不存在");
        l.setStatus(status);
        l.setUpdateTime(new Date());
        this.updateById(l);
        // 通过时把社区绑定 leaderId
        if ("APPROVED".equalsIgnoreCase(status)) {
            Community c = communityMapper.selectById(l.getCommunityId());
            if (c != null) {
                c.setLeaderId(l.getId());
                communityMapper.updateById(c);
            }
        }
    }
}
