package tradition.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tradition.service.StockService;

@RestController
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping("check/lock")
    public String checkAndLock(){
        this.stockService.checkAndLock();
        return "验库存并锁库存成功！";
    }
}
