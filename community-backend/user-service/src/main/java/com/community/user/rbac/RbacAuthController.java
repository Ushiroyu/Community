package com.community.user.rbac;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/rbac")
@RequiredArgsConstructor
public class RbacAuthController {

    private final UserRoleMapper urMapper;
    private final RolePermissionMapper rpMapper;
    private final PermissionMapper permMapper;

    @GetMapping("/has-perm")
    public Boolean hasPermission(@RequestParam Long userId, @RequestParam String perm) {
        List<UserRole> urs = urMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (urs.isEmpty()) return false;
        Set<Long> roleIds = new HashSet<>();
        for (UserRole ur : urs) roleIds.add(ur.getRoleId());
        List<RolePermission> rps = rpMapper.selectList(new LambdaQueryWrapper<RolePermission>().in(RolePermission::getRoleId, roleIds));
        if (rps.isEmpty()) return false;
        Set<Long> permIds = new HashSet<>();
        for (RolePermission rp : rps) permIds.add(rp.getPermissionId());
        List<Permission> perms = permMapper.selectList(new LambdaQueryWrapper<Permission>().in(Permission::getId, permIds));
        for (Permission p : perms) {
            if (Boolean.TRUE.equals(p.getEnabled()) && perm.equals(p.getCode())) return true;
        }
        return false;
    }
}
