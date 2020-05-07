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

package com.webank.webase.transaction.trans;

import com.alibaba.fastjson.JSON;
import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.trans.entity.ReqTransSendInfo;
import com.webank.webase.transaction.util.CommonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.web3j.protocol.core.methods.response.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transaction Controller.
 * 
 */
@Api(value = "/trans", tags = "transaction interface")
@Slf4j
@RestController
@RequestMapping(value = "/trans")
public class TransController extends BaseController {
    @Autowired
    TransService transService;

    /**
     * transaction send.
     * 
     * @param transSendInfo parameter
     * @param result checkResult
     * @return
     */
    @ApiOperation(value = "transaction send", notes = "transaction send")
    @PostMapping("/send")
    public ResponseEntity send(@Valid @RequestBody ReqTransSendInfo transSendInfo,
            BindingResult result) throws Exception {
        log.info("transSend start. transSendInfo:{}", JSON.toJSONString(transSendInfo));
        checkBindResult(result);
        return CommonUtils.buildSuccessRsp(transService.send(transSendInfo));
    }

    /**
     * get transaction by hash.
     */
    @GetMapping("/getTransactionByHash/{groupId}/{transHash}")
    public ResponseEntity getTransactionByHash(@PathVariable("groupId") Integer groupId,
            @PathVariable("transHash") String transHash) throws BaseException {
        log.info("start getTransactionByHash groupId:{} transHash:{}", groupId, transHash);
        return CommonUtils.buildSuccessRsp(transService.getTransactionByHash(groupId, transHash));
    }

    /**
     * get transaction receipt by hash.
     */
    @GetMapping("/getTransactionReceipt/{groupId}/{transHash}")
    public ResponseEntity getTransactionReceipt(@PathVariable("groupId") Integer groupId,
            @PathVariable("transHash") String transHash) throws BaseException {
        log.info("start getTransactionReceipt groupId:{} transHash:{}", groupId, transHash);
        return CommonUtils.buildSuccessRsp(transService.getTransactionReceipt(groupId, transHash));
    }
}
