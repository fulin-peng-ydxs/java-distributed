CREATE TABLE `tb_lock` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `lock_name` varchar(50) NOT NULL COMMENT '锁名',
    `class_name` varchar(100) DEFAULT NULL COMMENT '类名',
    `method_name` varchar(50) DEFAULT NULL COMMENT '方法名',
    `server_name` varchar(50) DEFAULT NULL COMMENT '服务器ip',
    `thread_name` varchar(50) DEFAULT NULL COMMENT '线程名',
    `create_time` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '获取锁时间',
    `desc` varchar(100) DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_unique` (`lock_name`)
)ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
