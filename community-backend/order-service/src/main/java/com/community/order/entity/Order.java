package com.community.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long leaderId;
    private Long supplierId;
    private Long addressId;

    @TableField("total_amount")
    private BigDecimal amount;
    private String status; // CREATED / PAID / SHIPPED / DELIVERED / CANCELED

    @TableField("tracking_no")
    private String trackingNo; // tracking number
    private String remark;
    @TableField("invoice_title")
    private String invoiceTitle;
    @TableField("invoice_tax_no")
    private String invoiceTaxNo;
    @TableField("invoice_type")
    private String invoiceType;
    @TableField("invoice_url")
    private String invoiceUrl;

    @TableField("pay_time")
    private Date payTime;
    @TableField("create_time")
    private Date createdAt;
    @TableField("update_time")
    private Date updatedAt;
}

