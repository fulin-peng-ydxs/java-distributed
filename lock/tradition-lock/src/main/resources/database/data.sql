CREATE TABLE `db_stock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `product_code` varchar(255) DEFAULT NULL COMMENT '��Ʒ���',
  `stock_code` varchar(255) DEFAULT NULL COMMENT '�ֿ���',
  `count` int(11) DEFAULT NULL COMMENT '�����',
  `version` int(11) DEFAULT NULL COMMENT '�汾',
   PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

INSERT INTO `db_stock` VALUES ('1', '1001', '������', '0', '5009');
INSERT INTO `db_stock` VALUES ('2', '1001', '�Ϻ���', '4999', '0');
INSERT INTO `db_stock` VALUES ('3', '1002', '���ڲ�', '4997', '0');
INSERT INTO `db_stock` VALUES ('4', '1002', '�Ϻ���', '5000', '0');

