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

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.contract.ContractMapper;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.entity.EncodeInfo;
import com.webank.webase.transaction.keystore.entity.SignType;
import com.webank.webase.transaction.trans.entity.ReqTransCallInfo;
import com.webank.webase.transaction.trans.entity.ReqTransSendInfo;
import com.webank.webase.transaction.trans.entity.TransInfoDto;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.ContractAbiUtil;
import com.webank.webase.transaction.util.JsonUtils;
import com.webank.webase.transaction.util.LogUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.fisco.bcos.sdk.jni.common.JniException;
import org.fisco.bcos.sdk.jni.utilities.tx.TransactionBuilderJniObj;
import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.client.protocol.response.Call;
import org.fisco.bcos.sdk.v3.codec.ContractCodec;
import org.fisco.bcos.sdk.v3.codec.ContractCodecException;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.codec.wrapper.ABIDefinition;
import org.fisco.bcos.sdk.v3.codec.wrapper.ABIDefinitionFactory;
import org.fisco.bcos.sdk.v3.codec.wrapper.ContractABIDefinition;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.crypto.signature.SignatureResult;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.transaction.codec.decode.RevertMessageParser;
import org.fisco.bcos.sdk.v3.transaction.codec.decode.TransactionDecoderService;
import org.fisco.bcos.sdk.v3.transaction.codec.encode.TransactionEncoderService;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessor;
import org.fisco.bcos.sdk.v3.transaction.manager.TransactionProcessorFactory;
import org.fisco.bcos.sdk.v3.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.v3.transaction.pusher.TransactionPusherService;
import org.fisco.bcos.sdk.v3.utils.Hex;
import org.fisco.bcos.sdk.v3.utils.Numeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * TransService.
 */
@Slf4j
@Service
public class TransService {
    @Autowired
    private BcosSDK bcosSDK;
    @Autowired
    private Client rpcClient;
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

    private static final int USE_SOLIDITY = 1;
    private static final int USE_WASM = 2;
    private static final int USE_WASM_DEPLOY = 10;

    /**
     * save transaction request data.
     *
     * @param req parameter
     * @return
     */
    public ResponseEntity save(ReqTransSendInfo req) throws BaseException {
        long startTime = System.currentTimeMillis();
        // check groupId
        String groupId = req.getGroupId();
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
        ABIDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(funcName, contractAbi);
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
        String groupId = req.getGroupId();
        String uuidDeploy = req.getUuidDeploy();
        String contractAddress = req.getContractAddress();
        List<Object> abiList = req.getContractAbi();
        String funcName = req.getFuncName();
        List<Object> params = req.getFuncParam();
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
        ABIDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(funcName, contractAbi);
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

        byte[] encodeFunction = this.encodeFunction2ByteArr(contractAbi, funcName, params, groupId);
        List<Type> typeList = this.handleCall(groupId, keyStoreService.getKeyPairFromFile(groupId).getAddress(),
                contractAddress, encodeFunction, contractAbi, funcName);
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED, typeList);
        return response;
    }

    /**
     * migrate from webase-front
     *
     * @param groupId
     * @param userAddress
     * @param contractAddress
     * @param encodedFunction
     * @param abiStr
     * @param funcName
     * @return decoded type list
     * //  [
     * //    {
     * //      "value": "Hello, World!",
     * //      "typeAsString": "string"
     * //    }
     * //  ]
     */
    public List<Type> handleCall(String groupId, String userAddress, String contractAddress,
                                 byte[] encodedFunction, String abiStr, String funcName) throws BaseException {

        Client client = bcosSDK.getClient(groupId);
        Pair<String, String> chainIdAndGroupId = TransactionProcessorFactory.getChainIdAndGroupId(client);
        TransactionProcessor transactionProcessor = new TransactionProcessor(client,
                keyStoreService.getRandomKeypair(groupId), groupId, chainIdAndGroupId.getLeft());
        Call.CallOutput callOutput = transactionProcessor
                .executeCall(userAddress, contractAddress, encodedFunction)
                .getCallResult();
        // if error
        if (callOutput.getStatus() != 0) {
            Tuple2<Boolean, String> parseResult =
                    RevertMessageParser.tryResolveRevertMessage(callOutput.getStatus(), callOutput.getOutput());
            log.error("call contract error:{}", parseResult);
            String parseResultStr = parseResult.getValue1() ? parseResult.getValue2() : "call contract error of status" + callOutput.getStatus();
            throw new BaseException(ConstantCode.TRANSACTION_QUERY_FAILED.getCode(), parseResultStr);
        } else {
            ContractCodec abiCodec = new ContractCodec(bcosSDK.getClient(groupId).getCryptoSuite(), client.isWASM());
            try {
                log.debug("========= callOutput.getOutput():{}", callOutput.getOutput());
                List<Type> typeList = abiCodec.decodeMethodAndGetOutputObject(abiStr, funcName, callOutput.getOutput());
                // bytes类型转十六进制
                // todo output is byte[] or string  Hex.decode
                log.info("call contract res:{}", JsonUtils.objToString(typeList));
                return typeList;
            } catch (ContractCodecException e) {
                log.error("handleCall decode call output fail:[]", e);
                throw new BaseException(ConstantCode.CONTRACT_TYPE_DECODED_ERROR);
            }
        }
    }

    /**
     * get transaction event.
     *
     * @param groupId       groupId
     * @param uuidStateless uuid
     * @return
     */
    public ResponseEntity getEvent(String groupId, String uuidStateless) throws BaseException {
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
        List<ABIDefinition> abiList = ContractAbiUtil.getEventAbiDefinitions(contractAbi);
        if (abiList.isEmpty()) {
            log.warn("getEvent fail. uuidStateless:{} event is not exists", uuidStateless);
            throw new BaseException(ConstantCode.EVENT_NOT_EXISTS);
        }
        // get TransactionReceipt
        Client client = bcosSDK.getClient(groupId);
        TransactionReceipt receipt = client.getTransactionReceipt(transHash, false).getTransactionReceipt();
        TransactionDecoderService transactionDecoderService = new TransactionDecoderService(client.getCryptoSuite(), false);
        // decode receipt's event
        TransactionResponse transactionResponse = transactionDecoderService.decodeReceiptWithoutValues(contractAbi, receipt);
        response.setData(transactionResponse);
        return response;
    }

    /**
     * get transaction output.
     *
     * @param groupId       groupId
     * @param uuidStateless uuid
     * @return
     */
    public ResponseEntity getOutput(String groupId, String uuidStateless) throws BaseException {
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        TransInfoDto transInfo = transMapper.selectTransInfo(groupId, uuidStateless);
        if (transInfo == null) {
            log.warn("getOutput fail. trans is not exist uuidStateless:{}.", uuidStateless);
            throw new BaseException(ConstantCode.TRANS_NOT_EXIST);
        }
        String transOutput = transInfo.getTransOutput();
        String funcName = transInfo.getFuncName();
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

        Client client = bcosSDK.getClient(groupId);
        ContractCodec abiCodec = new ContractCodec(client.getCryptoSuite(), false);
        try {
            List<Type> typeList = abiCodec.decodeMethodAndGetOutputObject(contractAbi, funcName, transOutput);
            // bytes类型转十六进制
            // todo output is byte[] or string  Hex.decode
            log.info("call contract res:{}", JsonUtils.objToString(typeList));
            response.setData(typeList);
        } catch (ContractCodecException e) {
            log.error("handleCall decode call output fail:[]", e);
            throw new BaseException(ConstantCode.CONTRACT_TYPE_DECODED_ERROR);
        }
        return response;
    }

    /**
     * get transaction info.
     *
     * @param groupId       groupId
     * @param uuidStateless uuid
     * @return
     */
    public ResponseEntity getTransInfo(String groupId, String uuidStateless)
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
        String groupId = transInfoDto.getGroupId();
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

            byte[] encodedFunction = this.encodeFunction2ByteArr(contractAbi, funcName, params, groupId);

            // data sign
            String signMsg = signMessage(groupId, signType, transInfoDto.getSignUserId(),
                    contractAddress, encodedFunction, false);
            if (StringUtils.isBlank(signMsg)) {
                return;
            }
            // send transaction
            TransactionReceipt receipt = sendMessage(groupId, signMsg);
//            TransactionReceipt receipt = transFuture.get(properties.getTransMaxWait(), TimeUnit.SECONDS);

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
     * signMessage to create raw transaction and encode data
     *
     * @param groupId         id
     * @param contractAddress info
     * @param data            info
     * @return
     */
    public String signMessage(String groupId, int signType, String signUserId, String contractAddress,
                              byte[] data, boolean isDeploy) {
        log.info("signMessage data:{}", Hex.toHexString(data));
        Client client = bcosSDK.getClient(groupId);
        // to encode raw tx
        Pair<String, String> chainIdAndGroupId = TransactionProcessorFactory.getChainIdAndGroupId(client);
        long rawTransaction = 0L;
        String encodedTransaction = "";
        String transactionDataHash = "";
        try {
            rawTransaction = TransactionBuilderJniObj
                    .createTransactionData(groupId, chainIdAndGroupId.getLeft(),
                            contractAddress, Hex.toHexString(data), "", client.getBlockLimit().longValue());
            encodedTransaction = TransactionBuilderJniObj.encodeTransactionData(rawTransaction);
            transactionDataHash = client.getCryptoSuite().hash(encodedTransaction);
            log.debug("signMessage rawTransaction:{}", JsonUtils.objToString(rawTransaction));
        } catch (JniException e) {
            log.error("createTransactionData jni error ", e);
        }
        TransactionEncoderService encoderService = new TransactionEncoderService(client.getCryptoSuite());
        String signMsg = "";
        try{
            if (signType == SignType.LOCALCONFIG.getValue()) {
                CryptoKeyPair cryptoKeyPair = keyStoreService.getKeyPairFromFile(groupId);
                signMsg = encoderService.encodeAndSign(rawTransaction, cryptoKeyPair, USE_SOLIDITY);
            } else if (signType == SignType.LOCALRANDOM.getValue()) {
                CryptoKeyPair cryptoKeyPair = keyStoreService.getRandomKeypair(groupId);
                signMsg = encoderService.encodeAndSign(rawTransaction, cryptoKeyPair, USE_SOLIDITY);
            } else if (signType == SignType.CLOUDCALL.getValue()) {
                SignatureResult signData = this.requestSignForSign(encodedTransaction, signUserId, groupId);
                int mark = client.isWASM() ? USE_WASM : USE_SOLIDITY;
                if (client.isWASM() && isDeploy) {
                    mark = USE_WASM_DEPLOY;
                }
                log.info("mark {}", mark);
                String transactionDataHashSignedData = Hex.toHexString(signData.encode());
                signMsg = TransactionBuilderJniObj.createSignedTransaction(rawTransaction,
                        transactionDataHashSignedData,
                        transactionDataHash, mark);
            }
        } catch (JniException e) {
            log.error("createSignedTransactionData jni error:", e);
        }

        return signMsg;
    }

    /**
     * sign by
     * @param encodedDataStr
     * @param signUserId
     * @return
     */
    public SignatureResult requestSignForSign(String encodedDataStr, String signUserId, String groupId) {
        EncodeInfo encodeInfo = new EncodeInfo();
        encodeInfo.setSignUserId(signUserId);
        encodeInfo.setEncodedDataStr(encodedDataStr);

        Instant startTime = Instant.now();
        String signDataStr = keyStoreService.getSignData(encodeInfo);
        log.info("get requestSignForSign cost time: {}",
                Duration.between(startTime, Instant.now()).toMillis());
        SignatureResult signData = CommonUtils.stringToSignatureData(signDataStr,
                bcosSDK.getClient(groupId).getCryptoSuite().cryptoTypeConfig);
        return signData;
    }

    /**
     * send message to node.
     *
     * @param signMsg signMsg
     */
    public TransactionReceipt sendMessage(String groupId, String signMsg) {
        Client client = bcosSDK.getClient(groupId);
        TransactionPusherService txPusher = new TransactionPusherService(client);
        log.info("sendMessage signMsg:{}", signMsg);
        TransactionReceipt receipt = txPusher.push(signMsg);
        log.info("sendMessage receipt:{}", JsonUtils.objToString(receipt));
        this.decodeReceiptMsg(client, receipt);
        return receipt;
    }

    /**
     * sdk仅用于了预编译合约，其余解析需要自行完成
     *
     * @param client
     * @param receipt
     */
    public void decodeReceiptMsg(Client client, TransactionReceipt receipt) {
        // decode receipt
        TransactionDecoderService txDecoder = new TransactionDecoderService(client.getCryptoSuite(), client.isWASM());
        String receiptMsg = txDecoder.decodeReceiptStatus(receipt).getReceiptMessages();
        receipt.setMessage(receiptMsg);
    }

    /**
     * check groupId.
     *
     * @param groupId info
     * @return
     */
    public boolean checkGroupId(String groupId) {
        List<String> connList = rpcClient.getGroupList().getResult().getGroupList();
        for (String gId : connList) {
            if (gId.equals(groupId)) {
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


    /**
     * get encoded function for /trans/query-transaction
     *
     * @param abiStr
     * @param funcName
     * @param funcParam
     * @return
     */
    public byte[] encodeFunction2ByteArr(String abiStr, String funcName, List<Object> funcParam, String groupId) throws BaseException {

        funcParam = funcParam == null ? new ArrayList<>() : funcParam;
        this.validFuncParam(abiStr, funcName, funcParam, groupId);
        log.debug("abiStr:{} ,funcName:{},funcParam {},groupID {}", abiStr, funcName,
                funcParam, groupId);
        ContractCodec abiCodec = new ContractCodec(bcosSDK.getClient(groupId).getCryptoSuite(), false);
        byte[] encodeFunction;

        try {
            encodeFunction = abiCodec.encodeMethod(abiStr, funcName, funcParam);
        } catch (ContractCodecException e) {
            log.error("transHandleWithSign encode fail:[]", e);
            throw new BaseException(ConstantCode.CONTRACT_TYPE_ENCODED_ERROR);
        }
        log.debug("encodeFunction2Str encodeFunction:{}", encodeFunction);
        return encodeFunction;
    }

    public String encodeFunction2Str(String abiStr, String funcName, List<Object> funcParam, String groupId) throws BaseException {
        byte[] encodeFunctionByteArr = this.encodeFunction2ByteArr(abiStr, funcName, funcParam, groupId);
        return Numeric.toHexString(encodeFunctionByteArr);
    }

    private ABIDefinition getABIDefinition(String abiStr, String functionName, String groupId) throws BaseException {
        ABIDefinitionFactory factory = new ABIDefinitionFactory(bcosSDK.getClient(groupId).getCryptoSuite());

        ContractABIDefinition contractABIDefinition = factory.loadABI(abiStr);
        List<ABIDefinition> abiDefinitionList = contractABIDefinition.getFunctions()
                .get(functionName);
        if (abiDefinitionList.isEmpty()) {
            throw new BaseException(ConstantCode.FUNCTION_NOT_EXISTS);
        }
        // abi only contain one function, so get first one
        ABIDefinition function = abiDefinitionList.get(0);
        return function;
    }

    /**
     * check input
     *
     * @param contractAbiStr
     * @param funcName
     * @param funcParam
     * @param groupId
     */
    private void validFuncParam(String contractAbiStr, String funcName, List<Object> funcParam, String groupId) throws BaseException {
        ABIDefinition abiDefinition = this.getABIDefinition(contractAbiStr, funcName, groupId);
        List<ABIDefinition.NamedType> inputTypeList = abiDefinition.getInputs();
        if (inputTypeList.size() != funcParam.size()) {
            log.error("validFuncParam param not match");
            throw new BaseException(ConstantCode.IN_FUNCPARAM_ERROR);
        }
        for (int i = 0; i < inputTypeList.size(); i++) {
            String type = inputTypeList.get(i).getType();
            if (type.startsWith("bytes")) {
                if (type.contains("[][]")) {
                    // todo bytes[][]
                    log.warn("validFuncParam param, not support bytes 2d array or more");
                    return;
                }
                // if not bytes[], bytes or bytesN
                if (!type.endsWith("[]")) {
                    // update funcParam
                    String bytesHexStr = (String) (funcParam.get(i));
                    byte[] inputArray = Numeric.hexStringToByteArray(bytesHexStr);
                    // bytesN: bytes1, bytes32 etc.
                    if (type.length() > "bytes".length()) {
                        int bytesNLength = Integer.parseInt(type.substring("bytes".length()));
                        if (inputArray.length != bytesNLength) {
                            log.error("validFuncParam param of bytesN size not match");
                            throw new BaseException(ConstantCode.IN_FUNCPARAM_ERROR);
                        }
                    }
                    // replace hexString with array
                    funcParam.set(i, inputArray);
                } else {
                    // if bytes[] or bytes32[]
                    List<String> hexStrArray = (List<String>) (funcParam.get(i));
                    List<byte[]> bytesArray = new ArrayList<>(hexStrArray.size());
                    for (int j = 0; j < hexStrArray.size(); j++) {
                        String bytesHexStr = hexStrArray.get(j);
                        byte[] inputArray = Numeric.hexStringToByteArray(bytesHexStr);
                        // check: bytesN: bytes1, bytes32 etc.
                        if (type.length() > "bytes[]".length()) {
                            // bytes32[] => 32[]
                            String temp = type.substring("bytes".length());
                            // 32[] => 32
                            int bytesNLength = Integer
                                    .parseInt(temp.substring(0, temp.length() - 2));
                            if (inputArray.length != bytesNLength) {
                                log.error("validFuncParam param of bytesN size not match");
                                throw new BaseException(
                                        ConstantCode.IN_FUNCPARAM_ERROR);
                            }
                        }
                        bytesArray.add(inputArray);
                    }
                    // replace hexString with array
                    funcParam.set(i, bytesArray);
                }
            }
        }
    }

}
