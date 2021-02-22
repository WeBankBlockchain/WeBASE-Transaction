/*
 * Copyright 2014-2021 the original author or authors.
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

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.Constants;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.frontinterface.FrontInterfaceService;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.entity.EncodeInfo;
import com.webank.webase.transaction.keystore.entity.EncryptType;
import com.webank.webase.transaction.keystore.entity.RspUserInfo;
import com.webank.webase.transaction.trans.entity.ContractFunction;
import com.webank.webase.transaction.trans.entity.ReqSendSignedInfo;
import com.webank.webase.transaction.trans.entity.ReqTransSendInfo;
import com.webank.webase.transaction.trans.entity.TransResultDto;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.ContractAbiUtil;
import com.webank.webase.transaction.util.EncoderUtil;
import com.webank.webase.transaction.util.JsonUtils;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.abi.TypeReference;
import org.fisco.bcos.web3j.abi.datatypes.Function;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.crypto.ExtendedRawTransaction;
import org.fisco.bcos.web3j.crypto.RawTransaction;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.protocol.core.methods.response.AbiDefinition;
import org.fisco.bcos.web3j.protocol.core.methods.response.NodeVersion.Version;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.utils.Numeric;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TransService.
 * 
 */
@Slf4j
@Service
public class TransService {

    @Autowired
    private KeyStoreService keyStoreService;
    @Autowired
    FrontInterfaceService frontInterfaceService;
    @Autowired
    Map<Integer, EncoderUtil> encoderMap;

    /**
     * send transaction.
     * 
     * @param req parameter
     * @return
     */
    public TransResultDto send(ReqTransSendInfo req) throws Exception {
        int chainId = req.getChainId();
        int groupId = req.getGroupId();
        // check sign user id
        String signUserId = req.getSignUserId();
        RspUserInfo rspUserInfo = keyStoreService.checkSignUserId(signUserId);
        if (rspUserInfo == null) {
            log.error("checkSignUserId fail.");
            throw new BaseException(ConstantCode.SIGN_USERID_ERROR);
        }
        // check param ,get function of abi
        ContractFunction contractFunction =
                buildContractFunction(req.getFunctionAbi(), req.getFuncName(), req.getFuncParam());

        // encode function
        Function function = new Function(req.getFuncName(), contractFunction.getFinalInputs(),
                contractFunction.getFinalOutputs());
        String encodedFunction = getEncoderUtil(rspUserInfo.getEncryptType()).encode(function);

        TransResultDto transResultDto = new TransResultDto();
        String contractAddress = req.getContractAddress();
        if (contractFunction.getConstant()) {
            Object response =
                    sendQueryTransaction(encodedFunction, contractAddress, req.getFuncName(),
                            JsonUtils.toJSONString(req.getFunctionAbi()), chainId, groupId);
            transResultDto.setQueryInfo(JsonUtils.objToString(response));
            transResultDto.setConstant(true);
        } else {
            // data sign
            String signMsg = signMessage(chainId, groupId, signUserId, rspUserInfo.getEncryptType(),
                    contractAddress, encodedFunction);
            if (StringUtils.isBlank(signMsg)) {
                throw new BaseException(ConstantCode.DATA_SIGN_ERROR);
            }
            // send transaction
            TransactionReceipt receipt =
                    frontInterfaceService.sendSignedTransaction(chainId, groupId, signMsg, true);
            BeanUtils.copyProperties(receipt, transResultDto);
            transResultDto.setConstant(false);
        }
        return transResultDto;
    }

    /**
     * get transaction by hash.
     * 
     */
    public Object getTransactionByHash(int chainId, int groupId, String transHash) {
        return frontInterfaceService.getTransactionByHash(chainId, groupId, transHash);
    }

    /**
     * get transaction receipt by hash.
     * 
     */
    public Object getTransactionReceipt(int chainId, int groupId, String transHash) {
        return frontInterfaceService.getTransactionReceipt(chainId, groupId, transHash);
    }

    /**
     * send signed transaction.
     * 
     * @param req parameter
     * @return
     */
    public TransResultDto sendSigned(ReqSendSignedInfo req) throws Exception {
        int chainId = req.getChainId();
        int groupId = req.getGroupId();
        // check function name
        AbiDefinition abiDefinition = null;
        try {
            abiDefinition = ContractAbiUtil.getAbiDefinition(req.getFuncName(),
                    JsonUtils.toJSONString(req.getFunctionAbi()));
        } catch (Exception e) {
            log.error("abi parse error. abi:{}", JsonUtils.toJSONString(req.getFunctionAbi()));
            throw new BaseException(ConstantCode.ABI_PARSE_ERROR);
        }
        if (Objects.isNull(abiDefinition)) {
            log.warn("transaction fail. func:{} is not existed", req.getFuncName());
            throw new BaseException(ConstantCode.FUNCTION_NOT_EXISTS);
        }

        TransResultDto transResultDto = new TransResultDto();
        if (abiDefinition.isConstant()) {
            Object response = sendQueryTransaction(req.getSignedOrEncodedStr(),
                    req.getContractAddress(), req.getFuncName(),
                    JsonUtils.toJSONString(req.getFunctionAbi()), chainId, groupId);
            transResultDto.setQueryInfo(JsonUtils.objToString(response));
            transResultDto.setConstant(true);
        } else {
            // send transaction
            TransactionReceipt receipt = frontInterfaceService.sendSignedTransaction(chainId,
                    groupId, req.getSignedOrEncodedStr(), true);
            BeanUtils.copyProperties(receipt, transResultDto);
            transResultDto.setConstant(false);
        }
        return transResultDto;
    }

    public TransactionReceipt sendSignedTransaction(String signedStr, Boolean sync, int chainId,
            int groupId) {
        return frontInterfaceService.sendSignedTransaction(chainId, groupId, signedStr, sync);
    }

    public Object sendQueryTransaction(String encodeStr, String contractAddress, String funcName,
            String functionAbi, int chainId, int groupId) {
        // transaction param
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("contractAddress", contractAddress);
        params.put("funcName", funcName);
        params.put("contractAbi", functionAbi);
        params.put("encodeStr", encodeStr);
        return frontInterfaceService.sendQueryTransaction(chainId, groupId, params);
    }

    /**
     * data sign.
     * 
     * @param groupId id
     * @param signUserId type
     * @param contractAddress info
     * @param data info
     * @return
     */
    public String signMessage(int chainId, int groupId, String signUserId, int encryptType,
            String contractAddress, String data) throws BaseException {
        Random r = new Random();
        BigInteger randomid = new BigInteger(250, r);
        BigInteger blockLimit = frontInterfaceService.getLatestBlockNumber(chainId, groupId)
                .add(Constants.LIMIT_VALUE);
        Version version = frontInterfaceService.getClientVersion(chainId, groupId);
        String signMsg;
        log.info("signMessage encryptType: {}", encryptType);
        EncoderUtil encoderUtil = getEncoderUtil(encryptType);
        if (version.getVersion().contains("2.0.0-rc1")
                || version.getVersion().contains("release-2.0.1")) {
            RawTransaction rawTransaction = RawTransaction.createTransaction(randomid,
                    Constants.GAS_PRICE, Constants.GAS_LIMIT, blockLimit, contractAddress,
                    BigInteger.ZERO, data);
            byte[] encodedTransaction = encoderUtil.encode(rawTransaction);
            String encodedDataStr = Numeric.toHexString(encodedTransaction);

            EncodeInfo encodeInfo = new EncodeInfo();
            encodeInfo.setSignUserId(signUserId);
            encodeInfo.setEncodedDataStr(encodedDataStr);
            String signDataStr = keyStoreService.getSignData(encodeInfo);
            if (StringUtils.isBlank(signDataStr)) {
                log.warn("deploySend get sign data error.");
                return null;
            }

            SignatureData signData = CommonUtils.stringToSignatureData(signDataStr, encryptType);
            byte[] signedMessage = encoderUtil.encode(rawTransaction, signData);
            signMsg = Numeric.toHexString(signedMessage);
        } else {
            String chainID = version.getChainID();
            ExtendedRawTransaction extendedRawTransaction =
                    ExtendedRawTransaction.createTransaction(randomid, Constants.GAS_PRICE,
                            Constants.GAS_LIMIT, blockLimit, contractAddress, BigInteger.ZERO, data,
                            new BigInteger(chainID), BigInteger.valueOf(groupId), "");
            byte[] encodedTransaction = encoderUtil.encode(extendedRawTransaction);
            String encodedDataStr = Numeric.toHexString(encodedTransaction);

            EncodeInfo encodeInfo = new EncodeInfo();
            encodeInfo.setSignUserId(signUserId);
            encodeInfo.setEncodedDataStr(encodedDataStr);

            Instant startTime = Instant.now();
            String signDataStr = keyStoreService.getSignData(encodeInfo);
            log.info("getSignData from sign useTime: {}",
                    Duration.between(startTime, Instant.now()).toMillis());

            if (StringUtils.isBlank(signDataStr)) {
                log.warn("deploySend get sign data error.");
                return null;
            }

            SignatureData signData = CommonUtils.stringToSignatureData(signDataStr, encryptType);
            byte[] signedMessage = encoderUtil.encode(extendedRawTransaction, signData);
            signMsg = Numeric.toHexString(signedMessage);
        }
        return signMsg;
    }

    /**
     * build Function with abi.
     */
    private ContractFunction buildContractFunction(List<Object> functionAbi, String funcName,
            List<Object> params) throws BaseException {
        // check function name
        AbiDefinition abiDefinition = null;
        try {
            abiDefinition =
                    ContractAbiUtil.getAbiDefinition(funcName, JsonUtils.toJSONString(functionAbi));
        } catch (Exception e) {
            log.error("abi parse error. abi:{}", JsonUtils.toJSONString(functionAbi));
            throw new BaseException(ConstantCode.ABI_PARSE_ERROR);
        }
        if (Objects.isNull(abiDefinition)) {
            log.warn("transaction fail. func:{} is not existed", funcName);
            throw new BaseException(ConstantCode.FUNCTION_NOT_EXISTS);
        }

        // input format
        List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
        // check param match inputs
        if (funcInputTypes.size() != params.size()) {
            log.error("load contract function error for function params not fit");
            throw new BaseException(ConstantCode.IN_FUNCPARAM_ERROR);
        }
        List<Type> finalInputs = ContractAbiUtil.inputFormat(funcInputTypes, params);
        // output format
        List<String> funOutputTypes = ContractAbiUtil.getFuncOutputType(abiDefinition);
        List<TypeReference<?>> finalOutputs = ContractAbiUtil.outputFormat(funOutputTypes);

        // build ContractFunction
        ContractFunction cf =
                ContractFunction.builder().funcName(funcName).constant(abiDefinition.isConstant())
                        .inputList(funcInputTypes).outputList(funOutputTypes)
                        .finalInputs(finalInputs).finalOutputs(finalOutputs).build();
        return cf;
    }

    /**
     * get EncoderUtil.
     */
    public EncoderUtil getEncoderUtil(int encryptType) throws BaseException {
        if (!EncryptType.isInclude(encryptType)) {
            throw new BaseException(ConstantCode.INVALID_ENCRYPT_TYPE);
        }
        return encoderMap.get(encryptType);
    }
}
