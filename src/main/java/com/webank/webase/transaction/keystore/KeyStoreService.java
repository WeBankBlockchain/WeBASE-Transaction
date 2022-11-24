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

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.entity.EncodeInfo;
import com.webank.webase.transaction.keystore.entity.KeyStoreInfo;
import com.webank.webase.transaction.keystore.entity.SignInfo;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.JsonUtils;
import com.webank.webase.transaction.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
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
    @Autowired
    private BcosSDK bcosSDK;

    private static final int PUBLIC_KEY_LENGTH_IN_HEX = 128;
    private static final String SIGN_ADDSIGN_URL = "http://%s/WeBASE-Sign/sign";
    private static final String SIGN_USERINFO_URL = "http://%s/WeBASE-Sign/user/%s/userInfo";

    /**
     * get user Address.
     * 
     * @return
     */
    public CryptoKeyPair getKeyPairFromFile(String groupId) {
        String privateKey = properties.getPrivateKey();
        CryptoKeyPair cryptoKeyPair = bcosSDK.getClient(groupId).getCryptoSuite().loadKeyPair(privateKey);
        return cryptoKeyPair;
    }
    
    /**
     * get random Address.
     * 
     * @return
     */
    public CryptoKeyPair getRandomKeypair(String groupId) {
        CryptoKeyPair cryptoKeyPair = this.getKeypairFromSdk(groupId);
        return cryptoKeyPair;
    }

    /**
     * get random address
     * @param groupId
     * @return
     */
    public String getRandomAddress(String groupId) {
        CryptoKeyPair cryptoKeyPair = this.getRandomKeypair(groupId);
        return cryptoKeyPair.getAddress();
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

    private CryptoKeyPair getKeypairFromSdk(String groupId) {
        return bcosSDK.getClient(groupId).getCryptoSuite().getKeyPairFactory().generateKeyPair();
    }
}
