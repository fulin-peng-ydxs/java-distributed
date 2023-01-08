package tradition;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "tradition.dao")
public class DistributedLockApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributedLockApplication.class, args);
    }
}
