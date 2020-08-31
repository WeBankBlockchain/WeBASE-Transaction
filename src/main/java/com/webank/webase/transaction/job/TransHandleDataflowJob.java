/*
 * Copyright 2014-2020 the original author or authors.
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

package com.webank.webase.transaction.job;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.trans.TransMapper;
import com.webank.webase.transaction.trans.TransService;
import com.webank.webase.transaction.trans.entity.TransInfoDto;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * TransHandleDataflowJob.
 *
 */
@Slf4j
@Component
@ConditionalOnProperty(value = {"constant.ifDistributedTask"}, matchIfMissing = false)
public class TransHandleDataflowJob implements DataflowJob<TransInfoDto> {
    @Autowired
    TransService transService;
    @Autowired
    private TransMapper transMapper;
    @Autowired
    private ConstantProperties properties;

    @Value("${job.dataflow.shardingTotalCount}")
    private int shardingTotalCount;

    @Override
    public List<TransInfoDto> fetchData(ShardingContext context) {
        log.debug("trans fetchData item:{}", context.getShardingItem());
        // query untreated data
        List<TransInfoDto> transInfoList = transMapper.selectUnStatTransByJob(
                properties.getRequestCountMax(), properties.getSelectCount() * shardingTotalCount,
                properties.getIntervalTime(), shardingTotalCount, context.getShardingItem());
        Iterator<TransInfoDto> iterator = transInfoList.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId() % shardingTotalCount != context.getShardingItem()) {
                iterator.remove();
            }
        }
        return transInfoList;
    }

    @Override
    public void processData(ShardingContext shardingContext, List<TransInfoDto> transInfoList) {
        transService.handleTransInfo(transInfoList);
    }
}
