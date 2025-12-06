package com.community.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.util.ApiResponse;
import com.community.order.config.RabbitConfig;
import com.community.order.entity.Order;
import com.community.order.entity.OrderItem;
import com.community.order.entity.ShipmentEvent;
import com.community.order.messaging.OrderEventPublisher;
import com.community.order.mapper.OrderItemMapper;
import com.community.order.service.OrderService;
import com.community.order.service.ShipmentService;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ShipmentService shipmentService;
    private final OrderItemMapper orderItemMapper;
    private final RestClient productClient;
    private final RestClient userClient;
    private final RestClient leaderClient;
    private final ObjectProvider<RabbitTemplate> rabbitProvider;
    private final OrderEventPublisher eventPublisher;

    @Autowired
    public OrderController(OrderService orderService,
                           ShipmentService shipmentService,
                           OrderItemMapper orderItemMapper,
                           @Qualifier("productClient") RestClient productClient,
                           @Qualifier("userClient") RestClient userClient,
                           @Qualifier("leaderClient") RestClient leaderClient,
                           ObjectProvider<RabbitTemplate> rabbitProvider,
                           OrderEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.shipmentService = shipmentService;
        this.orderItemMapper = orderItemMapper;
        this.productClient = productClient;
        this.userClient = userClient;
        this.leaderClient = leaderClient;
        this.rabbitProvider = rabbitProvider;
        this.eventPublisher = eventPublisher;
    }

    // 创建订单：商品信息、Redis 预占库存、创建订单和订单项、投递延时消息
    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse create(@RequestParam Long userId,
                              @RequestParam(required = false) Long leaderId,
                              @RequestParam Long productId,
                              @RequestParam Integer quantity,
                              @RequestParam(required = false) Long addressId) {
        if (quantity == null || quantity <= 0) return ApiResponse.error("数量非法");

        // 自动根据 address -> community -> leaderUserId 解析团长
        Long resolvedLeaderId = leaderId;
        if (resolvedLeaderId == null && addressId != null) {
            Long communityId = fetchCommunityIdFromAddress(userId, addressId);
            if (communityId != null) {
                resolvedLeaderId = fetchLeaderUserIdByCommunity(communityId);
            }
        }
        if (resolvedLeaderId == null) {
            return ApiResponse.error("该地址没有绑定社区或团长");
        }

        ApiResponse prodResp = productClient.get()
                .uri(u -> u.path("/products/{id}").build(productId))
                .retrieve()
                .body(ApiResponse.class);
        if (prodResp == null || prodResp.getCode() != 0) return ApiResponse.error("查询商品失败");

        Object productObj = prodResp.getOrDefault("product", prodResp.get("item"));
        if (!(productObj instanceof Map<?, ?> product)) return ApiResponse.error("商品数据缺失");

        Long supplierId = toLong(product.get("supplierId"));
        BigDecimal price = toBigDecimal(product.get("price"));
        if (supplierId == null) return ApiResponse.error("商品未绑定供应商");
        if (price == null) price = BigDecimal.ZERO;

        // 1) Redis 预占库存
        ApiResponse reserve = productClient.post()
                .uri(u -> u.path("/products/{id}/reserve").queryParam("qty", quantity).build(productId))
                .retrieve().body(ApiResponse.class);
        if (reserve == null || reserve.getCode() != 0) return ApiResponse.error("库存不足，请调整数量");

        // 2) 创建订单 + 订单项（单商品简化）
        Order o = new Order();
        o.setUserId(userId);
        // leaderId 这里保存团长用户ID，后续按用户ID查询
        o.setLeaderId(resolvedLeaderId);
        o.setSupplierId(supplierId);
        o.setAddressId(addressId);
        o.setAmount(price.multiply(BigDecimal.valueOf(quantity)));
        o.setStatus("CREATED");
        o.setCreatedAt(new Date());
        o.setUpdatedAt(new Date());

        OrderItem it = new OrderItem();
        it.setProductId(productId);
        it.setQuantity(quantity);
        it.setPrice(price);
        orderService.createOrderWithItems(o, List.of(it));

        // 3) 投递延时消息（超时未支付自动取消并回补库存）
        rabbitProvider.ifAvailable(r -> {
            MessagePostProcessor mpp = msg -> {
                msg.getMessageProperties().setExpiration(String.valueOf(15 * 60 * 1000));
                msg.getMessageProperties().setMessageId(String.valueOf(o.getId()));
                return msg;
            };
            Map<String, Object> payload = Map.of("orderId", o.getId(), "userId", userId);
            try {
                r.convertAndSend("", RabbitConfig.ORDER_DELAY_QUEUE, payload, mpp);
            } catch (Exception ex) {
                // 忽略本地环境无 RabbitMQ 的异常，不影响下单流程
            }
        });

        return ApiResponse.ok("下单成功").data("orderId", o.getId());
    }

    // 支付：更新状态，发布 order.paid 事件（包含订单项 productId/quantity）
    @PostMapping("/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse pay(@PathVariable Long id) {
        Order o = orderService.getById(id);
        if (o == null) return ApiResponse.error("订单不存在");
        if ("PAID".equals(o.getStatus())) return ApiResponse.ok("已支付");
        if (!"CREATED".equals(o.getStatus())) return ApiResponse.error("状态不可支付");
        o.setStatus("PAID");
        o.setPayTime(new Date());
        o.setUpdatedAt(new Date());
        orderService.updateById(o);

        // 查询订单项并发布
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        List<OrderEventPublisher.OrderPaidEvent.Item> list = new ArrayList<>();
        for (OrderItem it : items)
            list.add(new OrderEventPublisher.OrderPaidEvent.Item(it.getProductId(), it.getQuantity()));
        eventPublisher.publishOrderPaid(new OrderEventPublisher.OrderPaidEvent(o.getId(), list));

        return ApiResponse.ok("已支付");
    }

    // 发货：校验权限并记录事件
    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    public ApiResponse ship(@PathVariable Long id,
                            @RequestParam(required = false) String trackingNo) {
        Order order = orderService.getById(id);
        if (order == null) return ApiResponse.error("订单不存在");
        if (!isAdmin()) {
            Long uid = currentUserIdOrNull();
            if (uid == null || !uid.equals(order.getSupplierId())) return ApiResponse.error("无权限");
        }
        if ("SHIPPED".equals(order.getStatus())) return ApiResponse.ok("已发货");
        if (!"PAID".equals(order.getStatus())) return ApiResponse.error("订单状态不是待发货");
        order.setStatus("SHIPPED");
        if (StringUtils.hasText(trackingNo)) order.setTrackingNo(trackingNo);
        order.setUpdatedAt(new Date());
        orderService.updateById(order);

        ShipmentEvent se = new ShipmentEvent();
        se.setOrderId(order.getId());
        se.setEventTime(new Date());
        se.setStatus("SHIPPED");
        se.setRemark("已发货");
        shipmentService.addEvent(se);
        return ApiResponse.ok("已发货");
    }

    // 确认收货
    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse confirm(@PathVariable Long id) {
        Order o = orderService.getById(id);
        if (o == null) return ApiResponse.error("订单不存在");
        if ("DELIVERED".equals(o.getStatus())) return ApiResponse.ok("已收货");
        if (!"SHIPPED".equals(o.getStatus())) return ApiResponse.error("订单状态不是待收货");
        o.setStatus("DELIVERED");
        o.setUpdatedAt(new Date());
        orderService.updateById(o);
        return ApiResponse.ok("确认收货成功");
    }

    // 供应商待发货列表（=PAID）
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    public ApiResponse pending(@RequestParam Long supplierId) {
        if (!isAdmin()) {
            Long uid = currentUserIdOrNull();
            if (uid == null || !uid.equals(supplierId)) return ApiResponse.error("无权限");
        }
        List<Order> orders = orderService.list(new LambdaQueryWrapper<Order>()
                .eq(Order::getSupplierId, supplierId)
                .eq(Order::getStatus, "PAID")
                .orderByDesc(Order::getCreatedAt));
        return ApiResponse.ok().data("orders", orders);
    }

    // ===== helper =====
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth instanceof JwtAuthenticationToken jwt) {
            Map<String, Object> claims = jwt.getToken().getClaims();
            Object v = claims.getOrDefault("user_id",
                    claims.getOrDefault("uid", claims.getOrDefault("sub", claims.get("userId"))));
            return toLong(v);
        }
        try {
            return Long.valueOf(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取用户地址的 communityId（不存在或跨用户时返回 null）
     */
    private Long fetchCommunityIdFromAddress(Long userId, Long addressId) {
        try {
            ApiResponse addrResp = userClient.get()
                    .uri(u -> u.path("/users/{uid}/addresses/{aid}").build(userId, addressId))
                    .retrieve()
                    .body(ApiResponse.class);
            if (addrResp == null || addrResp.getCode() != 0) return null;
            Object addrObj = addrResp.getOrDefault("address", addrResp.get("data"));
            if (addrObj instanceof Map<?, ?> map) {
                return toLong(map.get("communityId"));
            }
            Map<String, Object> data = addrResp.getData();
            if (data != null && data.get("address") instanceof Map<?, ?> map2) {
                return toLong(map2.get("communityId"));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 根据 communityId 查询对应的团长用户ID（优先 leaderUserId）
     */
    private Long fetchLeaderUserIdByCommunity(Long communityId) {
        try {
            ApiResponse resp = leaderClient.get()
                    .uri(u -> u.path("/communities/{id}").build(communityId))
                    .retrieve()
                    .body(ApiResponse.class);
            if (resp == null || resp.getCode() != 0) return null;
            Object leaderUserId = resp.get("leaderUserId");
            if (leaderUserId == null) leaderUserId = resp.get("leaderId");
            Object communityObj = resp.get("community");
            if (leaderUserId == null && communityObj instanceof Map<?, ?> communityMap) {
                leaderUserId = communityMap.get("leaderUserId");
                if (leaderUserId == null) leaderUserId = communityMap.get("leaderId");
            }
            Map<String, Object> data = resp.getData();
            if (leaderUserId == null && data != null) {
                if (data.get("leaderUserId") != null) leaderUserId = data.get("leaderUserId");
                else leaderUserId = data.get("leaderId");
            }
            return toLong(leaderUserId);
        } catch (Exception ignored) {
            return null;
        }
    }

    // 按团长查询订单（LEADER/ADMIN）
    @GetMapping("/by-leader")
    @PreAuthorize("hasAnyRole('LEADER','ADMIN')")
    public ApiResponse byLeader(@RequestParam(required = false) Long leaderId,
                                @RequestParam(required = false) String status,
                                @RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "10") long size) {
        Long uid = currentUserIdOrNull();
        if (!isAdmin()) {
            if (uid == null) return ApiResponse.error("无权限");
            if (leaderId == null) leaderId = uid;
            if (!uid.equals(leaderId)) return ApiResponse.error("无权限");
        }
        if (leaderId == null) return ApiResponse.error("缺少团长ID");
        var qw = new LambdaQueryWrapper<Order>()
                .eq(Order::getLeaderId, leaderId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null && !status.isBlank()) qw.eq(Order::getStatus, status);
        var p = orderService.page(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, size), qw);
        return ApiResponse.ok().data("list", p.getRecords()).data("total", p.getTotal());
    }

    // 管理员分页
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse all(@RequestParam(defaultValue = "1") long page,
                           @RequestParam(defaultValue = "10") long size,
                           @RequestParam(required = false) String status) {
        var qw = new LambdaQueryWrapper<Order>().orderByDesc(Order::getCreatedAt);
        if (status != null && !status.isBlank()) qw.eq(Order::getStatus, status);
        var p = orderService.page(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, size), qw);
        return ApiResponse.ok().data("list", p.getRecords()).data("total", p.getTotal());
    }
}
