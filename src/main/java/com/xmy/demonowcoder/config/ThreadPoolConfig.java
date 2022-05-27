package com.xmy.demonowcoder.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
// 启动Spring可定时任务线程池
@EnableScheduling
// 启动@Async注解实现线程池
@EnableAsync
public class ThreadPoolConfig {
}
