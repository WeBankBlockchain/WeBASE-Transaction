/*
 * Copyright 2012-2019 the original author or authors.
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

import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.ECKeyPair;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.utils.Numeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSON;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.LogUtils;
import lombok.extern.slf4j.Slf4j;

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
    private ConstantProperties properties;

    private static final int PUBLIC_KEY_LENGTH_IN_HEX = 128;
    private static final String SIGN_ADDSIGN_URL = "http://%s/WeBASE-Sign/sign";
    private static final String SIGN_USERINFO_URL = "http://%s/WeBASE-Sign/user/%s/userInfo";

    /**
     * getKey.
     * 
     * @return
     */
    public KeyStoreInfo getKey() throws BaseException {
        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            String publicKey = Numeric.toHexStringWithPrefixZeroPadded(keyPair.getPublicKey(),
                    PUBLIC_KEY_LENGTH_IN_HEX);
            String privateKey = Numeric.toHexStringNoPrefix(keyPair.getPrivateKey());
            String address = "0x" + Keys.getAddress(publicKey);

            KeyStoreInfo keyStoreInfo = new KeyStoreInfo();
            keyStoreInfo.setPublicKey(publicKey);
            keyStoreInfo.setPrivateKey(privateKey);
            keyStoreInfo.setAddress(address);

            return keyStoreInfo;
        } catch (Exception e) {
            log.error("createEcKeyPair fail.");
            throw new BaseException(ConstantCode.SYSTEM_ERROR);
        }
    }
    
    /**
     * getAddress.
     * 
     * @return
     */
    public String getAddress() throws BaseException {
        try {
            String privateKey = properties.getPrivateKey();
            Credentials credentials = Credentials.create(privateKey);
            return credentials.getAddress();
        } catch (Exception e) {
            log.error("getAddress fail.");
            throw new BaseException(ConstantCode.SYSTEM_ERROR);
        }
    }

    /**
     * getSignDate from sign service.
     * 
     * @param params params
     * @return
     */
    public String getSignDate(EncodeInfo params) {
        try {
            SignInfo signInfo = new SignInfo();
            String url = String.format(SIGN_ADDSIGN_URL, properties.getSignServer());
            log.info("getSignDate url:{}", url);
            HttpHeaders headers = CommonUtils.buildHeaders();
            HttpEntity<String> formEntity =
                    new HttpEntity<String>(JSON.toJSONString(params), headers);
            ResponseEntity response =
                    restTemplate.postForObject(url, formEntity, ResponseEntity.class);
            log.info("getSignDate response:{}", JSON.toJSONString(response));
            if (response.getCode() == 0) {
                signInfo = CommonUtils.object2JavaBean(response.getData(), SignInfo.class);
            }
            return signInfo.getSignDataStr();
        } catch (Exception e) {
            log.error("getSignDate exception", e);
            LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0005,
                    ConstantProperties.MSG_ABNORMAL_S0005);
        }
        return null;
    }
    
    /**
     * checkSignUserId.
     * 
     * @param userId userId
     * @return
     */
    public boolean checkSignUserId(int userId) {
        try {
            String url = String.format(SIGN_USERINFO_URL, properties.getSignServer(), userId);
            log.info("checkSignUserId url:{}", url);
            ResponseEntity response =
                    restTemplate.getForObject(url, ResponseEntity.class);
            log.info("checkSignUserId response:{}", JSON.toJSONString(response));
            if (response.getCode() == 0) {
                return true;
            }
        } catch (Exception e) {
            log.error("checkSignUserId exception", e);
        }
        return false;
    }
}
