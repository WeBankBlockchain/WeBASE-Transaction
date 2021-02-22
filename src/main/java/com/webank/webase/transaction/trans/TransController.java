/*
 * Copyright 2014-2021 the original author or authors.
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

import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.trans.entity.ReqQueryTransHandle;
import com.webank.webase.transaction.trans.entity.ReqSendSignedInfo;
import com.webank.webase.transaction.trans.entity.ReqSignedTransHandle;
import com.webank.webase.transaction.trans.entity.ReqTransSendInfo;
import com.webank.webase.transaction.trans.entity.TransResultDto;
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
        Instant startTime = Instant.now();
        log.info("transSend start. transSendInfo:{}", JsonUtils.toJSONString(transSendInfo));
        checkBindResult(result);
        TransResultDto transResultDto = transService.send(transSendInfo);
        log.info("transSend end. useTime: {}",
                Duration.between(startTime, Instant.now()).toMillis());
        return CommonUtils.buildSuccessRsp(transResultDto);
    }

    /**
     * get transaction by hash.
     */
    @GetMapping("/getTransactionByHash/{chainId}/{groupId}/{transHash}")
    public ResponseEntity getTransactionByHash(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId, @PathVariable("transHash") String transHash)
            throws BaseException {
        log.info("start getTransactionByHash chainId:{} groupId:{} transHash:{}", chainId, groupId,
                transHash);
        return CommonUtils
                .buildSuccessRsp(transService.getTransactionByHash(chainId, groupId, transHash));
    }

    /**
     * get transaction receipt by hash.
     */
    @GetMapping("/getTransactionReceipt/{chainId}/{groupId}/{transHash}")
    public ResponseEntity getTransactionReceipt(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId, @PathVariable("transHash") String transHash)
            throws BaseException {
        log.info("start getTransactionReceipt chainId:{} groupId:{} transHash:{}", chainId, groupId,
                transHash);
        return CommonUtils
                .buildSuccessRsp(transService.getTransactionReceipt(chainId, groupId, transHash));
    }


    @ApiOperation(value = "sendSignedTransaction")
    @PostMapping("/sendSignedTransaction")
    public ResponseEntity sendSignedTransaction(
            @Valid @RequestBody ReqSignedTransHandle reqSignedTransHandle, BindingResult result)
            throws BaseException {
        Instant startTime = Instant.now();
        log.info("sendSignedTransaction start. startTime:{}", startTime.toEpochMilli());

        checkBindResult(result);
        TransactionReceipt receipt = transService.sendSignedTransaction(
                reqSignedTransHandle.getSignedStr(), reqSignedTransHandle.getSync(),
                reqSignedTransHandle.getChainId(), reqSignedTransHandle.getGroupId());

        log.info("sendSignedTransaction end. useTime: {}",
                Duration.between(startTime, Instant.now()).toMillis());
        return CommonUtils.buildSuccessRsp(receipt);
    }

    @ApiOperation(value = "sendQueryTransaction")
    @PostMapping("/sendQueryTransaction")
    public ResponseEntity sendQueryTransaction(
            @Valid @RequestBody ReqQueryTransHandle reqQueryTransHandle, BindingResult result)
            throws BaseException {
        Instant startTime = Instant.now();
        log.info("sendQueryTransaction start. startTime:{}", startTime.toEpochMilli());

        checkBindResult(result);
        Object obj = transService.sendQueryTransaction(reqQueryTransHandle.getEncodeStr(),
                reqQueryTransHandle.getContractAddress(), reqQueryTransHandle.getFuncName(),
                JsonUtils.toJSONString(reqQueryTransHandle.getFunctionAbi()),
                reqQueryTransHandle.getChainId(), reqQueryTransHandle.getGroupId());
        log.info("sendQueryTransaction end. useTime: {}",
                Duration.between(startTime, Instant.now()).toMillis());
        return CommonUtils.buildSuccessRsp(obj);
    }
    
    @ApiOperation(value = "sendSigned")
    @PostMapping("/sendSigned")
    public ResponseEntity sendSigned(
            @Valid @RequestBody ReqSendSignedInfo reqSendSignedInfo, BindingResult result)
            throws Exception {
        Instant startTime = Instant.now();
        log.info("sendSigned start. startTime:{}", startTime.toEpochMilli());

        checkBindResult(result);
        TransResultDto transResultDto = transService.sendSigned(reqSendSignedInfo);

        log.info("sendSigned end. useTime: {}",
                Duration.between(startTime, Instant.now()).toMillis());
        return CommonUtils.buildSuccessRsp(transResultDto);
    }
}
