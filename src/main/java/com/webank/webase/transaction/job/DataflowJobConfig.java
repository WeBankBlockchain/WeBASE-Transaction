/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.webase.transaction.job;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobRootConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import javax.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * DataflowJobConfig.
 */
@Data
@Configuration
@ConditionalOnProperty(value = {"constant.ifDistributedTask"}, matchIfMissing = false)
@EnableTransactionManagement(proxyTargetClass = true)
@ConfigurationProperties(prefix = "job.dataflow")
public class DataflowJobConfig {
    @Resource
    private ZookeeperRegistryCenter regCenter;

    private int shardingTotalCount;
    private String shardingItemParameters;
    @Value("${constant.cronTrans}")
    private String cronMonitor;

    /**
     * transMonitorScheduler.
     * 
     * @param dataflowJob instance
     * @return
     */
    @Bean(initMethod = "init")
    public JobScheduler transMonitorScheduler(final TransHandleDataflowJob dataflowJob) {
        return new SpringJobScheduler(dataflowJob, regCenter, transMonitorDataflowConfig());
    }

    /**
     * transMonitorDataflowConfig.
     * 
     * @return
     */
    private LiteJobConfiguration transMonitorDataflowConfig() {
        JobCoreConfiguration dataflowCoreConfig =
                JobCoreConfiguration
                        .newBuilder(TransHandleDataflowJob.class.getName(), cronMonitor,
                                shardingTotalCount)
                        .shardingItemParameters(shardingItemParameters).build();
        DataflowJobConfiguration dataflowJobConfig =
                new DataflowJobConfiguration(dataflowCoreConfig,
                        TransHandleDataflowJob.class.getCanonicalName(), false);
        JobRootConfiguration dataflowJobRootConfig =
                LiteJobConfiguration.newBuilder(dataflowJobConfig).overwrite(true).build();

        return (LiteJobConfiguration) dataflowJobRootConfig;
    }

}
