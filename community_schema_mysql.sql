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
 * 6) Seed Data
 * ============================= */
-- 6.1 Roles & Permissions
INSERT INTO `role` (`code`,`name`,`enabled`) VALUES
 ('ADMIN','管理员',1),
 ('LEADER','团长',1),
 ('SUPPLIER','供应商',1),
 ('USER','普通用户',1);

INSERT INTO `permission` (`code`,`name`,`enabled`) VALUES
 ('admin:*','后台管理权限',1),
 ('leader:approve','团长审核',1),
 ('product:approve','商品审核',1),
 ('order:ship','发货处理',1),
 ('stats:view','查看统计',1);

-- 6.2 Users
-- 演示统一密码明文为 password（BCrypt 见文件头注释）
SET @pwd := '$2b$10$S5G4wqPbNRwCbV6cglGG2Ow7qQlU7ZpIoPVXP4w/ES.Buy5YgZf3K';

-- 管理员 1 个
INSERT INTO `user` (`username`,`password`,`role`,`nickname`,`phone`)
VALUES ('admin', @pwd, 'ADMIN','平台管理员','0900-000-000');

-- 供应商账号 10 个：supplier01..10
INSERT INTO `user` (`username`,`password`,`role`)
SELECT CONCAT('supplier', LPAD(n,2,'0')), @pwd, 'SUPPLIER' FROM (
  SELECT n FROM numbers WHERE n BETWEEN 1 AND 10
) t;

-- 团长账号 10 个：leader01..10
INSERT INTO `user` (`username`,`password`,`role`)
SELECT CONCAT('leader', LPAD(n,2,'0')), @pwd, 'LEADER' FROM (
  SELECT n FROM numbers WHERE n BETWEEN 1 AND 10
) t;

-- 普通用户 80 个：user001..user080
INSERT INTO `user` (`username`,`password`,`role`)
SELECT CONCAT('user', LPAD(n,3,'0')), @pwd, 'USER' FROM (
  SELECT n FROM numbers WHERE n BETWEEN 1 AND 80
) t;

-- 建立 RBAC user_role 基础映射（与 user.role 一致）
INSERT INTO `user_role` (`user_id`,`role_id`)
SELECT u.id, r.id FROM `user` u JOIN `role` r ON r.code = u.role;

-- 管理员赋全权限
INSERT INTO `role_permission` (`role_id`,`permission_id`)
SELECT r.id, p.id FROM `role` r CROSS JOIN `permission` p WHERE r.code='ADMIN';

-- 供应商权限：发货/商品审核/查看统计
INSERT INTO `role_permission` (`role_id`,`permission_id`)
SELECT r.id, p.id FROM `role` r JOIN `permission` p ON p.code IN ('order:ship','product:approve','stats:view') WHERE r.code='SUPPLIER';

-- 团长权限：查看统计
INSERT INTO `role_permission` (`role_id`,`permission_id`)
SELECT r.id, p.id FROM `role` r JOIN `permission` p ON p.code IN ('stats:view') WHERE r.code='LEADER';

-- 6.3 Communities（20 个，台北周边随机经纬度）
INSERT INTO `community` (`name`,`address`,`lat`,`lng`)
SELECT CONCAT('社区', LPAD(n,2,'0')),
       CONCAT('台北市示例路 ', n, ' 号'),
       25.02 + (RAND()*0.2),
       121.45 + (RAND()*0.2)
FROM (SELECT n FROM numbers WHERE n BETWEEN 1 AND 20) t;

-- 6.4 Leaders（10 个，对应 leader 用户，全部 APPROVED）
SET @leader_min_user_id := (SELECT MIN(id) FROM `user` WHERE `role`='LEADER');
SET @community_min_id   := (SELECT MIN(id) FROM `community`);
INSERT INTO `leader` (`user_id`,`community_id`,`real_name`,`phone`,`status`)
SELECT u.id,
       (@community_min_id - 1) + ((ROW_NUMBER() OVER (ORDER BY u.id) - 1) % (SELECT COUNT(*) FROM community) + 1),
       CONCAT('团长', LPAD(ROW_NUMBER() OVER (ORDER BY u.id),2,'0')),
       CONCAT('09', LPAD(ROUND(RAND()*99999999),8,'0')),
       'APPROVED'
FROM `user` u WHERE u.role='LEADER';

-- 6.5 Suppliers（10 个，对应 supplier 用户，全部 APPROVED）
INSERT INTO `supplier` (`user_id`,`company_name`,`status`)
SELECT u.id, CONCAT('供应商公司', LPAD(ROW_NUMBER() OVER (ORDER BY u.id),2,'0')), 'APPROVED'
FROM `user` u WHERE u.role='SUPPLIER';

-- 6.6 Categories（15 个，简单一级类目）
INSERT INTO `category` (`name`) VALUES
 ('蔬菜'),('水果'),('肉类'),('海鲜'),('粮油'),('乳品'),('零食'),('饮料'),('日化'),('家居'),('母婴'),('个护'),('宠物'),('烘焙'),('生鲜礼盒');

-- 6.7 Products（120 个，均为上架且已审核，库存 100~1000）
SET @cat_min_id := (SELECT MIN(id) FROM `category`);
SET @sup_min_id := (SELECT MIN(id) FROM `supplier`);
SET @sup_cnt    := (SELECT COUNT(*) FROM `supplier`);
INSERT INTO `product` (`name`,`category_id`,`price`,`stock`,`version`,`supplier_id`,`status`,`approved`)
SELECT CONCAT('商品', LPAD(n,3,'0')),
       @cat_min_id + ((n-1) % (SELECT COUNT(*) FROM `category`)),
       ROUND(10 + RAND()*990, 2),
       100 + (n % 901),
       0,
       @sup_min_id + ((n-1) % @sup_cnt),
       1, 1
FROM (SELECT n FROM numbers WHERE n BETWEEN 1 AND 120) t;

-- 6.7.1 SKUs（为前 30 个商品生成 3 个 SKU）
INSERT INTO `product_sku` (`product_id`,`attrs`,`stock`,`price`)
SELECT p.id,
       JSON_OBJECT('颜色',CASE (n%3) WHEN 0 THEN '红' WHEN 1 THEN '绿' ELSE '蓝' END,'规格',CONCAT((n%3+1)*250,'g')),
       50 + (n%150),
       p.price + (n%3)
FROM `product` p
JOIN (SELECT n FROM numbers WHERE n BETWEEN 1 AND 90) t2 ON p.id <= (SELECT MIN(id)+29 FROM product)
WHERE (t2.n % 3) IN (0,1,2);

-- 6.8 Addresses（为所有用户各插入 1 条地址）
SET @comm_min := (SELECT MIN(id) FROM community);
SET @comm_cnt := (SELECT COUNT(*) FROM community);
INSERT INTO `address` (`user_id`,`community_id`,`detail`,`is_default`)
SELECT u.id,
       @comm_min + (ROW_NUMBER() OVER (ORDER BY u.id) - 1) % @comm_cnt,
       CONCAT('XX小区', LPAD(ROW_NUMBER() OVER (ORDER BY u.id),3,'0'), ' 栋 1 单元 10层10室'),
       1
FROM `user` u WHERE u.role IN ('USER','LEADER');

-- 6.9 Orders（近 60 天内 300 单；分布 CREATED/PAID/SHIPPED/DELIVERED）
SET @user_min := (SELECT MIN(id) FROM `user` WHERE role='USER');
SET @user_cnt := (SELECT COUNT(*) FROM `user` WHERE role='USER');
SET @leader_min := (SELECT MIN(id) FROM `leader`);
SET @leader_cnt := (SELECT COUNT(*) FROM `leader`);
SET @addr_min := (SELECT MIN(a.id) FROM `address` a JOIN `user` u ON a.user_id=u.id AND u.role IN ('USER','LEADER'));
SET @sup_cnt := (SELECT COUNT(*) FROM `supplier`);
SET @sup_min := (SELECT MIN(id) FROM `supplier`);

INSERT INTO `order` (`user_id`,`leader_id`,`supplier_id`,`address_id`,`status`,`total_amount`,`payment_method`,`tracking_no`,`remark`,`pay_time`,`create_time`,`update_time`)
SELECT 
  (@user_min + ((n-1) % @user_cnt))                              AS user_id,
  (@leader_min + ((n-1) % @leader_cnt))                            AS leader_id,
  (@sup_min + ((n-1) % @sup_cnt))                                  AS supplier_id,
  (@addr_min + ((n-1) % (SELECT COUNT(*) FROM `address`)))         AS address_id,
  CASE 
    WHEN (n % 20) < 3  THEN 'CREATED'
    WHEN (n % 20) < 9  THEN 'PAID'
    WHEN (n % 20) < 16 THEN 'SHIPPED'
    ELSE 'DELIVERED'
  END AS status,
  0.00 AS total_amount,
  CASE (n % 4) WHEN 0 THEN 'ALIPAY' WHEN 1 THEN 'WECHAT' WHEN 2 THEN 'CARD' ELSE 'CASH' END AS payment_method,
  CASE WHEN (n % 20) >= 9 THEN CONCAT('TN', LPAD(n,8,'0')) ELSE NULL END AS tracking_no,
  NULL AS remark,
  CASE WHEN (n % 20) >= 9 THEN DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*60) DAY) ELSE NULL END AS pay_time,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*60) DAY) AS create_time,
  NOW() AS update_time
FROM (SELECT n FROM numbers WHERE n BETWEEN 1 AND 300) t;

-- 6.10 Order Items（每单 1~4 件，同一供应商下的商品）
SET @cat_cnt := (SELECT COUNT(*) FROM category);
INSERT INTO `order_item` (`order_id`,`product_id`,`quantity`,`price`)
SELECT o.id,
       p.id,
       1 + (o.id % 4) AS quantity,
       p.price
FROM `order` o
JOIN `product` p ON p.supplier_id = o.supplier_id
WHERE (p.id % 5) = (o.id % 5);

-- 6.11 订单金额回填
UPDATE `order` o
JOIN (
  SELECT order_id, SUM(quantity * price) AS total
  FROM `order_item` GROUP BY order_id
) t ON t.order_id = o.id
SET o.total_amount = t.total;

-- 6.12 物流事件：为 SHIPPED/DELIVERED 订单写入跟踪节点
INSERT INTO `shipment_event` (`order_id`,`status`,`location`,`remark`,`event_time`)
SELECT id, 'SHIPPED', '仓库', '已出库', DATE_ADD(`create_time`, INTERVAL 1 DAY) FROM `order` WHERE `status` IN ('SHIPPED','DELIVERED');
INSERT INTO `shipment_event` (`order_id`,`status`,`location`,`remark`,`event_time`)
SELECT id, 'IN_TRANSIT', '转运中心', '干线运输中', DATE_ADD(`create_time`, INTERVAL 2 DAY) FROM `order` WHERE `status`='DELIVERED';
INSERT INTO `shipment_event` (`order_id`,`status`,`location`,`remark`,`event_time`)
SELECT id, 'DELIVERED', '收货地址', '已签收', DATE_ADD(`create_time`, INTERVAL 3 DAY) FROM `order` WHERE `status`='DELIVERED';

-- 6.13 Admin 消费日志示例
INSERT INTO `event_consume_log` (`consumer`,`message_id`,`event_type`,`status`,`retry_count`,`updated_at`,`created_at`) VALUES
 ('admin-service','msg-001','product.approved','DONE',0,NOW(),NOW()),
 ('admin-service','msg-002','order.paid','DONE',0,NOW(),NOW());

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
