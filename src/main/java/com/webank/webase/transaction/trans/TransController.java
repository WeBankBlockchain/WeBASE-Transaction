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

package com.webank.webase.transaction.trans;

import com.webank.webase.transaction.util.JsonUtils;
import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.trans.entity.ReqTransCallInfo;
import com.webank.webase.transaction.trans.entity.ReqTransSendInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
            BindingResult result) throws BaseException {
        log.info("transSend start. transSendInfo:{}", JsonUtils.toJSONString(transSendInfo));
        checkParamResult(result);
        return transService.save(transSendInfo);
    }

    /**
     * transaction call.
     * 
     * @param transCallInfo parameter
     * @param result checkResult
     * @return
     */
    @ApiOperation(value = "transaction call", notes = "transaction call")
    @PostMapping("/call")
    public ResponseEntity call(@Valid @RequestBody ReqTransCallInfo transCallInfo,
            BindingResult result) throws BaseException {
        log.info("call start. transCallInfo:{}", JsonUtils.toJSONString(transCallInfo));
        checkParamResult(result);
        return transService.call(transCallInfo);
    }

    /**
     * get transaction event.
     * 
     * @param groupId id
     * @param uuidStateless uuid
     * @return
     */
    @ApiOperation(value = "getEvent", notes = "get transaction event")
    @GetMapping("/event/{groupId}/{uuidStateless}")
    public ResponseEntity getEvent(@PathVariable("groupId") int groupId,
            @PathVariable("uuidStateless") String uuidStateless) throws BaseException {
        return transService.getEvent(groupId, uuidStateless);
    }

    /**
     * get transaction output.
     * 
     * @param groupId id
     * @param uuidStateless uuid
     * @return
     */
    @ApiOperation(value = "getOutput", notes = "get transaction output")
    @GetMapping("/output/{groupId}/{uuidStateless}")
    public ResponseEntity getOutput(@PathVariable("groupId") int groupId,
            @PathVariable("uuidStateless") String uuidStateless) throws BaseException {
        return transService.getOutput(groupId, uuidStateless);
    }
    
    /**
     * get transaction info.
     * 
     * @param groupId id
     * @param uuidStateless uuid
     * @return
     */
    @ApiOperation(value = "getTransInfo", notes = "get transaction info")
    @GetMapping("/transInfo/{groupId}/{uuidStateless}")
    public ResponseEntity getTransInfo(@PathVariable("groupId") int groupId,
            @PathVariable("uuidStateless") String uuidStateless) throws BaseException {
        return transService.getTransInfo(groupId, uuidStateless);
    }
}
