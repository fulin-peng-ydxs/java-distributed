package id.quick;

import id.snowflake.SnowflakeIdGenerator;

/**
 * 雪花算法使用
 *
 * @author fulin-peng
 * 2023-12-01  10:48
 */
public class SnowflakeIdGeneratorQuick {

    public static void main(String[] args) {
        SnowflakeIdGenerator snowflakeIdGenerator = new SnowflakeIdGenerator(1, 1);
        System.out.println(snowflakeIdGenerator.nextId());
    }
}
