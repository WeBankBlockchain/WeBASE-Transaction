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
package com.webank.webase.transaction.trans;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.BaseResponse;
import com.webank.webase.transaction.base.exception.BaseException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * TransController.
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
     * @param transaction send parameter
     * @param result checkResult
     * @return
     * @throws BaseException 
     */
    @ApiOperation(value = "transaction send", notes = "transaction send")
    @ApiImplicitParam(name = "req", value = "transaction info", required = true,
            dataType = "ReqTransSend")
    @PostMapping("/send")
    public BaseResponse send(@Valid @RequestBody ReqTransSend req,
            BindingResult result) throws BaseException {
        log.info("transSend start. req:{}", JSON.toJSONString(req));
        checkParamResult(result);
        return transService.save(req);
    }
    
    @PostMapping("/call")
    public BaseResponse call(@Valid @RequestBody ReqTransCall req,
    		BindingResult result) throws BaseException {
    	log.info("call start. req:{}", JSON.toJSONString(req));
    	checkParamResult(result);
    	return transService.call(req);
    }
}
