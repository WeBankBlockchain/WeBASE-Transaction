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

package com.webank.webase.transaction.trans;

import com.alibaba.fastjson.JSON;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.Constants;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.config.Web3Config;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.entity.EncodeInfo;
import com.webank.webase.transaction.trans.entity.ContractFunction;
import com.webank.webase.transaction.trans.entity.ReqTransSendInfo;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.ContractAbiUtil;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.channel.client.TransactionSucCallback;
import org.fisco.bcos.channel.handler.ChannelConnections;
import org.fisco.bcos.web3j.abi.FunctionEncoder;
import org.fisco.bcos.web3j.abi.FunctionReturnDecoder;
import org.fisco.bcos.web3j.abi.TypeReference;
import org.fisco.bcos.web3j.abi.datatypes.Function;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.crypto.ExtendedRawTransaction;
import org.fisco.bcos.web3j.crypto.ExtendedTransactionEncoder;
import org.fisco.bcos.web3j.crypto.RawTransaction;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.crypto.TransactionEncoder;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.DefaultBlockParameterName;
import org.fisco.bcos.web3j.protocol.core.Request;
import org.fisco.bcos.web3j.protocol.core.methods.request.Transaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.AbiDefinition;
import org.fisco.bcos.web3j.protocol.core.methods.response.SendTransaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.utils.Numeric;
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
    Map<Integer, Web3j> web3jMap;
    @Autowired
    Web3Config web3Config;
    @Autowired
    private Constants constants;
    @Autowired
    private KeyStoreService keyStoreService;

    /**
     * send transaction.
     * 
     * @param req parameter
     * @return
     */
    public Object send(ReqTransSendInfo req) throws Exception {
        // check groupId
        int groupId = req.getGroupId();
        if (!checkGroupId(groupId)) {
            log.error("send fail. groupId:{} has not been configured", groupId);
            throw new BaseException(ConstantCode.GROUPID_NOT_CONFIGURED);
        }

        // check param ,get function of abi
        ContractFunction contractFunction =
                buildContractFunction(req.getFunctionAbi(), req.getFuncName(), req.getFuncParam());

        // encode function
        Function function = new Function(req.getFuncName(), contractFunction.getFinalInputs(),
                contractFunction.getFinalOutputs());
        String encodedFunction = FunctionEncoder.encode(function);

        Object response;
        String contractAddress = req.getContractAddress();
        if (contractFunction.getConstant()) {
            String callOutput = web3jMap.get(groupId)
                    .call(Transaction.createEthCallTransaction(keyStoreService.getRandomAddress(),
                            contractAddress, encodedFunction), DefaultBlockParameterName.LATEST)
                    .send().getValue().getOutput();
            List<Type> typeList =
                    FunctionReturnDecoder.decode(callOutput, function.getOutputParameters());
            if (typeList.size() > 0) {
                response =
                        ContractAbiUtil.callResultParse(contractFunction.getOutputList(), typeList);
            } else {
                response = typeList;
            }
        } else {
            // check sign user id
            String signUserId = req.getSignUserId();
            boolean result = keyStoreService.checkSignUserId(signUserId);
            if (!result) {
                throw new BaseException(ConstantCode.SIGN_USERID_ERROR);
            }
            // data sign
            String signMsg = signMessage(groupId, signUserId, contractAddress, encodedFunction);
            if (StringUtils.isBlank(signMsg)) {
                throw new BaseException(ConstantCode.DATA_SIGN_ERROR);
            }
            // send transaction
            final CompletableFuture<TransactionReceipt> transFuture = new CompletableFuture<>();
            sendMessage(groupId, signMsg, transFuture);
            TransactionReceipt receipt =
                    transFuture.get(constants.getTransMaxWait(), TimeUnit.SECONDS);
            response = receipt;
        }
        return response;
    }

    /**
     * get transaction by hash.
     * 
     */
    public Object getTransactionByHash(Integer groupId, String transHash) throws BaseException {
        // check groupId
        if (!checkGroupId(groupId)) {
            log.error("getTransactionByHash fail. groupId:{} has not been configured", groupId);
            throw new BaseException(ConstantCode.GROUPID_NOT_CONFIGURED);
        }

        Object transaction = null;
        try {
            Optional<org.fisco.bcos.web3j.protocol.core.methods.response.Transaction> opt =
                    getWeb3j(groupId).getTransactionByHash(transHash).send().getTransaction();
            if (opt.isPresent()) {
                transaction = opt.get();
            }
        } catch (IOException e) {
            log.error("getTransactionByHash fail. transHash:{} ", transHash);
            throw new BaseException(ConstantCode.NODE_REQUEST_FAILED);
        }
        return transaction;
    }

    /**
     * get transaction receipt by hash.
     * 
     */
    public Object getTransactionReceipt(Integer groupId, String transHash) throws BaseException {
        // check groupId
        if (!checkGroupId(groupId)) {
            log.error("getTransactionReceipt fail. groupId:{} has not been configured", groupId);
            throw new BaseException(ConstantCode.GROUPID_NOT_CONFIGURED);
        }

        Object transactionReceipt = null;
        try {
            Optional<TransactionReceipt> opt = getWeb3j(groupId).getTransactionReceipt(transHash)
                    .send().getTransactionReceipt();
            if (opt.isPresent()) {
                transactionReceipt = opt.get();
            }
        } catch (IOException e) {
            log.error("getTransactionReceipt fail. transHash:{} ", transHash);
            throw new BaseException(ConstantCode.NODE_REQUEST_FAILED);
        }
        return transactionReceipt;
    }

    /**
     * data sign.
     * 
     * @param groupId id
     * @param signType type
     * @param contractAddress info
     * @param data info
     * @return
     */
    public String signMessage(int groupId, String signUserId, String contractAddress, String data)
            throws BaseException {
        Random r = new Random();
        BigInteger randomid = new BigInteger(250, r);

        BigInteger blockLimit = getWeb3j(groupId).getBlockNumberCache();
        String versionContent = Constants.version;
        String signMsg;
        if (versionContent.contains("2.0.0-rc1") || versionContent.contains("release-2.0.1")) {
            RawTransaction rawTransaction = RawTransaction.createTransaction(randomid,
                    Constants.GAS_PRICE, Constants.GAS_LIMIT, blockLimit, contractAddress,
                    BigInteger.ZERO, data);
            byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction);
            String encodedDataStr = Numeric.toHexString(encodedTransaction);

            EncodeInfo encodeInfo = new EncodeInfo();
            encodeInfo.setSignUserId(signUserId);
            encodeInfo.setEncodedDataStr(encodedDataStr);
            String signDataStr = keyStoreService.getSignData(encodeInfo);
            if (StringUtils.isBlank(signDataStr)) {
                log.warn("deploySend get sign data error.");
                return null;
            }

            SignatureData signData = CommonUtils.stringToSignatureData(signDataStr);
            byte[] signedMessage = TransactionEncoder.encode(rawTransaction, signData);
            signMsg = Numeric.toHexString(signedMessage);
        } else {
            String chainId = Constants.chainId;
            ExtendedRawTransaction extendedRawTransaction =
                    ExtendedRawTransaction.createTransaction(randomid, Constants.GAS_PRICE,
                            Constants.GAS_LIMIT, blockLimit, contractAddress, BigInteger.ZERO, data,
                            new BigInteger(chainId), BigInteger.valueOf(groupId), "");
            byte[] encodedTransaction = ExtendedTransactionEncoder.encode(extendedRawTransaction);
            String encodedDataStr = Numeric.toHexString(encodedTransaction);

            EncodeInfo encodeInfo = new EncodeInfo();
            encodeInfo.setSignUserId(signUserId);
            encodeInfo.setEncodedDataStr(encodedDataStr);

            Instant startTime = Instant.now();
            String signDataStr = keyStoreService.getSignData(encodeInfo);
            log.info("get signdatastr cost time: {}",
                    Duration.between(startTime, Instant.now()).toMillis());

            if (StringUtils.isBlank(signDataStr)) {
                log.warn("deploySend get sign data error.");
                return null;
            }

            SignatureData signData = CommonUtils.stringToSignatureData(signDataStr);
            byte[] signedMessage =
                    ExtendedTransactionEncoder.encode(extendedRawTransaction, signData);
            signMsg = Numeric.toHexString(signedMessage);
        }
        return signMsg;
    }

    /**
     * send message to node.
     * 
     * @param signMsg signMsg
     * @param future future
     */
    public void sendMessage(int groupId, String signMsg,
            final CompletableFuture<TransactionReceipt> future) throws IOException, BaseException {
        Request<?, SendTransaction> request = getWeb3j(groupId).sendRawTransaction(signMsg);
        request.setNeedTransCallback(true);
        request.setTransactionSucCallback(new TransactionSucCallback() {
            @Override
            public void onResponse(TransactionReceipt receipt) {
                log.info("onResponse receipt:{}", receipt);
                future.complete(receipt);
                return;
            }
        });
        request.send();
    }

    /**
     * check groupId.
     * 
     * @param groupId info
     * @return
     */
    public boolean checkGroupId(int groupId) {
        List<ChannelConnections> connList = web3Config.getGroupConfig().getAllChannelConnections();
        for (ChannelConnections conn : connList) {
            if (groupId == conn.getGroupId()) {
                return true;
            }
        }
        return false;
    }

    /**
     * get target group's web3j
     * 
     * @param groupId
     * @return
     */
    public Web3j getWeb3j(Integer groupId) throws BaseException {
        if (web3jMap.isEmpty()) {
            log.error("web3jMap is empty, please check your node status.");
            throw new BaseException(ConstantCode.WEB3JMAP_IS_EMPTY);
        }
        Web3j web3j = web3jMap.get(groupId);
        if (Objects.isNull(web3j)) {
            log.error("web3j of {} is null, please check groupId", groupId);
            throw new BaseException(ConstantCode.WEB3J_IS_NULL);
        }
        return web3j;
    }

    /**
     * build Function with abi.
     */
    private ContractFunction buildContractFunction(List<Object> functionAbi, String funcName,
            List<Object> params) throws BaseException {
        // check function name
        AbiDefinition abiDefinition =
                ContractAbiUtil.getAbiDefinition(funcName, JSON.toJSONString(functionAbi));
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
}
