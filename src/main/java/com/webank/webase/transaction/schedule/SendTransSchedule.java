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
package com.webank.webase.transaction.schedule;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.contract.ContractMapper;
import com.webank.webase.transaction.contract.ContractService;
import com.webank.webase.transaction.contract.DeployInfoDto;
import com.webank.webase.transaction.trans.TransInfoDto;
import com.webank.webase.transaction.trans.TransMapper;
import com.webank.webase.transaction.trans.TransService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SendTransSchedule {
    @Autowired
    private TransMapper transMapper;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private TransService transService;
    @Autowired
    private ContractService contractService;
    @Autowired
    private ConstantProperties properties;

    /**
     * deploySchedule.
     */
    public void deploySchedule() {
    	log.debug("deploySchedule start...");
    	List<DeployInfoDto> deployInfoList =
    			contractMapper.selectUnStatTrans(properties.getRequestCountMax(),
    					properties.getSelectCount(), properties.getIntervalTime());
    	contractService.handleDeployInfo(deployInfoList);
    }
    
    /**
     * transSchedule.
     */
    public void transSchedule() {
        log.debug("transSchedule start...");
        List<TransInfoDto> transInfoList =
                transMapper.selectUnStatTrans(properties.getRequestCountMax(),
                        properties.getSelectCount(), properties.getIntervalTime());
        transService.handleTransInfo(transInfoList);
    }
}
