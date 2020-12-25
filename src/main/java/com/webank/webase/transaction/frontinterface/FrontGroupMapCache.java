/**
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
package com.webank.webase.transaction.frontinterface;


import com.webank.webase.transaction.base.Constants;
import com.webank.webase.transaction.frontinterface.entity.FrontGroup;
import com.webank.webase.transaction.repository.mapper.TbFrontGroupMapMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FrontGroupMapCache {

    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private TbFrontGroupMapMapper tbFrontGroupMapMapper;
    @Autowired
    private Constants constants;

    @Scheduled(cron = "${constant.frontGroupCacheClearTaskCron}")
    public void taskStart() {
        frontGroupCacheClearTask();
    }

    public synchronized void frontGroupCacheClearTask() {
        log.info("start frontGroupCacheClearTask.");
        Cache cache = cacheManager.getCache("frontGroup");
        if (cache != null) {
            cache.clear();
        }
        log.info("end frontGroupCacheClearTask.");
    }
    
    /**
     * getFrontGroupList.
     */
    @Cacheable(cacheNames = "frontGroup")
    public List<FrontGroup> getFrontGroupList(int chainId, int groupId) {
        List<FrontGroup> list = new ArrayList<>();
        if (StringUtils.isBlank(constants.getFrontServer())) {
            list = getListByMultiId(chainId, groupId);
        } else {
            list = getLocalConfig(chainId, groupId);
        }
        return list;
    }
    
    /**
     * getListByMultiId.
     */
    public List<FrontGroup> getListByMultiId(int chainId, int groupId) {
        List<FrontGroup> list = tbFrontGroupMapMapper.selectByChainIdAndGroupId(chainId, groupId);
        return list;
    }
    
    /**
     * getLocalConfig.
     */
    public List<FrontGroup> getLocalConfig(int chainId, int groupId) {
        List<FrontGroup> list = new ArrayList<>();
        String frontServer = constants.getFrontServer();
        FrontGroup frontGroup = new FrontGroup();
        frontGroup.setChainId(chainId);
        frontGroup.setGroupId(groupId);
        frontGroup.setFrontIp(frontServer.split(":")[0]);
        frontGroup.setFrontPort(Integer.parseInt(frontServer.split(":")[1]));
        list.add(frontGroup);
        return list;
    }
}
