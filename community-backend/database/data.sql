-- data.sql
-- Extracted from ../community_schema_mysql.sql; includes seed/demo data only.
-- Run this after applying community_schema_mysql_schema.sql.
SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `community`;

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

