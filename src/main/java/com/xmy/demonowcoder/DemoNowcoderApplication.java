package com.xmy.demonowcoder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class DemoNowcoderApplication {

    @PostConstruct
    public void init() {
        /**
         * 解决Netty启动冲突
         * 查阅：Netty4Util.setAvailableProcessors()
         */
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoNowcoderApplication.class, args);
    }

}
