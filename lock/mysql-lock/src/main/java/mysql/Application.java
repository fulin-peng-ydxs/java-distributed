package mysql;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author PengFuLin
 * 2023/2/14 0:36
 */
@SpringBootApplication
@MapperScan(basePackages = {"tradition.dao","mysql.dao"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
