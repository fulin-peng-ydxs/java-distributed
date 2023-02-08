package redis.service;

import org.redisson.RedissonMultiLock;
import org.redisson.RedissonRedLock;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import redis.lock.DistributedLockClient;
import redis.lock.DistributedRedisLock;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author PengFuLin
 * 2023/2/6 23:41
 */
public class RedisService {


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLockClient distributedLockClient;

    @Autowired
    private RedissonClient redissonClient;


    //redis乐观锁
    public void deduct() {
        this.redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.watch("stock");  //监听此key的变化
                // 1. 查询库存信息
                Object stock = operations.opsForValue().get("stock");
                // 2. 判断库存是否充足
                int st = 0;
                if (stock != null && (st = Integer.parseInt(stock.toString())) > 0) {
                    // 3. 扣减库存
                    operations.multi(); //开启事务
                    operations.opsForValue().set("stock", String.valueOf(--st));
                    List<?> exec = operations.exec();   //提交事务：如果监听的值在其中发生了变更则提交失败
                    if (exec.size() == 0) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        deduct();
                    }
                    return exec;
                }
                return null;
            }
        });
    }

    //redis悲观锁基础实现
    public void deductBasic() {
        // 加锁setnx
        while (!this.redisTemplate.opsForValue().setIfAbsent("lock", "111")){
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            // 1. 查询库存信息
            String stock = redisTemplate.opsForValue().get("stock").toString();
            // 2. 判断库存是否充足
            if (stock != null && stock.length() != 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    // 3.扣减库存
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }
        } finally {
            // 解锁
            this.redisTemplate.delete("lock");
        }
    }

    //redis悲观锁lua实现
    public void deductLua() {
        String uuid = UUID.randomUUID().toString();
        // 加锁setnx
        while (!this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS)) {
            try {
                Thread.sleep(50);  // 重试：循环
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            // this.redisTemplate.expire("lock", 3, TimeUnit.SECONDS);
            // 1. 查询库存信息
            String stock = redisTemplate.opsForValue().get("stock").toString();
            // 2. 判断库存是否充足
            if (stock != null && stock.length() != 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0)
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));  // 3.扣减库存
            }
        } finally {
            // 先判断是否自己的锁，再解锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then " +
                    "   return redis.call('del', KEYS[1]) " +
                    "else " +
                    "   return 0 " +
                    "end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
        }
    }

    //redis悲观锁可重入实现
    public void deductReentrant() {
        DistributedRedisLock redisLock = this.distributedLockClient.getRedisLock("lock");
        redisLock.lock();
        try {
            // 1. 查询库存信息
            String stock = redisTemplate.opsForValue().get("stock").toString();
            // 2. 判断库存是否充足
            if (stock != null && stock.length() != 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    // 3.扣减库存
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }
            testReentrant();  //可重入测试
        } finally {
            redisLock.unlock();
        }
    }

    public void testReentrant () {
        DistributedRedisLock lock = this.distributedLockClient.getRedisLock("lock");
        lock.lock();
        System.out.println("测试可重入锁。. . . ");
        lock.unlock();
    }

    //redisson


    //redisson可重入锁：默认会自动过期和自动续期
    public void redissonReentrant() {
        RLock lock = redissonClient.getLock("anyLock");
        //加锁
        lock.lock();
        // 1. 查询库存信息
        String stock = redisTemplate.opsForValue().get("stock").toString();
        // 2. 判断库存是否充足
        if (stock != null && stock.length() != 0) {
            Integer st = Integer.valueOf(stock);
            if (st > 0) {
                // 3.扣减库存
                redisTemplate.opsForValue().set("stock", String.valueOf(--st));
            }
        }
        // 解锁
        lock.unlock();
    }

    //redisson可重入锁：不会自动续期
    public void redissonReentrantTime() {
        RLock lock = redissonClient.getLock("anyLock");
        //加锁:10s后会自动释放锁
        lock.lock(10,TimeUnit.SECONDS);
        // 1. 查询库存信息
        String stock = redisTemplate.opsForValue().get("stock").toString();
        // 2. 判断库存是否充足
        if (stock != null && stock.length() != 0) {
            Integer st = Integer.valueOf(stock);
            if (st > 0) {
                // 3.扣减库存
                redisTemplate.opsForValue().set("stock", String.valueOf(--st));
            }
        }
        // 解锁
        lock.unlock();
    }

    //redisson可重入锁：不会自动续期
    public void redissonReentrantTryTime() {
        RLock lock = redissonClient.getLock("anyLock");
        //尝试加锁：等待10s，10秒内未获取自动释放，拿锁10后也会自动释放锁
        try {
            lock.tryLock(10,10,TimeUnit.SECONDS);
        }catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 1. 查询库存信息
        String stock = redisTemplate.opsForValue().get("stock").toString();
        // 2. 判断库存是否充足
        if (stock != null && stock.length() != 0) {
            Integer st = Integer.valueOf(stock);
            if (st > 0) {
                // 3.扣减库存
                redisTemplate.opsForValue().set("stock", String.valueOf(--st));
            }
        }
        // 解锁
        lock.unlock();
    }

   //redisson公平锁
    public void redissonFairLock(){
        RLock fairLock = redissonClient.getFairLock("anyLock");
    }


    //redisson联锁
    public void redissonUnionLock(){
        RLock lock1 = redissonClient.getLock("lock1");
        RLock lock2 = redissonClient.getLock("lock2");
        RLock lock3 = redissonClient.getLock("lock3");
        // 同时加锁：lock1 lock2 lock3
        // 所有的锁都上锁成功才算成功。
        RedissonMultiLock lock = new RedissonMultiLock(lock1, lock2, lock3);

        lock.lock();

        lock.unlock();

    }

    //redisson红锁
    public void redissonRedLock(){
        RLock lock1 = redissonClient.getLock("lock1");
        RLock lock2 = redissonClient.getLock("lock2");
        RLock lock3 = redissonClient.getLock("lock3");
        // 同时加锁：lock1 lock2 lock3
        // 红锁在大部分节点上加锁成功就算成功。
        RedissonRedLock lock = new RedissonRedLock(lock1, lock2, lock3);
        lock.lock();
        lock.unlock();
    }

    //redisson读写锁
    public void redissonReadWriteLock() throws  Exception{
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("anyRWLock");
        // 最常见的使用方法
        rwlock.readLock().lock();
        // 或
            rwlock.writeLock().lock();

        // 10秒钟以后自动解锁
        // 无需调用unlock方法手动解锁
        rwlock.readLock().lock(10, TimeUnit.SECONDS);
        // 或
        rwlock.writeLock().lock(10, TimeUnit.SECONDS);

        // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
        boolean res = rwlock.readLock().tryLock(100, 10, TimeUnit.SECONDS);
        // 或
        res = rwlock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
    }

    //redisson信号量
    public void redissonRSemaphore() throws Exception {
        RSemaphore semaphore = redissonClient.getSemaphore("semaphore");
        semaphore.trySetPermits(3); //初始化：信号量
        semaphore.acquire(); //获取信号量 -1
        // do something
        semaphore.release(); //释放信号量 +1
    }


    //redisson闭锁
    public void redissonRCountDownLatch() throws  Exception{
        RCountDownLatch latch = redissonClient.getCountDownLatch("anyCountDownLatch");
        latch.trySetCount(1); //初始化闭锁值
        //减1
        latch.countDown();

        latch.await(); //阻塞：等待值为0后释放
    }

}
