package tradition.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tradition.dao.StockMapper;
import tradition.entity.Stock;
import tradition.lock.DistributedLockClient;
import tradition.lock.DistributedRedisLock;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StockService {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLockClient distributedLockClient;

    //并发超卖问题演示
    public void checkAndLock() {
        // 先查询库存是否充足
        Stock stock = this.stockMapper.selectById(1L);
        // 再减库存
        if (stock != null && stock.getCount() > 0){
            stock.setCount(stock.getCount() - 1);
            this.stockMapper.updateById(stock);
        }
    }

    //jvm加锁演示
    public synchronized void checkAndLockBySynchronized() {
        // 先查询库存是否充足
        Stock stock = this.stockMapper.selectById(1L);
        // 再减库存
        if (stock != null && stock.getCount() > 0){
            stock.setCount(stock.getCount() - 1);
            this.stockMapper.updateById(stock);
        }
    }

    //mysql加锁演示

    //单条sql
    public void checkAndLockByUpdateStock(){
        this.stockMapper.updateStock("123", 1);
    }

    //悲观锁
    @Transactional //开启事务，以维持锁
    public void checkAndLockByForUpdate(){
        // 先查询库存是否充足
        Stock stock = this.stockMapper.selectStockForUpdate(1L);
        // 再减库存
        if (stock != null && stock.getCount() > 0){
            stock.setCount(stock.getCount() - 1);
            this.stockMapper.updateById(stock);
        }
    }

    //乐观锁
    public void checkAndLockForVersion(){
        // 先查询库存是否充足
        Stock stock = this.stockMapper.selectById(1L);
        // 再减库存
        if (stock != null && stock.getCount() > 0){
            // 获取版本号（或者时间戳）
            Integer version = stock.getVersion();
            stock.setCount(stock.getCount() - 1);
            // 每次更新 版本号 + 1
            stock.setVersion(stock.getVersion() + 1);
            // 更新之前先判断是否是之前查询的那个版本，如果不是重试
            if (this.stockMapper.update(stock, new UpdateWrapper<Stock>().eq("id", stock.getId()).eq("version", version)) == 0) {
                checkAndLock();
            }
        }
    }

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



}
