package br.pucrs.construcao.representantes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class RepresentantesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RepresentantesServiceApplication.class, args);
    }
}
