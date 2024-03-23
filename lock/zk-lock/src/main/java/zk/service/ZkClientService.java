package zk.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tradition.dao.StockMapper;
import tradition.entity.Stock;
import zk.config.ZkClientConfig;
import zk.lock.ZkDistributedLock;
import zk.lock.ZkDistributedLockForWait;
import zk.lock.ZkDistributedLockReentrant;

/**
 * @author PengFuLin
 * 2023/2/12 21:48
 */
@Service
public class ZkClientService {

    @Autowired
    private ZkClientConfig client;

    @Autowired
    private StockMapper stockMapper;


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

}
