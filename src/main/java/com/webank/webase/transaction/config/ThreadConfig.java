package com.webank.webase.transaction.config;

import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Data
@EnableAsync
@Configuration
@ConfigurationProperties(prefix = "thread")
public class ThreadConfig {
    private int corePoolSize;
    private int maxPoolSize;
    private int queueCapacity;
    private int keepAlive;

    /**
     * transExecutor.
     * 
     * @return
     */
    @Bean
    public ThreadPoolTaskExecutor transExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAlive);
        executor.setRejectedExecutionHandler(new AbortPolicy());
        executor.setThreadNamePrefix("transExecutor-");
        executor.initialize();
        return executor;
    }
}
