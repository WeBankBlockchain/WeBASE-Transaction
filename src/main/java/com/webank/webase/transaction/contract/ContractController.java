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

package com.webank.webase.transaction.contract;

import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.contract.entity.ReqContractCompile;
import com.webank.webase.transaction.contract.entity.ReqDeployInfo;
import com.webank.webase.transaction.contract.entity.RspContractCompile;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.JsonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.time.Duration;
import java.time.Instant;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
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
        Instant startTime = Instant.now();
        log.info("deploy start. deployInfo:{}", JsonUtils.toJSONString(deployInfo));
        checkBindResult(result);
        TransactionReceipt transactionReceipt = contractService.deploy(deployInfo);
        log.info("deploy end. useTime: {}", Duration.between(startTime, Instant.now()).toMillis());
        return CommonUtils.buildSuccessRsp(transactionReceipt);
    }

    /**
     * contract compile.
     * 
     * @param req parameter
     * @param result checkResult
     * @return
     */
    @ApiOperation(value = "contract compile", notes = "contract compile")
    @PostMapping("/compile")
    public ResponseEntity contractCompile(@Valid @RequestBody ReqContractCompile req,
            BindingResult result) throws Exception {
        Instant startTime = Instant.now();
        log.info("contractCompile start. req:{}", JsonUtils.toJSONString(req));
        checkBindResult(result);
        RspContractCompile compileInfo = contractService.contractCompile(req);
        log.info("contractCompile end. useTime: {}",
                Duration.between(startTime, Instant.now()).toMillis());
        return CommonUtils.buildSuccessRsp(compileInfo);
    }
}
