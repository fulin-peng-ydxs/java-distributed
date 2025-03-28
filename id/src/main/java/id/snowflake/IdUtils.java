package id.snowflake;


/**
 * @author pengshuaifeng
 * 2025/3/28
 */
public class IdUtils {

    private static final SnowflakeIdGenerator idWorker = new SnowflakeIdGenerator(0, 0);
    public static Long getId() {
        return idWorker.nextId();
    }
}
