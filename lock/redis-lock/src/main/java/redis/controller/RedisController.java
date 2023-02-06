package redis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.service.RedisService;

/**
 * @author PengFuLin
 * 2023/2/6 23:39
 */
@RestController
public class RedisController {

    @Autowired
    private RedisService redisService;

    @GetMapping("check/lock")
    public String checkAndLock(){
        this.redisService.deduct();
        return "验库存并锁库存成功！";
    }
}
