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
import com.webank.webase.transaction.util.JsonUtils;
import com.webank.webase.transaction.util.ReqQueryTransHandle;
import com.webank.webase.transaction.util.ReqSignedTransHandle;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.protocol.core.methods.response.Transaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

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


    @ApiOperation(value = "send signed transaction ")
    @ApiImplicitParam(name = "reqSignedTransHandle", value = "transaction info", required = true, dataType = "ReqSignedTransHandle")
    @PostMapping("/signed-transaction")
    public TransactionReceipt sendSignedTransaction(@Valid @RequestBody ReqSignedTransHandle reqSignedTransHandle, BindingResult result) throws BaseException {
        log.info("transHandleLocal start. ReqSignedTransHandle:[{}]", JsonUtils.toJSONString(reqSignedTransHandle));

        Instant startTime = Instant.now();
        log.info("transHandleLocal start startTime:{}", startTime.toEpochMilli());

        checkBindResult(result);
        String signedStr = reqSignedTransHandle.getSignedStr();
        if (StringUtils.isBlank(signedStr)) {
            throw new BaseException(ConstantCode.ENCODE_STR_CANNOT_BE_NULL);
        }
        TransactionReceipt receipt =  transService.sendSignedTransaction(signedStr, reqSignedTransHandle.getSync(),reqSignedTransHandle.getGroupId());
        log.info("transHandleLocal end  useTime:{}", Duration.between(startTime, Instant.now()).toMillis());
        return receipt;
    }

    @ApiOperation(value = "send query transaction ")
    @ApiImplicitParam(name = "reqQueryTransHandle", value = "transaction info", required = true, dataType = "ReqQueryTransHandle")
    @PostMapping("/query-transaction")
    public Object sendQueryTransaction(@Valid @RequestBody ReqQueryTransHandle reqQueryTransHandle, BindingResult result)   throws BaseException{
        log.info("transHandleLocal start. ReqQueryTransHandle:[{}]", JsonUtils.toJSONString(reqQueryTransHandle));

        Instant startTime = Instant.now();
        log.info("transHandleLocal start startTime:{}", startTime.toEpochMilli());

        checkBindResult(result);
        String encodeStr = reqQueryTransHandle.getEncodeStr();
        if (StringUtils.isBlank(encodeStr)) {
            throw new BaseException(ConstantCode.ENCODE_STR_CANNOT_BE_NULL);
        }
        Object obj =  transService.sendQueryTransaction(encodeStr, reqQueryTransHandle.getContractAddress(),reqQueryTransHandle.getFuncName(),reqQueryTransHandle.getContractAbi(),reqQueryTransHandle.getGroupId(),reqQueryTransHandle.getUserAddress());
        log.info("transHandleLocal end  useTime:{}", Duration.between(startTime, Instant.now()).toMillis());
        return obj;
    }
}
