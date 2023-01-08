CREATE TABLE `db_stock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_code` varchar(255) DEFAULT NULL COMMENT '商品编号',
  `stock_code` varchar(255) DEFAULT NULL COMMENT '仓库编号',
  `count` int(11) DEFAULT NULL COMMENT '库存量',
  `version` int(11) DEFAULT NULL COMMENT '版本',
   PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

INSERT INTO `db_stock` VALUES ('1', '1001', '北京仓', '0', '5009');
INSERT INTO `db_stock` VALUES ('2', '1001', '上海仓', '4999', '0');
INSERT INTO `db_stock` VALUES ('3', '1002', '深圳仓', '4997', '0');
INSERT INTO `db_stock` VALUES ('4', '1002', '上海仓', '5000', '0');

