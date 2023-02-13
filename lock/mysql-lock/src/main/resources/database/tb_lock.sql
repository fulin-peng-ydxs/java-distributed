CREATE TABLE `tb_lock` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `lock_name` varchar(50) NOT NULL COMMENT '����',
    `class_name` varchar(100) DEFAULT NULL COMMENT '����',
    `method_name` varchar(50) DEFAULT NULL COMMENT '������',
    `server_name` varchar(50) DEFAULT NULL COMMENT '������ip',
    `thread_name` varchar(50) DEFAULT NULL COMMENT '�߳���',
    `create_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '��ȡ��ʱ��',
    `desc` varchar(100) DEFAULT NULL COMMENT '����',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_unique` (`lock_name`)
)ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
