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

package com.webank.webase.transaction.contract;

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.contract.entity.ReqDeployInfo;
import com.webank.webase.transaction.frontinterface.FrontInterfaceService;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.entity.RspUserInfo;
import com.webank.webase.transaction.trans.TransService;
import com.webank.webase.transaction.util.ContractAbiUtil;
import com.webank.webase.transaction.util.JsonUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.abi.FunctionEncoder;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.protocol.core.methods.response.AbiDefinition;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * ContractService.
 * 
 */
@Slf4j
@Service
public class ContractService {

    @Autowired
    TransService transService;
    @Autowired
    private KeyStoreService keyStoreService;
    @Autowired
    FrontInterfaceService frontInterfaceService;

    /**
     * contract deploy.
     * 
     * @param req parameter
     * @return
     */
    public TransactionReceipt deploy(ReqDeployInfo req) throws Exception {
        int chainId = req.getChainId();
        int groupId = req.getGroupId();
        // check sign user id
        String signUserId = req.getSignUserId();
        RspUserInfo rspUserInfo = keyStoreService.checkSignUserId(signUserId);
        if (rspUserInfo == null) {
            throw new BaseException(ConstantCode.SIGN_USERID_ERROR);
        }
        // check parameters
        String contractAbi = JsonUtils.toJSONString(req.getContractAbi());
        List<Object> params = req.getFuncParam();
        AbiDefinition abiDefinition = null;
        try {
            abiDefinition = ContractAbiUtil.getAbiDefinition(contractAbi);
        } catch (Exception e) {
            log.error("abi parse error. abi:{}", contractAbi);
            throw new BaseException(ConstantCode.ABI_PARSE_ERROR);
        }
        List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
        if (funcInputTypes.size() != params.size()) {
            log.warn("deploy fail. funcInputTypes:{}, params:{}", funcInputTypes, params);
            throw new BaseException(ConstantCode.IN_FUNCPARAM_ERROR);
        }
        // Constructor encode
        String encodedConstructor = "";
        if (funcInputTypes.size() > 0) {
            List<Type> finalInputs = ContractAbiUtil.inputFormat(funcInputTypes, params);
            encodedConstructor = FunctionEncoder.encodeConstructor(finalInputs);
        }
        // data sign
        String data = req.getBytecodeBin() + encodedConstructor;
        String signMsg = transService.signMessage(chainId, groupId, req.getSignUserId(),
                rspUserInfo.getEncryptType(), "", data);
        if (StringUtils.isBlank(signMsg)) {
            throw new BaseException(ConstantCode.DATA_SIGN_ERROR);
        }
        // send transaction
        TransactionReceipt receipt =
                frontInterfaceService.sendSignedTransaction(chainId, groupId, signMsg, true);
        return receipt;
    }
}
