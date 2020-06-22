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

import com.webank.webase.transaction.util.JsonUtils;
import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.contract.entity.ReqDeployInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
     * contract compile.
     * 
     * @param file file
     * @return
     */
    @ApiOperation(value = "contract compile", notes = "contract compile")
    @PostMapping("/compile")
    public ResponseEntity compile(
            @ApiParam(value = "contract zip file",
                    required = true) @RequestParam("file") MultipartFile file)
            throws BaseException, IOException {
        return contractService.compile(file);
    }

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
            throws BaseException {
        log.info("deploy start. deployInfo:{}", JsonUtils.toJSONString(deployInfo));
        checkParamResult(result);
        return contractService.deploy(deployInfo);
    }

    /**
     * get contract address.
     * 
     * @param groupId id
     * @param uuidDeploy uuid
     * @return
     */
    @ApiOperation(value = "getAddress", notes = "get contract address")
    @GetMapping("/address/{groupId}/{uuidDeploy}")
    public ResponseEntity getAddress(@PathVariable("groupId") int groupId,
            @PathVariable("uuidDeploy") String uuidDeploy) throws BaseException {
        return contractService.getAddress(groupId, uuidDeploy);
    }

    /**
     * get deploy event.
     * 
     * @param groupId id
     * @param uuidDeploy uuid
     * @return
     */
    @ApiOperation(value = "getEvent", notes = "get deploy event")
    @GetMapping("/event/{groupId}/{uuidDeploy}")
    public ResponseEntity getEvent(@PathVariable("groupId") int groupId,
            @PathVariable("uuidDeploy") String uuidDeploy) throws BaseException {
        return contractService.getEvent(groupId, uuidDeploy);
    }
    
    /**
     * get deploy info.
     * 
     * @param groupId id
     * @param uuidDeploy uuid
     * @return
     */
    @ApiOperation(value = "getDeployInfo", notes = "get deploy info")
    @GetMapping("/deployInfo/{groupId}/{uuidDeploy}")
    public ResponseEntity getDeployInfo(@PathVariable("groupId") int groupId,
            @PathVariable("uuidDeploy") String uuidDeploy) throws BaseException {
        return contractService.getDeployInfo(groupId, uuidDeploy);
    }
}
