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

package com.webank.webase.transaction.keystore;

import com.webank.webase.transaction.util.JsonUtils;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.entity.EncodeInfo;
import com.webank.webase.transaction.keystore.entity.KeyStoreInfo;
import com.webank.webase.transaction.keystore.entity.SignInfo;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.ECKeyPair;
import org.fisco.bcos.web3j.crypto.Keys;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.fisco.bcos.web3j.utils.Numeric;
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
    private ConstantProperties properties;

    private static final int PUBLIC_KEY_LENGTH_IN_HEX = 128;
    private static final String SIGN_ADDSIGN_URL = "http://%s/WeBASE-Sign/sign";
    private static final String SIGN_USERINFO_URL = "http://%s/WeBASE-Sign/user/%s/userInfo";

    /**
     * get KeyStoreInfo.
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
     * get user Address.
     * 
     * @return
     */
    public String getAddress() throws BaseException {
        try {
            String privateKey = properties.getPrivateKey();
            Credentials credentials = GenCredential.create(privateKey);
            return credentials.getAddress();
        } catch (Exception e) {
            log.error("getAddress fail.");
            throw new BaseException(ConstantCode.SYSTEM_ERROR);
        }
    }
    
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
                    new HttpEntity<String>(JsonUtils.toJSONString(params), headers);
            ResponseEntity response =
                    restTemplate.postForObject(url, formEntity, ResponseEntity.class);
            log.info("getSignData response:{}", JsonUtils.toJSONString(response));
            if (response.getCode() == 0) {
                signInfo = CommonUtils.object2JavaBean(response.getData(), SignInfo.class);
            }
            return signInfo.getSignDataStr();
        } catch (Exception e) {
            log.error("getSignData exception", e);
            LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0005,
                    ConstantProperties.MSG_ABNORMAL_S0005);
        }
        return null;
    }

    /**
     * checkSignUserId.
     * 
     * @param signUserId business id of user in sign
     * @return
     */
    public boolean checkSignUserId(String signUserId) {
        try {
            String url = String.format(SIGN_USERINFO_URL, properties.getSignServer(), signUserId);
            log.info("checkSignUserId url:{}", url);
            ResponseEntity response = restTemplate.getForObject(url, ResponseEntity.class);
            log.info("checkSignUserId response:{}", JsonUtils.toJSONString(response));
            if (response.getCode() == 0) {
                return true;
            }
        } catch (Exception e) {
            log.error("checkSignUserId exception", e);
        }
        return false;
    }
}
