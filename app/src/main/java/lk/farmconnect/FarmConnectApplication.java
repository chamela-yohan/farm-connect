package lk.farmconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FarmConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmConnectApplication.class, args);
    }
}