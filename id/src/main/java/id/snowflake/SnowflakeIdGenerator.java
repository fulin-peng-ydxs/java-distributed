package id.snowflake;

public class SnowflakeIdGenerator {

    // 起始的时间戳
    private final static long START_STMP = 1480166465631L;
    private long lastStmp = -1L;

    // 每一部分占用的位数
    private final static long SEQUENCE_BIT = 12; // 序列号占用的位数
    private final static long MACHINE_BIT = 5;   // 机器标识占用的位数
    private final static long DATACENTER_BIT = 5;// 数据中心占用的位数
    // 每一部分的最大值
    private final static long MAX_DATACENTER_NUM = ~(-1L << DATACENTER_BIT);
    private final static long MAX_MACHINE_NUM = ~(-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);
    // 每一部分向左的位移
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private long sequence = 0L; // 序列号
    private final long datacenterId;  // 数据中心
    private final long machineId;     // 机器标识

    /**
     * 构造函数：传入数据中心和机器标识
     * 2023/12/1 0001 10:51
     * @author fulin-peng
     */
    public SnowflakeIdGenerator(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    /**
     * 生成自增id
     * 2023/12/1 0001 11:07
     * @author fulin-peng
     */
    public synchronized long nextId() {
        long currStmp = getNewstmp();
        if (currStmp < lastStmp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }

        if (currStmp == lastStmp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;  //每4096个值一轮回
            if (sequence == 0L) {
                currStmp = getNextMill();
            }
        } else {
            sequence = 0L;
        }

        lastStmp = currStmp;

        return (currStmp - START_STMP) << TIMESTMP_LEFT // 时间戳部分
                | datacenterId << DATACENTER_LEFT       // 数据中心部分
                | machineId << MACHINE_LEFT             // 机器标识部分
                | sequence;                             // 序列号部分
    }

    /**
     * 下一时间戳
     * 2023/12/1 0001 11:00
     * @author fulin-peng
     */
    private long getNextMill() {
        long mill = getNewstmp();
        while (mill <= lastStmp) {
            mill = getNewstmp();
        }
        return mill;
    }

    /**
     * 当前时间戳
     * 2023/12/1 0001 10:59
     * @author fulin-peng
     */
    private long getNewstmp() {
        return System.currentTimeMillis();
    }


    /**
     * 单例对象控制器
     * 2023/12/1 0001 11:21
     * @author fulin-peng
     */
    private static class Holder {
        // 单例的初始化，使用时请根据实际情况替换datacenterId和machineId的值
        private static final SnowflakeIdGenerator INSTANCE = new SnowflakeIdGenerator(1, 1);
    }

    /**
     * 单例对象获取
     * 2023/12/1 0001 11:22
     * @author fulin-peng
     */
    public static SnowflakeIdGenerator getInstance() {
        return Holder.INSTANCE;
    }
}
