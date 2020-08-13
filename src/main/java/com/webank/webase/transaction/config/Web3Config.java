/*
 * Copyright 2014-2019 the original author or authors.
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

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.chain.entity.ChainConfigInfo;
import com.webank.webase.transaction.chain.entity.QueryConfigInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.channel.client.Service;
import org.fisco.bcos.channel.handler.ChannelConnections;
import org.fisco.bcos.channel.handler.GroupChannelConnectionsConfig;
import org.fisco.bcos.web3j.crypto.EncryptType;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.channel.ChannelEthereumService;
import org.fisco.bcos.web3j.protocol.core.methods.response.NodeVersion;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
    private int corePoolSize = 100;
    private int maxPoolSize = 500;
    private int queueCapacity = 500;
    private int keepAlive = 60;
    private List<ChainConfigInfo> chainConfigList;
    private int encryptType;
    private List<QueryConfigInfo> configInfoList = new ArrayList<>();

    @Bean(name = "encryptType")
    public EncryptType EncryptType() {
        // 1: guomi, 0: standard
        log.info("*****init EncrytType:" + encryptType);
        return new EncryptType(encryptType);
    }

    /**
     * init web3j.
     * 
     * @return
     */
    @Bean
    @DependsOn("encryptType")
    public Map<Integer, Map<Integer, Web3j>> web3jMapWithChainId() throws Exception {
        ConcurrentHashMap<Integer, Map<Integer, Web3j>> web3jMapWithChainId =
                new ConcurrentHashMap<Integer, Map<Integer, Web3j>>(chainConfigList.size());
        for (ChainConfigInfo chainConfigInfo : chainConfigList) {
            int chainId = chainConfigInfo.getChainId();
            GroupChannelConnectionsConfig groupConfig = chainConfigInfo.getGroupConfig();
            ConcurrentHashMap<Integer, Web3j> web3jMap = new ConcurrentHashMap<Integer, Web3j>(
                    groupConfig.getAllChannelConnections().size());
            List<Integer> groupList = new ArrayList<>();
            for (ChannelConnections connect : groupConfig.getAllChannelConnections()) {
                int groupId = connect.getGroupId();
                log.info("init chainId:{} groupId:{}", chainId, groupId);
                // set service
                Service service = new Service();
                service.setOrgID(orgName);
                service.setThreadPool(sdkThreadPool());
                service.setAllChannelConnections(groupConfig);
                service.setGroupId(groupId);
                service.run();
                ChannelEthereumService channelEthereumService = new ChannelEthereumService();
                channelEthereumService.setTimeout(timeout);
                channelEthereumService.setChannelService(service);
                Web3j web3j = Web3j.build(channelEthereumService, groupId);
                web3j.getGroupList().send().getGroupList();
                web3jMap.put(groupId, web3j);
                groupList.add(groupId);
            }
            web3jMapWithChainId.put(chainId, web3jMap);
            configInfoList.add(new QueryConfigInfo(chainId, groupList));
        }
        return web3jMapWithChainId;
    }

    /**
     * set sdk threadPool.
     * 
     * @return
     */
    @Bean
    public ThreadPoolTaskExecutor sdkThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAlive);
        executor.setRejectedExecutionHandler(new AbortPolicy());
        executor.setThreadNamePrefix("sdkThreadPool-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Map<Integer, NodeVersion> versionMap(
            Map<Integer, Map<Integer, Web3j>> web3jMapWithChainId) throws Exception {
        ConcurrentHashMap<Integer, NodeVersion> versionMap =
                new ConcurrentHashMap<Integer, NodeVersion>(web3jMapWithChainId.size());
        for (Integer s : web3jMapWithChainId.keySet()) {
            Map<Integer, Web3j> web3jMap = web3jMapWithChainId.get(s);
            Set<Integer> iSet = web3jMap.keySet();
            if (iSet.isEmpty()) {
                log.error("web3jMap is empty, please check your node status.");
                throw new BaseException(ConstantCode.WEB3JMAP_IS_EMPTY);
            }
            // get random index to get web3j
            Integer index = iSet.iterator().next();
            NodeVersion version = web3jMap.get(index).getNodeVersion().send();
            versionMap.put(s, version);
        }
        return versionMap;
    }
}
