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

package com.webank.webase.transaction.contract;

import com.alibaba.fastjson.JSON;
import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.contract.entity.ReqDeployInfo;
import com.webank.webase.transaction.util.CommonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ContractController.
 * 
 */
@Api(value = "/contract", tags = "contract interface")
@Slf4j
@RestController
@RequestMapping(value = "/contract")
public class ContractController extends BaseController {
    @Autowired
    ContractService contractService;

    /**
     * contract deploy.
     * 
     * @param deployInfo parameter
     * @param result checkResult
     * @return
     */
    @ApiOperation(value = "contract deploy", notes = "contract deploy")
    @PostMapping("/deploy")
    public ResponseEntity deploy(@Valid @RequestBody ReqDeployInfo deployInfo, BindingResult result)
            throws Exception {
        log.info("deploy start. deployInfo:{}", JSON.toJSONString(deployInfo));
        checkBindResult(result);
        return CommonUtils.buildSuccessRsp(contractService.deploy(deployInfo));
    }
}
