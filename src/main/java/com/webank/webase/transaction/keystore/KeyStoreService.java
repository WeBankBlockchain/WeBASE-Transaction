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

package com.webank.webase.transaction.keystore;

import com.alibaba.fastjson.JSON;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.Constants;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.entity.EncodeInfo;
import com.webank.webase.transaction.keystore.entity.RspUserInfo;
import com.webank.webase.transaction.keystore.entity.SignInfo;
import com.webank.webase.transaction.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.EncryptType;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * KeyStoreService.
 *
 */
@Slf4j
@Service
public class KeyStoreService {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private Constants properties;

    private static final String SIGN_ADDSIGN_URL = "http://%s/WeBASE-Sign/sign";
    private static final String SIGN_USERINFO_URL = "http://%s/WeBASE-Sign/user/%s/userInfo";

    /**
     * get random Address.
     * 
     * @return
     */
    public String getRandomAddress() throws BaseException {
        try {
            Credentials credentials = GenCredential.create();
            return credentials.getAddress();
        } catch (Exception e) {
            log.error("getRandomAddress fail.");
            throw new BaseException(ConstantCode.SYSTEM_ERROR);
        }
    }

    /**
     * getSignDatd from webase-sign service.
     * 
     * @param params params
     * @return
     */
    public String getSignData(EncodeInfo params) {
        try {
            SignInfo signInfo = new SignInfo();
            String url = String.format(SIGN_ADDSIGN_URL, properties.getSignServer());
            log.info("getSignData url:{}", url);
            HttpHeaders headers = CommonUtils.buildHeaders();
            HttpEntity<String> formEntity =
                    new HttpEntity<String>(JSON.toJSONString(params), headers);
            ResponseEntity response =
                    restTemplate.postForObject(url, formEntity, ResponseEntity.class);
            log.debug("getSignData response:{}", JSON.toJSONString(response));
            if (response.getCode() == 0) {
                signInfo = CommonUtils.object2JavaBean(response.getData(), SignInfo.class);
            }
            return signInfo.getSignDataStr();
        } catch (Exception e) {
            log.error("getSignData exception", e);
        }
        return null;
    }

    /**
     * checkSignUserId.
     * 
     * @param signUserId business id of user in sign
     * @return
     */
    public boolean checkSignUserId(String signUserId) throws BaseException {
        try {
            if (StringUtils.isBlank(signUserId)) {
                log.error("signUserId is null");
                return false;
            }
            String url = String.format(SIGN_USERINFO_URL, properties.getSignServer(), signUserId);
            log.info("checkSignUserId url:{}", url);
            ResponseEntity response = restTemplate.getForObject(url, ResponseEntity.class);
            log.debug("checkSignUserId response:{}", JSON.toJSONString(response));
            if (response.getCode() == 0) {
                RspUserInfo rspUserInfo = CommonUtils.object2JavaBean(response.getData(), RspUserInfo.class);
                if (rspUserInfo.getEncryptType() != EncryptType.encryptType) {
                    log.error("signUserId encryptType not match");
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            log.error("checkSignUserId exception", e);
        }
        return false;
    }


}
