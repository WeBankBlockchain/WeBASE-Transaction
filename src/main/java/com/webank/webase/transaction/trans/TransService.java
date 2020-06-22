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

package com.webank.webase.transaction.trans;

import com.fasterxml.jackson.databind.JsonNode;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.config.Web3Config;
import com.webank.webase.transaction.contract.ContractMapper;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.entity.EncodeInfo;
import com.webank.webase.transaction.keystore.entity.KeyStoreInfo;
import com.webank.webase.transaction.keystore.entity.SignType;
import com.webank.webase.transaction.trans.entity.ReqTransCallInfo;
import com.webank.webase.transaction.trans.entity.ReqTransSendInfo;
import com.webank.webase.transaction.trans.entity.TransInfoDto;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.ContractAbiUtil;
import com.webank.webase.transaction.util.LogUtils;
import com.webank.webase.transaction.util.JsonUtils;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
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
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.RawTransaction;
import org.fisco.bcos.web3j.crypto.ExtendedRawTransaction;
import org.fisco.bcos.web3j.crypto.EncryptType;
import org.fisco.bcos.web3j.crypto.TransactionEncoder;
import org.fisco.bcos.web3j.crypto.ExtendedTransactionEncoder;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.crypto.gm.GenCredential;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.DefaultBlockParameterName;
import org.fisco.bcos.web3j.protocol.core.Request;
import org.fisco.bcos.web3j.protocol.core.methods.request.Transaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.AbiDefinition;
import org.fisco.bcos.web3j.protocol.core.methods.response.SendTransaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.utils.Numeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
    private TransMapper transMapper;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private ThreadPoolTaskExecutor transExecutor;
    @Autowired
    private ConstantProperties properties;
    @Autowired
    private KeyStoreService keyStoreService;

    /**
     * save transaction request data.
     * 
     * @param req parameter
     * @return
     */
    public ResponseEntity save(ReqTransSendInfo req) throws BaseException {
        long startTime = System.currentTimeMillis();
        // check groupId
        int groupId = req.getGroupId();
        if (!checkGroupId(groupId)) {
            log.warn("save fail. groupId:{} has not been configured", groupId);
            throw new BaseException(ConstantCode.GROUPID_NOT_CONFIGURED);
        }
        // check stateless uuid
        String uuidStateless = req.getUuidStateless();
        TransInfoDto transInfo = transMapper.selectTransInfo(groupId, uuidStateless);
        if (transInfo != null) {
            log.error("save groupId:{} uuidStateless:{} exists", groupId, uuidStateless);
            long endTime = System.currentTimeMillis();
            LogUtils.monitorBusinessLogger().info(ConstantProperties.CODE_BUSINESS_10004,
                    endTime - startTime, ConstantProperties.MSG_BUSINESS_10004);
            throw new BaseException(ConstantCode.UUID_IS_EXISTS);
        }
        // check sign type
        if (!SignType.isInclude(req.getSignType())) {
            log.warn("save fail. signType:{} is not existed", req.getSignType());
            throw new BaseException(ConstantCode.SIGN_TYPE_ERROR);
        }
        // check sign user id
        if (SignType.CLOUDCALL.getValue() == req.getSignType()) {
            String signUserId = req.getSignUserId();
            if (StringUtils.isBlank(signUserId)) {
                log.warn("deploy fail. sign user id is empty");
                throw new BaseException(ConstantCode.SIGN_USERID_EMPTY);
            } else {
                boolean result = keyStoreService.checkSignUserId(signUserId);
                if (!result) {
                    throw new BaseException(ConstantCode.SIGN_USERID_ERROR);
                }
            }
        }
        String uuidDeploy = req.getUuidDeploy();
        String contractAddress = req.getContractAddress();
        List<Object> abiList = req.getContractAbi();
        // check request style
        if (StringUtils.isBlank(uuidDeploy)
                && (StringUtils.isBlank(contractAddress) || abiList.isEmpty())) {
            throw new BaseException(ConstantCode.ADDRESS_ABI_EMPTY);
        }
        // check if contract has been deployed
        if (StringUtils.isBlank(contractAddress)) {
            contractAddress = contractMapper.selectContractAddress(groupId, uuidDeploy);
        }
        if (StringUtils.isBlank(contractAddress)) {
            log.warn("save fail. contract has not been deployed");
            throw new BaseException(ConstantCode.CONTRACT_NOT_DEPLOED);
        }
        // check contractAbi
        String contractAbi = "";
        if (abiList.isEmpty()) {
            contractAbi = contractMapper.selectContractAbi(groupId, uuidDeploy);
            if (StringUtils.isBlank(contractAbi)) {
                log.warn("save fail. uuidDeploy:{} abi is not exists", uuidDeploy);
                throw new BaseException(ConstantCode.CONTRACT_ABI_EMPTY);
            }
        } else {
            contractAbi = JsonUtils.toJSONString(abiList);
        }
        // check function
        String funcName = req.getFuncName();
        AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(funcName, contractAbi);
        if (abiDefinition == null) {
            log.warn("save fail. func:{} is not exists", funcName);
            throw new BaseException(ConstantCode.FUNCTION_NOT_EXISTS);
        }
        if (abiDefinition.isConstant()) {
            log.warn("save fail. func:{} is constant", funcName);
            throw new BaseException(ConstantCode.FUNCTION_NOT_CONSTANT);
        }
        // check function parameter
        List<Object> params = req.getFuncParam();
        List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
        if (funcInputTypes.size() != params.size()) {
            log.warn("save fail. funcInputTypes:{}, params:{}", funcInputTypes, params);
            throw new BaseException(ConstantCode.IN_FUNCPARAM_ERROR);
        }
        // check input format
        ContractAbiUtil.inputFormat(funcInputTypes, params);
        // check output format
        List<String> funOutputTypes = ContractAbiUtil.getFuncOutputType(abiDefinition);
        ContractAbiUtil.outputFormat(funOutputTypes);
        // insert db
        TransInfoDto transInfoDto = new TransInfoDto();
        transInfoDto.setGroupId(groupId);
        transInfoDto.setUuidStateless(uuidStateless);
        transInfoDto.setUuidDeploy(uuidDeploy);
        transInfoDto.setContractAbi(contractAbi);
        transInfoDto.setContractAddress(contractAddress);
        transInfoDto.setFuncName(funcName);
        transInfoDto.setFuncParam(JsonUtils.toJSONString(params));
        transInfoDto.setSignType(req.getSignType());
        transInfoDto.setSignUserId(req.getSignUserId());
        transInfoDto.setGmtCreate(new Date());
        transMapper.insertTransInfo(transInfoDto);

        log.info("save end. groupId:{} uuidStateless:{}", groupId, uuidStateless);
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        long endTime = System.currentTimeMillis();
        LogUtils.monitorBusinessLogger().info(ConstantProperties.CODE_BUSINESS_10002,
                endTime - startTime, ConstantProperties.MSG_BUSINESS_10002);
        return response;
    }

    /**
     * transaction query.
     * 
     * @param req parameter
     * @return
     */
    public ResponseEntity call(ReqTransCallInfo req) throws BaseException {
        int groupId = req.getGroupId();
        String uuidDeploy = req.getUuidDeploy();
        String contractAddress = req.getContractAddress();
        List<Object> abiList = req.getContractAbi();
        String funcName = req.getFuncName();
        List<Object> params = req.getFuncParam();
        try {
            // check groupId
            if (!checkGroupId(groupId)) {
                log.warn("call fail. groupId:{} has not been configured", groupId);
                throw new BaseException(ConstantCode.GROUPID_NOT_CONFIGURED);
            }
            // check request style
            if (StringUtils.isBlank(uuidDeploy)
                    && (StringUtils.isBlank(contractAddress) || abiList.isEmpty())) {
                throw new BaseException(ConstantCode.ADDRESS_ABI_EMPTY);
            }
            // check if contract has been deployed
            if (StringUtils.isBlank(contractAddress)) {
                contractAddress = contractMapper.selectContractAddress(groupId, uuidDeploy);
            }
            if (StringUtils.isBlank(contractAddress)) {
                log.warn("save fail. contract has not been deployed");
                throw new BaseException(ConstantCode.CONTRACT_NOT_DEPLOED);
            }
            // check contractAbi
            String contractAbi = "";
            if (abiList.isEmpty()) {
                contractAbi = contractMapper.selectContractAbi(groupId, uuidDeploy);
                if (StringUtils.isBlank(contractAbi)) {
                    log.warn("save fail. uuidDeploy:{} abi is not exists", uuidDeploy);
                    throw new BaseException(ConstantCode.CONTRACT_ABI_EMPTY);
                }
            } else {
                contractAbi = JsonUtils.toJSONString(abiList);
            }
            // check function
            AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(funcName, contractAbi);
            if (abiDefinition == null) {
                log.warn("call fail. func:{} is not exists", funcName);
                throw new BaseException(ConstantCode.FUNCTION_NOT_EXISTS);
            }
            if (!abiDefinition.isConstant()) {
                log.warn("call fail. func:{} is not constant", funcName);
                throw new BaseException(ConstantCode.FUNCTION_MUST_CONSTANT);
            }
            // check function parameter
            List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
            if (funcInputTypes.size() != params.size()) {
                log.warn("call fail. funcInputTypes:{}, params:{}", funcInputTypes, params);
                throw new BaseException(ConstantCode.IN_FUNCPARAM_ERROR);
            }
            // check input format
            List<Type> finalInputs = ContractAbiUtil.inputFormat(funcInputTypes, params);
            // check output format
            List<String> funOutputTypes = ContractAbiUtil.getFuncOutputType(abiDefinition);
            List<TypeReference<?>> finalOutputs = ContractAbiUtil.outputFormat(funOutputTypes);
            // encode function
            Function function = new Function(funcName, finalInputs, finalOutputs);
            String encodedFunction = FunctionEncoder.encode(function);
            String callOutput = web3jMap.get(groupId)
                    .call(Transaction.createEthCallTransaction(keyStoreService.getRandomAddress(),
                            contractAddress, encodedFunction), DefaultBlockParameterName.LATEST)
                    .send().getValue().getOutput();
            List<Type> typeList =
                    FunctionReturnDecoder.decode(callOutput, function.getOutputParameters());
            ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
            if (typeList.size() > 0) {
                response.setData(ContractAbiUtil.callResultParse(funOutputTypes, typeList));
            } else {
                response.setData(typeList);
            }
            return response;
        } catch (IOException e) {
            log.error("call funcName:{} Exception:{}", funcName, e);
            throw new BaseException(ConstantCode.TRANSACTION_QUERY_FAILED);
        }
    }

    /**
     * get transaction event.
     * 
     * @param groupId groupId
     * @param uuidStateless uuid
     * @return
     */
    public ResponseEntity getEvent(int groupId, String uuidStateless) throws BaseException {
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        TransInfoDto transInfo = transMapper.selectTransInfo(groupId, uuidStateless);
        if (transInfo == null) {
            log.warn("getOutput fail. trans is not exist uuidStateless:{}.", uuidStateless);
            throw new BaseException(ConstantCode.TRANS_NOT_EXIST);
        }
        String transHash = transInfo.getTransHash();
        // check if trans has been sent
        if (StringUtils.isBlank(transHash)) {
            log.warn("getEvent fail. trans not sent to the chain uuidStateless:{}.", uuidStateless);
            throw new BaseException(ConstantCode.TRANS_NOT_SENT);
        }
        String contractAbi = transInfo.getContractAbi();
        if (StringUtils.isBlank(contractAbi)) {
            log.warn("getEvent fail. uuidStateless:{} abi is not exists", uuidStateless);
            throw new BaseException(ConstantCode.CONTRACT_ABI_EMPTY);
        }
        List<AbiDefinition> abiList = ContractAbiUtil.getEventAbiDefinitions(contractAbi);
        if (abiList.isEmpty()) {
            log.warn("getEvent fail. uuidStateless:{} event is not exists", uuidStateless);
            throw new BaseException(ConstantCode.EVENT_NOT_EXISTS);
        }
        try {
            // get TransactionReceipt
            TransactionReceipt receipt = web3jMap.get(groupId).getTransactionReceipt(transHash)
                    .send().getTransactionReceipt().get();
            Object result = ContractAbiUtil.receiptParse(receipt, abiList);
            response.setData(result);
        } catch (IOException e) {
            log.error("getEvent getTransactionReceipt fail. transHash:{} ", transHash);
            throw new BaseException(ConstantCode.NODE_REQUEST_FAILED);
        }
        return response;
    }

    /**
     * get transaction output.
     * 
     * @param groupId groupId
     * @param uuidStateless uuid
     * @return
     */
    public ResponseEntity getOutput(int groupId, String uuidStateless) throws BaseException {
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        TransInfoDto transInfo = transMapper.selectTransInfo(groupId, uuidStateless);
        if (transInfo == null) {
            log.warn("getOutput fail. trans is not exist uuidStateless:{}.", uuidStateless);
            throw new BaseException(ConstantCode.TRANS_NOT_EXIST);
        }
        String transOutput = transInfo.getTransOutput();
        // check if trans has been sent
        if (StringUtils.isBlank(transOutput)) {
            log.warn("getOutput fail. trans output is empty uuidStateless:{}.", uuidStateless);
            throw new BaseException(ConstantCode.TRANS_OUTPUT_EMPTY);
        }
        String contractAbi = transInfo.getContractAbi();
        if (StringUtils.isBlank(contractAbi)) {
            log.warn("getOutput fail. uuidStateless:{} abi is not exists", uuidStateless);
            throw new BaseException(ConstantCode.CONTRACT_ABI_EMPTY);
        }
        // get AbiDefinition
        AbiDefinition abiDefinition =
                ContractAbiUtil.getAbiDefinition(transInfo.getFuncName(), contractAbi);
        // check output format
        List<String> funOutputTypes = ContractAbiUtil.getFuncOutputType(abiDefinition);
        List<TypeReference<?>> finalOutputs = ContractAbiUtil.outputFormat(funOutputTypes);
        // encode function
        Function function = new Function(transInfo.getFuncName(), null, finalOutputs);
        List<Type> typeList = FunctionReturnDecoder.decode(transInfo.getTransOutput(),
                function.getOutputParameters());
        if (typeList.size() > 0) {
            response.setData(ContractAbiUtil.callResultParse(funOutputTypes, typeList));
        } else {
            response.setData(typeList);
        }
        return response;
    }

    /**
     * get transaction info.
     * 
     * @param groupId groupId
     * @param uuidStateless uuid
     * @return
     */
    public ResponseEntity getTransInfo(int groupId, String uuidStateless)
            throws BaseException {
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        TransInfoDto transInfo = transMapper.selectTransInfo(groupId, uuidStateless);
        if (transInfo == null) {
            log.warn("getTransactionHash fail. trans is not exist uuidStateless:{}.",
                    uuidStateless);
            throw new BaseException(ConstantCode.TRANS_NOT_EXIST);
        }
        response.setData(transInfo);
        return response;
    }

    /**
     * handleTransInfo.
     * 
     * @param transInfoList transInfoList
     */
    public void handleTransInfo(List<TransInfoDto> transInfoList) {
        for (TransInfoDto transInfoDto : transInfoList) {
            try {
                Thread.sleep(properties.getSleepTime());
                transExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        transSend(transInfoDto);
                    }
                });
            } catch (RejectedExecutionException e) {
                log.error("schedule threadPool is full", e);
            } catch (InterruptedException e) {
                log.error("schedule InterruptedException", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * transaction send.
     * 
     * @param transInfoDto transaction info
     */
    public void transSend(TransInfoDto transInfoDto) {
        log.debug("transSend transInfoDto:{}", JsonUtils.toJSONString(transInfoDto));
        Long id = transInfoDto.getId();
        log.info("transSend id:{}", id);
        int groupId = transInfoDto.getGroupId();
        int requestCount = transInfoDto.getRequestCount();
        int signType = transInfoDto.getSignType();
        try {
            // check status
            int status = transMapper.selectStatus(id, transInfoDto.getGmtCreate());
            if (status == 1) {
                log.debug("transSend id:{} has successed.", id);
                return;
            }
            // requestCount + 1
            transMapper.updateRequestCount(id, requestCount + 1, transInfoDto.getGmtCreate());
            // check requestCount
            if (requestCount == properties.getRequestCountMax()) {
                log.warn("transSend id:{} has reached limit:{}", id,
                        properties.getRequestCountMax());
                LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0004,
                        ConstantProperties.MSG_ABNORMAL_S0004);
                return;
            }

            String contractAbi = transInfoDto.getContractAbi();
            String contractAddress = transInfoDto.getContractAddress();
            String funcName = transInfoDto.getFuncName();
            List<Object> params = JsonUtils.toJavaObjectList(transInfoDto.getFuncParam(), Object.class);

            // get function abi
            AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(funcName, contractAbi);
            // input format
            List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
            List<Type> finalInputs = ContractAbiUtil.inputFormat(funcInputTypes, params);
            // output format
            List<String> funOutputTypes = ContractAbiUtil.getFuncOutputType(abiDefinition);
            List<TypeReference<?>> finalOutputs = ContractAbiUtil.outputFormat(funOutputTypes);
            // encode function
            Function function = new Function(funcName, finalInputs, finalOutputs);
            String encodedFunction = FunctionEncoder.encode(function);
            // data sign
            String signMsg = signMessage(groupId, signType, transInfoDto.getSignUserId(),
                    contractAddress, encodedFunction);
            if (StringUtils.isBlank(signMsg)) {
                return;
            }
            // send transaction
            final CompletableFuture<TransactionReceipt> transFuture = new CompletableFuture<>();
            sendMessage(groupId, signMsg, transFuture);
            TransactionReceipt receipt =
                    transFuture.get(properties.getTransMaxWait(), TimeUnit.SECONDS);
            transInfoDto.setTransHash(receipt.getTransactionHash());
            transInfoDto.setTransOutput(receipt.getOutput());
            transInfoDto.setReceiptStatus(receipt.isStatusOK());
            if (receipt.isStatusOK()) {
                transInfoDto.setHandleStatus(1);
            }
            transMapper.updateHandleStatus(transInfoDto);
        } catch (Exception e) {
            log.error("fail transSend id:{}", id, e);
            LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0002,
                    ConstantProperties.MSG_ABNORMAL_S0002);
        }
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
    public String signMessage(int groupId, int signType, String signUserId, String contractAddress,
            String data) throws IOException, BaseException {
        Random r = new Random();
        BigInteger randomid = new BigInteger(250, r);
        BigInteger blockLimit = web3jMap.get(groupId).getBlockNumberCache();
        String versionContent = web3jMap.get(groupId).getNodeVersion().sendForReturnString();
        String signMsg = "";
        if (versionContent.contains("2.0.0-rc1") || versionContent.contains("release-2.0.1")) {
            RawTransaction rawTransaction = RawTransaction.createTransaction(randomid,
                    ConstantProperties.GAS_PRICE, ConstantProperties.GAS_LIMIT, blockLimit,
                    contractAddress, BigInteger.ZERO, data);
            if (signType == SignType.LOCALCONFIG.getValue()) {
                Credentials credentials = GenCredential.create(properties.getPrivateKey());
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                signMsg = Numeric.toHexString(signedMessage);
            } else if (signType == SignType.LOCALRANDOM.getValue()) {
                KeyStoreInfo keyStoreInfo = keyStoreService.getKey();
                Credentials credentials = GenCredential.create(keyStoreInfo.getPrivateKey());
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                signMsg = Numeric.toHexString(signedMessage);
            } else if (signType == SignType.CLOUDCALL.getValue()) {
                byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction);
                String encodedDataStr = Numeric.toHexString(encodedTransaction);

                EncodeInfo encodeInfo = new EncodeInfo();
                encodeInfo.setEncodedDataStr(encodedDataStr);
                encodeInfo.setSignUserId(signUserId);
                String signDataStr = keyStoreService.getSignData(encodeInfo);
                if (StringUtils.isBlank(signDataStr)) {
                    log.warn("deploySend get sign data error.");
                    return null;
                }

                SignatureData signData = CommonUtils.stringToSignatureData(signDataStr);
                byte[] signedMessage = TransactionEncoder.encode(rawTransaction, signData);
                signMsg = Numeric.toHexString(signedMessage);
            }
        } else {
            JsonNode version = JsonUtils.stringToJsonNode(versionContent);
            if (version == null) {
                log.error("parse json error");
            }
            String chainId = version.get("Chain Id").asText();
            ExtendedRawTransaction extendedRawTransaction =
                    ExtendedRawTransaction.createTransaction(randomid, ConstantProperties.GAS_PRICE,
                            ConstantProperties.GAS_LIMIT, blockLimit, contractAddress,
                            BigInteger.ZERO, data, new BigInteger(chainId),
                            BigInteger.valueOf(groupId), "");
            if (signType == SignType.LOCALCONFIG.getValue()) {
                Credentials credentials = GenCredential.create(properties.getPrivateKey());
                byte[] signedMessage =
                        ExtendedTransactionEncoder.signMessage(extendedRawTransaction, credentials);
                signMsg = Numeric.toHexString(signedMessage);
            } else if (signType == SignType.LOCALRANDOM.getValue()) {
                KeyStoreInfo keyStoreInfo = keyStoreService.getKey();
                Credentials credentials = GenCredential.create(keyStoreInfo.getPrivateKey());
                byte[] signedMessage =
                        ExtendedTransactionEncoder.signMessage(extendedRawTransaction, credentials);
                signMsg = Numeric.toHexString(signedMessage);
            } else if (signType == SignType.CLOUDCALL.getValue()) {
                byte[] encodedTransaction =
                        ExtendedTransactionEncoder.encode(extendedRawTransaction);
                String encodedDataStr = Numeric.toHexString(encodedTransaction);

                EncodeInfo encodeInfo = new EncodeInfo();
                encodeInfo.setEncodedDataStr(encodedDataStr);
                encodeInfo.setSignUserId(signUserId);
                String signDataStr = keyStoreService.getSignData(encodeInfo);
                if (StringUtils.isBlank(signDataStr)) {
                    log.warn("deploySend get sign data error.");
                    return null;
                }

                SignatureData signData = CommonUtils.stringToSignatureData(signDataStr);
                byte[] signedMessage =
                        ExtendedTransactionEncoder.encode(extendedRawTransaction, signData);
                signMsg = Numeric.toHexString(signedMessage);
            }
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
            final CompletableFuture<TransactionReceipt> future) throws IOException {
        Request<?, SendTransaction> request = web3jMap.get(groupId).sendRawTransaction(signMsg);
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
     * delete transaction data.
     */
    public void deleteDataSchedule() {
        transMapper.deletePartData(properties.getKeepDays());
    }
}
