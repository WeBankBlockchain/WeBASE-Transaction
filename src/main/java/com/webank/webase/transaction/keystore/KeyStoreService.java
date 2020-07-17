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

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.Constants;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.entity.EncodeInfo;
import com.webank.webase.transaction.keystore.entity.ReqNewUser;
import com.webank.webase.transaction.keystore.entity.ReqUserInfo;
import com.webank.webase.transaction.keystore.entity.RspUserInfo;
import com.webank.webase.transaction.keystore.entity.SignInfo;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.SignRestTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.EncryptType;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * KeyStoreService.
 *
 */
@Slf4j
@Service
public class KeyStoreService {
    @Autowired
    SignRestTools signRestTools;
    @Autowired
    private Constants properties;

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
    public String getSignData(EncodeInfo param) throws BaseException {
        String url = String.format(SignRestTools.SIGN_URL, properties.getSignServer());
        log.debug("getSignData url:{}", url);
        ResponseEntity response = signRestTools.postToSign(url, param, ResponseEntity.class);
        if (response.getCode() == 0) {
            SignInfo signInfo = CommonUtils.object2JavaBean(response.getData(), SignInfo.class);
            return signInfo.getSignDataStr();
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
        if (StringUtils.isBlank(signUserId)) {
            log.error("signUserId is null");
            return false;
        }
        String url = String.format(SignRestTools.SIGN_USERINFO_URL, properties.getSignServer(),
                signUserId);
        log.debug("checkSignUserId url:{}", url);
        ResponseEntity response = signRestTools.getFromSign(url, ResponseEntity.class);
        if (response.getCode() == 0) {
            RspUserInfo rspUserInfo =
                    CommonUtils.object2JavaBean(response.getData(), RspUserInfo.class);
            if (rspUserInfo.getEncryptType() == EncryptType.encryptType) {
                return true;
            }
        }
        return false;
    }

    /**
     * newUser.
     *
     * @param reqNewUser
     * @return
     */
    public Object newUser(ReqNewUser reqNewUser) throws BaseException {
        String url = String.format(SignRestTools.SIGN_NEW_USER_URL, properties.getSignServer());
        log.debug("newUser url:{}", url);
        Object response = signRestTools.postToSign(url, reqNewUser, Object.class);
        return response;
    }

    /**
     * deleteUser.
     *
     * @param reqUserInfo
     * @return
     */
    public Object deleteUser(ReqUserInfo reqUserInfo) throws BaseException {
        String url = String.format(SignRestTools.SIGN_URL_USER, properties.getSignServer());
        log.debug("deleteUser url:{}", url);
        Object response = signRestTools.deleteToSign(url, reqUserInfo, Object.class);
        return response;
    }

    /**
     * getUserListByAppId.
     *
     * @param appId
     * @return
     */
    public Object getUserListByAppId(String appId, Integer pageNumber, Integer pageSize)
            throws BaseException {
        String url = String.format(SignRestTools.SIGN_GET_USER_LIST_URL, properties.getSignServer(),
                appId, pageNumber, pageSize);
        Object response = signRestTools.getFromSign(url, Object.class);
        return response;
    }


    /**
     * checkSignUserId.
     *
     * @param signUserId business id of user in sign
     * @return
     */
    public Object getUserBySignUserId(String signUserId) throws BaseException {
        String url = String.format(SignRestTools.SIGN_USERINFO_URL, properties.getSignServer(),
                signUserId);
        log.debug("getUserBySignUserId url:{}", url);
        Object response = signRestTools.getFromSign(url, Object.class);
        return response;
    }
}
