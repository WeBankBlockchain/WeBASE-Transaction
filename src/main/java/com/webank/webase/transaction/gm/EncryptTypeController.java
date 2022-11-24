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

package com.webank.webase.transaction.gm;

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ResponseEntity;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * return encrypt type to web 0 is standard, 1 is guomi
 */
@Api(value = "/encrypt", tags = "encrypt type interface(standard/guomi)")
@Slf4j
@RestController
@RequestMapping(value = "encrypt")
public class EncryptTypeController {
    @Autowired
    private BcosSDK bcosSDK;

    @GetMapping("/{groupId}")
    public ResponseEntity getEncryptType(@PathVariable("groupId") String groupId) {
        log.info("getEncryptType groupId:{}", groupId);
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        int encrypt = bcosSDK.getClient(groupId).getCryptoType();
        log.info("getEncryptType:{}", encrypt);
        response.setData(encrypt);
        return response;
    }
}
