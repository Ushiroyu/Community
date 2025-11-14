-- schema.sql
-- Extracted from ../community_schema_mysql.sql; includes schema/DDL only.
-- Generated via automated split for easier migrations.

-- Community Platform - MySQL DDL & Seed Data
-- 完整对接本仓库多模块（user/product/order/leader/supplier/admin），并附带可视化演示用的大量测试数据。
-- 适配 MyBatis-Plus 与实体：User/Address/Community/Leader/Supplier/Category/Product/Order/OrderItem/ShipmentEvent/RBAC/EventConsumeLog
-- 运行顺序：直接执行整份脚本即可（MySQL 8.0+）。
-- 默认管理员与演示账号密码统一为明文 "password" 的 BCrypt：
-- $2b$10$S5G4wqPbNRwCbV6cglGG2Ow7qQlU7ZpIoPVXP4w/ES.Buy5YgZf3K

SET NAMES utf8mb4;
SET time_zone = '+08:00';

-- 可按需切换库名
CREATE DATABASE IF NOT EXISTS `community` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `community`;

SET FOREIGN_KEY_CHECKS=0;

/* =============================
 * 1) Drop Tables (child -> parent)
 * ============================= */
DROP TABLE IF EXISTS `shipment_event`;
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `supplier`;
DROP TABLE IF EXISTS `leader`;
DROP TABLE IF EXISTS `address`;
DROP TABLE IF EXISTS `community`;
-- RBAC
DROP TABLE IF EXISTS `role_permission`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `permission`;
DROP TABLE IF EXISTS `role`;
-- Admin event log
DROP TABLE IF EXISTS `event_consume_log`;
-- Users last because of FKs above
DROP TABLE IF EXISTS `user`;
-- helper
DROP TABLE IF EXISTS `numbers`;

/* =============================
 * 2) Core Tables
 * ============================= */
CREATE TABLE `user` (
  `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username`    VARCHAR(50)  NOT NULL UNIQUE,
  `password`    VARCHAR(255) NOT NULL,
  `role`        ENUM('USER','LEADER','SUPPLIER','ADMIN') NOT NULL DEFAULT 'USER',
  `nickname`    VARCHAR(50)  NULL,
  `phone`       VARCHAR(20)  NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `community` (
  `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL,
  `address`     VARCHAR(255) NULL,
  `lat`         DOUBLE NULL,
  `lng`         DOUBLE NULL,
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `address` (
  `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`      BIGINT NOT NULL,
  `community_id` BIGINT NULL,
  `detail`       VARCHAR(255) NOT NULL,
  `is_default`   TINYINT(1) NOT NULL DEFAULT 0,
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_address_user`      FOREIGN KEY (`user_id`)      REFERENCES `user`(`id`)      ON DELETE CASCADE,
  CONSTRAINT `fk_address_community` FOREIGN KEY (`community_id`) REFERENCES `community`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_address_user` ON `address`(`user_id`);

CREATE TABLE `leader` (
  `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`      BIGINT NOT NULL,
  `community_id` BIGINT NOT NULL,
  `real_name`    VARCHAR(50)  NULL,
  `phone`        VARCHAR(20)  NULL,
  `status`       ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_leader_user` (`user_id`),
  CONSTRAINT `fk_leader_user`      FOREIGN KEY (`user_id`)      REFERENCES `user`(`id`)      ON DELETE CASCADE,
  CONSTRAINT `fk_leader_community` FOREIGN KEY (`community_id`) REFERENCES `community`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_leader_status` ON `leader`(`status`);

CREATE TABLE `supplier` (
  `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`      BIGINT NOT NULL,
  `company_name` VARCHAR(120) NOT NULL,
  `status`       ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_supplier_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_supplier_status` ON `supplier`(`status`);

CREATE TABLE `category` (
  `id`        BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name`      VARCHAR(100) NOT NULL,
  `parent_id` BIGINT NULL,
  CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `category`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `product` (
  `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name`         VARCHAR(200) NOT NULL,
  `category_id`  BIGINT NOT NULL,
  `price`        DECIMAL(10,2) NOT NULL,
  `stock`        INT NOT NULL DEFAULT 0,
  `version`      INT NOT NULL DEFAULT 0,
  `supplier_id`  BIGINT NOT NULL,
  `status`       TINYINT(1) NOT NULL DEFAULT 1,
  `approved`     TINYINT(1) NOT NULL DEFAULT 0,
  `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_product_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier`(`id`)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_product_category` ON `product`(`category_id`);
CREATE INDEX `idx_product_supplier` ON `product`(`supplier_id`);
CREATE INDEX `idx_product_status`   ON `product`(`status`, `approved`);

CREATE TABLE `order` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id`       BIGINT NOT NULL,
  `leader_id`     BIGINT NULL,
  `supplier_id`   BIGINT NULL,
  `address_id`    BIGINT NULL,
  `status`        ENUM('CREATED','PAID','SHIPPED','DELIVERED','CANCELED','REFUNDED') NOT NULL DEFAULT 'CREATED',
  `total_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0,
  `payment_method` ENUM('NONE','WECHAT','ALIPAY','CARD','CASH') NOT NULL DEFAULT 'NONE',
  `tracking_no`   VARCHAR(64) NULL,
  `remark`        VARCHAR(255) NULL,
  `invoice_title`   VARCHAR(200) NULL,
  `invoice_tax_no`  VARCHAR(50)  NULL,
  `invoice_type`    VARCHAR(50)  NULL,
  `invoice_url`     VARCHAR(255) NULL,
  `pay_time`      DATETIME NULL,
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_order_user`     FOREIGN KEY (`user_id`)     REFERENCES `user`(`id`),
  CONSTRAINT `fk_order_leader`   FOREIGN KEY (`leader_id`)   REFERENCES `leader`(`id`),
  CONSTRAINT `fk_order_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier`(`id`),
  CONSTRAINT `fk_order_address`  FOREIGN KEY (`address_id`)  REFERENCES `address`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_order_user`     ON `order`(`user_id`);
CREATE INDEX `idx_order_leader`   ON `order`(`leader_id`);
CREATE INDEX `idx_order_supplier` ON `order`(`supplier_id`);
CREATE INDEX `idx_order_status`   ON `order`(`status`);
CREATE INDEX `idx_order_created`  ON `order`(`create_time`);
CREATE INDEX `idx_order_leader_created` ON `order`(`leader_id`, `create_time`);

CREATE TABLE `order_item` (
  `id`         BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id`   BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `quantity`   INT NOT NULL,
  `price`      DECIMAL(10,2) NOT NULL,
  CONSTRAINT `fk_orderitem_order`   FOREIGN KEY (`order_id`)   REFERENCES `order`(`id`)   ON DELETE CASCADE,
  CONSTRAINT `fk_orderitem_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_order_item_order`   ON `order_item`(`order_id`);
CREATE INDEX `idx_order_item_product` ON `order_item`(`product_id`);

CREATE TABLE `shipment_event` (
  `id`        BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id`  BIGINT NOT NULL,
  `status`    VARCHAR(30) NOT NULL, -- SHIPPED / IN_TRANSIT / DELIVERED / ...
  `location`  VARCHAR(200) NULL,
  `remark`    VARCHAR(255) NULL,
  `event_time` DATETIME NOT NULL,
  CONSTRAINT `fk_ship_order` FOREIGN KEY (`order_id`) REFERENCES `order`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_ship_event_order_time` ON `shipment_event`(`order_id`, `event_time`);

-- 事务外发事件表（Order Outbox）
CREATE TABLE IF NOT EXISTS `outbox_event` (
  `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
  `event_type`    VARCHAR(100) NOT NULL,
  `aggregate_type` VARCHAR(50) NOT NULL,
  `aggregate_id`  BIGINT NOT NULL,
  `payload`       JSON NOT NULL,
  `status`        VARCHAR(20) NOT NULL DEFAULT 'NEW',
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_outbox_status_time` ON `outbox_event`(`status`, `created_at`);

-- 商品 SKU 表（Addons）
CREATE TABLE IF NOT EXISTS `product_sku` (
  `id`         BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `attrs`      JSON NULL,
  `stock`      INT NOT NULL DEFAULT 0,
  `price`      DECIMAL(10,2) NOT NULL,
  CONSTRAINT `fk_sku_product` FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `idx_sku_product` ON `product_sku`(`product_id`);

/* =============================
 * 3) RBAC Tables (user-service-addons)
 * ============================= */
CREATE TABLE `role` (
  `id`      BIGINT PRIMARY KEY AUTO_INCREMENT,
  `code`    VARCHAR(50)  NOT NULL UNIQUE,
  `name`    VARCHAR(100) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `permission` (
  `id`      BIGINT PRIMARY KEY AUTO_INCREMENT,
  `code`    VARCHAR(100) NOT NULL UNIQUE,
  `name`    VARCHAR(200) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_role` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `role_permission` (
  `role_id`      BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`),
  CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `permission`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/* =============================
 * 4) Admin Event Log
 * ============================= */
CREATE TABLE `event_consume_log` (
  `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
  `consumer`    VARCHAR(100) NOT NULL,
  `message_id`  VARCHAR(100) NOT NULL,
  `event_type`  VARCHAR(100) NOT NULL,
  `status`      VARCHAR(20)  NOT NULL,
  `retry_count` INT NOT NULL DEFAULT 0,
  `updated_at`  DATETIME NOT NULL,
  `created_at`  DATETIME NOT NULL,
  UNIQUE KEY `uk_consumer_msg` (`consumer`,`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS=1;

/* =============================
 * 5) Helper numbers (1..1000)
 * ============================= */
CREATE TABLE `numbers` (`n` INT PRIMARY KEY);
INSERT INTO `numbers`(`n`)
SELECT ones.n + tens.n*10 + hundreds.n*100 AS n
FROM (
  SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
  SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
  SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) hundreds
WHERE (ones.n + tens.n*10 + hundreds.n*100) BETWEEN 1 AND 1000;

/* =============================
 * 7) 性能与统计相关索引/视图（可选）
 * ============================= */
-- 顶部商品与团长统计的常用索引已在上方创建（order_item.product_id、order.create_time、order.leader_id 等）。
-- 如需固定报表，也可创建物化表/定时任务，这里仅示例普通视图：
DROP VIEW IF EXISTS `v_order_item_summary`;
CREATE VIEW `v_order_item_summary` AS
SELECT o.id AS order_id, o.leader_id, o.supplier_id, o.create_time, oi.product_id, oi.quantity, oi.price,
       (oi.quantity*oi.price) AS amount
FROM `order` o JOIN `order_item` oi ON oi.order_id = o.id;

/* =============================
 * 8) 快速检查（可在客户端随取随用）
 * ============================= */
-- SELECT COUNT(*) AS users, SUM(role='USER') AS users_user FROM `user`;
-- SELECT COUNT(*) AS products FROM `product`;
-- SELECT COUNT(*) AS orders, SUM(status='PAID') AS paid, SUM(status='SHIPPED') AS shipped, SUM(status='DELIVERED') AS delivered FROM `order`;
-- SELECT * FROM `shipment_event` ORDER BY event_time DESC LIMIT 20;

-- 完毕
