package tradition.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tradition.dao.StockMapper;
import tradition.entity.Stock;

@Service
public class StockService {

    @Autowired
    private StockMapper stockMapper;

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
}
