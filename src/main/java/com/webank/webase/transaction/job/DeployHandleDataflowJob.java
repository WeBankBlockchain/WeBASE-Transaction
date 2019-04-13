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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.contract.ContractMapper;
import com.webank.webase.transaction.contract.ContractService;
import com.webank.webase.transaction.contract.DeployInfoDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(value = {"constant.ifDistributedTask"}, matchIfMissing = false)
public class DeployHandleDataflowJob implements DataflowJob<DeployInfoDto> {
    @Autowired
    ContractService contractService;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private ConstantProperties properties;

    @Value("${job.dataflow.shardingTotalCount}")
    private int shardingTotalCount;

    @Override
    public List<DeployInfoDto> fetchData(ShardingContext context) {
        // query untreated data
        List<DeployInfoDto> deployInfoList = contractMapper.selectUnStatTransByJob(
                properties.getRequestCountMax(), properties.getSelectCount(),
                properties.getIntervalTime(), shardingTotalCount, context.getShardingItem());
        if (deployInfoList == null || deployInfoList.size() == 0) {
            log.info("no data was found in deploy item:{}", context.getShardingItem());
        }
        return deployInfoList;
    }

    @Override
    public void processData(ShardingContext shardingContext, List<DeployInfoDto> deployInfoList) {
    	contractService.handleDeployInfo(deployInfoList);
    }
}
