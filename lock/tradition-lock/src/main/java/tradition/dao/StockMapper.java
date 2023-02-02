package tradition.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import tradition.entity.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface StockMapper extends BaseMapper<Stock> {

    @Update("update db_stock set count=count -#{count} where product_code = #{productCode} and count >= #{count}")
    int updateStock (@Param("productCode") String productCode,@Param("count") Integer count);

    Stock selectStockForUpdate(Long id);
}
