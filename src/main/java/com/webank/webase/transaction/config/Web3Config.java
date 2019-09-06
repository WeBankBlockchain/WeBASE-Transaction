/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.webank.webase.transaction.config;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.channel.handler.ChannelConnections;
import org.fisco.bcos.channel.handler.GroupChannelConnectionsConfig;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * init web3sdk.
 *
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "sdk")
public class Web3Config {
    private String orgName;
    private int timeout = 10000;
    private GroupChannelConnectionsConfig groupConfig;

    /**
     * init web3j.
     * 
     * @return
     */
    @Bean
    public HashMap<Integer, Web3j> web3j() throws Exception {
        HashMap<Integer, Web3j> web3jMap = new HashMap<Integer, Web3j>();

        Service service = new Service();
        service.setOrgID(orgName);
        service.setGroupId(1);
        service.setThreadPool(sdkThreadPool());
        service.setAllChannelConnections(groupConfig);
        service.run();

        List<ChannelConnections> channelConnectList = groupConfig.getAllChannelConnections();
        for (ChannelConnections connect : channelConnectList) {
            int groupId = connect.getGroupId();
            log.info("init groupId:{}", groupId);
            ChannelEthereumService channelEthereumService = new ChannelEthereumService();
            channelEthereumService.setTimeout(timeout);
            channelEthereumService.setChannelService(service);
            Web3j web3j = Web3j.build(channelEthereumService, groupId);
            web3j.getGroupList().send().getGroupList();
            web3jMap.put(groupId, web3j);
        }
        return web3jMap;
    }

    /**
     * set sdk threadPool.
     * 
     * @return
     */
    @Bean
    public ThreadPoolTaskExecutor sdkThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new AbortPolicy());
        executor.setThreadNamePrefix("sdkThreadPool-");
        executor.initialize();
        return executor;
    }
}
