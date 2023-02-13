package zk.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.*;
import org.apache.curator.framework.recipes.shared.SharedCount;
import org.springframework.beans.factory.annotation.Autowired;
import tradition.dao.StockMapper;
import tradition.entity.Stock;
import zk.config.ZkClient;
import zk.lock.ZkDistributedLock;
import zk.lock.ZkDistributedLockForWait;
import zk.lock.ZkDistributedLockReentrant;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author PengFuLin
 * 2023/2/12 21:48
 */
public class ZKService {

    @Autowired
    private ZkClient client;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private CuratorFramework curatorFramework;


    //简单实现
    public void checkAndLock() {
        // 加锁，获取锁失败重试
        ZkDistributedLock lock = this.client.getZkDistributedLock("lock");
        lock.lock();
        // 先查询库存是否充足
        Stock stock = this.stockMapper.selectById(1L);
        // 再减库存
        if (stock != null && stock.getCount() > 0){
            stock.setCount(stock.getCount() - 1);
            this.stockMapper.updateById(stock);
        }
        // 释放锁
        lock.unlock();
    }

    //阻塞排队实现
    public void checkAndLockForWait() {
        // 加锁，获取锁失败重试
        ZkDistributedLockForWait lock = this.client.getZkDistributedLockForWait("lock");
        lock.lock();
        // 先查询库存是否充足
        Stock stock = this.stockMapper.selectById(1L);
        // 再减库存
        if (stock != null && stock.getCount() > 0){
            stock.setCount(stock.getCount() - 1);
            this.stockMapper.updateById(stock);
        }
        // 释放锁
        lock.unlock();
    }

    //可重入
    public void checkAndLockForkReentrant() {
        // 加锁，获取锁失败重试
        ZkDistributedLockReentrant lock = this.client.getZkDistributedLockReentrant("lock");
        lock.lock();
        // 先查询库存是否充足
        Stock stock = this.stockMapper.selectById(1L);
        // 再减库存
        if (stock != null && stock.getCount() > 0){
            stock.setCount(stock.getCount() - 1);
            this.stockMapper.updateById(stock);
        }
        // 释放锁
        lock.unlock();
    }

    //Curator客户端

    //可重入:基本原理也是序列化节点+监听事件的排队获取锁
    public void checkAndLockCurator() {
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, "/curator/lock");
        try {
            mutex.acquire();// 加锁：
//            mutex.acquire(long time, TimeUnit unit); //超时后会直接放弃抢锁
            // 先查询库存是否充足
            Stock stock = this.stockMapper.selectById(1L);
            // 再减库存
            if (stock != null && stock.getCount() > 0){
                stock.setCount(stock.getCount() - 1);
                this.stockMapper.updateById(stock);
            }
             this.testSub(mutex);
            mutex.release(); // 释放锁
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void testSub(InterProcessMutex mutex) {
        try {
            mutex.acquire();
            System.out.println("测试可重入锁。。。。");
            mutex.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //不可重入
    public void deduct() {
        InterProcessSemaphoreMutex mutex =
                new InterProcessSemaphoreMutex(curatorFramework, "/curator/lock");
        try {
            mutex.acquire();
            // 先查询库存是否充足
            Stock stock = this.stockMapper.selectById(1L);
            // 再减库存
            if (stock != null && stock.getCount() > 0){
                stock.setCount(stock.getCount() - 1);
                this.stockMapper.updateById(stock);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                mutex.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //读写
    public void testZkReadLock() {
        try {
            InterProcessReadWriteLock rwlock =
                    new InterProcessReadWriteLock(curatorFramework, "/curator/rwlock");
            rwlock.readLock().acquire(10, TimeUnit.SECONDS);
            // TODO：一顿读的操作。。。。
            rwlock.readLock().release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testZkWriteLock() {
        try {
            InterProcessReadWriteLock rwlock =
                    new InterProcessReadWriteLock(curatorFramework, "/curator/rwlock");
            rwlock.writeLock().acquire(10, TimeUnit.SECONDS);
            // TODO：一顿写的操作。。。。
            rwlock.writeLock().release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //信号量
    public void testSemaphore() {
        // 设置资源量 限流的线程数
        InterProcessSemaphoreV2 semaphoreV2 = new InterProcessSemaphoreV2(curatorFramework, "/locks/semaphore", 5);
        try {
            Lease acquire = semaphoreV2.acquire();// 获取资源，获取资源成功的线程可以继续处理业务操作。否则会被阻塞住
            //doSomething....
            semaphoreV2.returnLease(acquire); // 手动释放资源，后续请求线程就可以获取该资源
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //共享计数器
    public void testZkShareCount() {
        try {
            // 第三个参数是共享计数的初始值
            SharedCount sharedCount =
                    new SharedCount(curatorFramework, "/curator/count", 0);
            // 启动共享计数器
            sharedCount.start();
            // 获取共享计数的值
            int count = sharedCount.getCount();
            // 修改共享计数的值
            int random = new Random().nextInt(1000);
            sharedCount.setCount(random);
            System.out.println("我获取了共享计数的初始值：" + count + "，并把计数器的值改为：" + random);
            sharedCount.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
