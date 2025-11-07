package com.community.order.enhance;

import com.community.common.util.ApiResponse;
import com.community.order.entity.Order;
import com.community.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderMiscController {

    private final OrderService orderService;

    /**
     * 设置订单备注
     */
    @PostMapping("/{id}/remark")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse setRemark(@PathVariable Long id, @RequestParam String remark) {
        Order o = orderService.getById(id);
        if (o == null) return ApiResponse.error(404, "订单不存在");
        o.setRemark(remark);
        o.setUpdatedAt(new Date());
        orderService.updateById(o);
        return ApiResponse.ok("备注已更新").data("remark", remark);
    }

    /**
     * 设置发票信息并返回电子发票链接（示例生成）
     */
    @PostMapping("/{id}/invoice")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse setInvoice(@PathVariable Long id,
                                  @RequestParam String title,
                                  @RequestParam String taxNo,
                                  @RequestParam(defaultValue = "normal") String type) {
        Order o = orderService.getById(id);
        if (o == null) return ApiResponse.error(404, "订单不存在");

        o.setInvoiceTitle(title);
        o.setInvoiceTaxNo(taxNo);
        o.setInvoiceType(type);
        // 这里演示生成一个可访问地址，你可以替换成真实的发票生成逻辑
        o.setInvoiceUrl("/invoices/" + id + ".pdf");

        o.setUpdatedAt(new Date());
        orderService.updateById(o);

        return ApiResponse.ok("发票信息已更新").data("invoiceUrl", o.getInvoiceUrl());
    }
}
