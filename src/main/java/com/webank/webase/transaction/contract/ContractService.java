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

package com.webank.webase.transaction.contract;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.abi.FunctionEncoder;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.crypto.RawTransaction;
import org.fisco.bcos.web3j.crypto.Sign.SignatureData;
import org.fisco.bcos.web3j.crypto.TransactionEncoder;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.AbiDefinition;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.solidity.compiler.CompilationResult;
import org.fisco.bcos.web3j.solidity.compiler.CompilationResult.ContractMetadata;
import org.fisco.bcos.web3j.solidity.compiler.SolidityCompiler;
import org.fisco.bcos.web3j.solidity.compiler.SolidityCompiler.Options;
import org.fisco.bcos.web3j.utils.Numeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.EncodeInfo;
import com.webank.webase.transaction.keystore.KeyStoreInfo;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.SignType;
import com.webank.webase.transaction.trans.TransService;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.ContractAbiUtil;
import com.webank.webase.transaction.util.LogUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * TransService.
 * 
 */
@Slf4j
@Service
public class ContractService {
    @Autowired
    Map<Integer, Web3j> web3jMap;
    @Autowired
    TransService transService;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private ThreadPoolTaskExecutor transExecutor;
    @Autowired
    private ConstantProperties properties;
    @Autowired
    private KeyStoreService keyStoreService;

    /**
     * contract compile.
     * 
     * @param zipFile file
     * @return
     */
    public ResponseEntity compile(MultipartFile zipFile) throws BaseException, IOException {
        String path = new File("temp").getAbsolutePath();
        // clear temp folder
        CommonUtils.deleteFiles(path);
        // unzip
        CommonUtils.unZipFiles(zipFile, path);
        // get sol files
        File solFileList = new File(path);
        File[] solFiles = solFileList.listFiles();
        List<CompileInfo> compileInfos = new ArrayList<>();
        for (File solFile : solFiles) {
            if (!solFile.getName().endsWith(".sol")) {
                continue;
            }
            String contractName =
                    solFile.getName().substring(0, solFile.getName().lastIndexOf("."));
            // compile
            SolidityCompiler.Result res =
                    SolidityCompiler.compile(solFile, true, Options.ABI, Options.BIN);
            // check result
            if (res.isFailed()) {
                log.warn("compile fail. contract:{} compile error. {}", contractName, res.errors);
                throw new BaseException(ConstantCode.CONTRACT_COMPILE_ERROR.getCode(), res.errors);
            }
            // parse result
            CompilationResult result = CompilationResult.parse(res.output);
            List<ContractMetadata> contracts = result.getContracts();
            if (contracts.size() > 0) {
                CompileInfo compileInfo = new CompileInfo();
                compileInfo.setContractName(contractName);
                compileInfo.setContractBin(result.getContract(contractName).bin);
                compileInfo
                        .setContractAbi(JSONArray.parseArray(result.getContract(contractName).abi));
                compileInfos.add(compileInfo);
            }
        }
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        response.setData(compileInfos);
        return response;
    }

    /**
     * contract deploy.
     * 
     * @param req parameter
     * @return
     * @throws BaseException
     */
    public ResponseEntity deploy(ReqDeployInfo req) throws BaseException {
        long startTime = System.currentTimeMillis();
        int groupId = req.getGroupId();
        String uuid = req.getUuidDeploy();
        String contractAbi = JSON.toJSONString(req.getContractAbi());
        String contractBin = req.getContractBin();
        List<Object> params = req.getFuncParam();
        try {
            // check sign type
            if (!SignType.isInclude(req.getSignType())) {
                log.warn("deploy fail. signType:{} is not existed", req.getSignType());
                throw new BaseException(ConstantCode.SIGN_TYPE_ERROR);
            }
            // check parameters
            AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(contractAbi);
            List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
            if (funcInputTypes.size() != params.size()) {
                log.warn("deploy fail. funcInputTypes:{}, params:{}", funcInputTypes, params);
                throw new BaseException(ConstantCode.IN_FUNCPARAM_ERROR);
            }
            // check input format
            ContractAbiUtil.inputFormat(funcInputTypes, params);
            // insert db
            DeployInfoDto deployInfoDto = new DeployInfoDto();
            deployInfoDto.setGroupId(groupId);
            deployInfoDto.setUuidDeploy(uuid);
            deployInfoDto.setContractBin(contractBin);
            deployInfoDto.setContractAbi(contractAbi);
            deployInfoDto.setFuncParam(JSON.toJSONString(params));
            deployInfoDto.setSignType(req.getSignType());
            contractMapper.insertDeployInfo(deployInfoDto);
        } catch (DuplicateKeyException e) {
            log.error("save groupId:{} uuid:{} DuplicateKeyException:{}", groupId, uuid, e);
            long endTime = System.currentTimeMillis();
            LogUtils.monitorBusinessLogger().info(ConstantProperties.CODE_BUSINESS_10003,
                    endTime - startTime, ConstantProperties.MSG_BUSINESS_10003);
            throw new BaseException(ConstantCode.UUID_DEPLOY_IS_EXISTS);
        }
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        log.info("deploy end. groupId:{} uuid:{}", groupId, uuid);
        long endTime = System.currentTimeMillis();
        LogUtils.monitorBusinessLogger().info(ConstantProperties.CODE_BUSINESS_10001,
                endTime - startTime, ConstantProperties.MSG_BUSINESS_10001);
        return response;
    }

    /**
     * getAddress.
     * 
     * @param groupId groupId
     * @param uuidDeploy uuid
     * @return
     */
    public ResponseEntity getAddress(int groupId, String uuidDeploy) throws BaseException {
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        // check if contract has been deployed
        String contractAddress = contractMapper.selectContractAddress(groupId, uuidDeploy);
        if (StringUtils.isBlank(contractAddress)) {
            log.warn("getAddress fail. contract has not been deployed uuidDeploy:{}.", uuidDeploy);
            throw new BaseException(ConstantCode.CONTRACT_NOT_DEPLOED);
        }
        response.setData(contractAddress);
        return response;
    }
    
    /**
     * getEvent.
     * 
     * @param groupId groupId
     * @param uuidDeploy uuid
     * @return
     */
    public ResponseEntity getEvent(int groupId, String uuidDeploy) throws BaseException {
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        // check if contract has been deployed
        String transHash = contractMapper.selectTxHash(groupId, uuidDeploy);
        if (StringUtils.isBlank(transHash)) {
            log.warn("getEvent fail. contract has not been deployed uuidDeploy:{}.", uuidDeploy);
            throw new BaseException(ConstantCode.CONTRACT_NOT_DEPLOED);
        }
        String contractAbi = contractMapper.selectContractAbi(groupId, uuidDeploy);
        if (StringUtils.isBlank(contractAbi)) {
            log.warn("getEvent fail. uuidDeploy:{} abi is not exists", uuidDeploy);
            throw new BaseException(ConstantCode.CONTRACT_ABI_EMPTY);
        }
        List<AbiDefinition> abiList = ContractAbiUtil.getEventAbiDefinitions(contractAbi);
        if (abiList.isEmpty()) {
            log.warn("getEvent fail. uuidDeploy:{} event is not exists", uuidDeploy);
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
     * handleDeployInfo.
     * 
     * @param deployInfoList deployInfoList
     */
    public void handleDeployInfo(List<DeployInfoDto> deployInfoList) {
        for (DeployInfoDto deployInfoDto : deployInfoList) {
            try {
                Thread.sleep(properties.getSleepTime());
                transExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        deploySend(deployInfoDto);
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
     * deploySend.
     * 
     * @param deployInfoDto deployInfoDto
     */
    public void deploySend(DeployInfoDto deployInfoDto) {
        log.debug("deploySend deployInfoDto:{}", JSON.toJSONString(deployInfoDto));
        Long id = deployInfoDto.getId();
        log.info("deploySend id:{}", id);
        int groupId = deployInfoDto.getGroupId();
        int requestCount = deployInfoDto.getRequestCount();
        int signType = deployInfoDto.getSignType();
        try {
            // requestCount + 1
            contractMapper.updateRequestCount(id, requestCount + 1);
            // check requestCount
            if (requestCount == properties.getRequestCountMax()) {
                log.warn("deploySend id:{} has reached limit:{}", id,
                        properties.getRequestCountMax());
                LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0003,
                        ConstantProperties.MSG_ABNORMAL_S0003);
                return;
            }

            String contractAbi = deployInfoDto.getContractAbi();
            String contractBin = deployInfoDto.getContractBin();
            List<Object> params = JSONArray.parseArray(deployInfoDto.getFuncParam());

            // get function abi
            AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(contractAbi);
            List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
            // Constructor encode
            String encodedConstructor = "";
            if (funcInputTypes.size() > 0) {
                List<Type> finalInputs = ContractAbiUtil.inputFormat(funcInputTypes, params);
                encodedConstructor = FunctionEncoder.encodeConstructor(finalInputs);
            }
            // create transaction
            String data = contractBin + encodedConstructor;
            Random r = new Random();
            BigInteger randomid = new BigInteger(250, r);
            BigInteger blockLimit = web3jMap.get(groupId).getBlockNumberCache();
            RawTransaction rawTransaction =
                    RawTransaction.createTransaction(randomid, ConstantProperties.GAS_PRICE,
                            ConstantProperties.GAS_LIMIT, blockLimit, "", BigInteger.ZERO, data);

            // get sign data
            String signMsg = "";
            if (signType == SignType.LOCALCONFIG.getValue()) {
                Credentials credentials = Credentials.create(properties.getPrivateKey());
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                signMsg = Numeric.toHexString(signedMessage);
            } else if (signType == SignType.LOCALRANDOM.getValue()) {
                KeyStoreInfo keyStoreInfo = keyStoreService.getKey();
                Credentials credentials = Credentials.create(keyStoreInfo.getPrivateKey());
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                signMsg = Numeric.toHexString(signedMessage);
            } else if (signType == SignType.CLOUDCALL.getValue()) {
                byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction);
                String encodedDataStr = new String(encodedTransaction);

                EncodeInfo encodeInfo = new EncodeInfo();
                encodeInfo.setEncodedDataStr(encodedDataStr);
                String signDataStr = keyStoreService.getSignDate(encodeInfo);
                if (StringUtils.isBlank(signDataStr)) {
                    log.warn("deploySend get sign data error.");
                    return;
                }

                SignatureData signData = CommonUtils.stringToSignatureData(signDataStr);
                byte[] signedMessage = TransactionEncoder.encode(rawTransaction, signData);
                signMsg = Numeric.toHexString(signedMessage);
            }
            // send transaction
            final CompletableFuture<TransactionReceipt> transFuture = new CompletableFuture<>();
            transService.sendMessage(groupId, signMsg, transFuture);
            TransactionReceipt receipt =
                    transFuture.get(properties.getTransMaxWait(), TimeUnit.SECONDS);
            deployInfoDto.setContractAddress(receipt.getContractAddress());
            deployInfoDto.setTransHash(receipt.getTransactionHash());
            deployInfoDto.setReceiptStatus(receipt.isStatusOK());
            contractMapper.updateHandleStatus(deployInfoDto);
        } catch (Exception e) {
            log.error("fail deploySend id:{}", id, e);
            LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0001,
                    ConstantProperties.MSG_ABNORMAL_S0001);
        }
    }
}
