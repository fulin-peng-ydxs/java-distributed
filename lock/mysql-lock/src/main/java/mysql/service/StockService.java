package mysql.service;

import mysql.dao.LockMapper;
import mysql.entity.Lock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tradition.dao.StockMapper;
import tradition.entity.Stock;

import java.util.Date;

@Service
public class StockService {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private LockMapper lockMapper;

    /**
     * 数据库分布式锁
     */
    public void checkAndLock() {
        // 加锁
        Lock lock = new Lock(null, "lock",
                this.getClass().getName(),null, null,null,
                new Date(), null);
        try {
            this.lockMapper.insert(lock);
        } catch (Exception ex) {
            // 获取锁失败，则重试
            try {
                Thread.sleep(50);
                this.checkAndLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 先查询库存是否充足
        Stock stock = this.stockMapper.selectById(1L);

        // 再减库存
        if (stock != null && stock.getCount() > 0){

            stock.setCount(stock.getCount() - 1);
            this.stockMapper.updateById(stock);
        }
        // 释放锁
        this.lockMapper.deleteById(lock.getId());
    }
}
