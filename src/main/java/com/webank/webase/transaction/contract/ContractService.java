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

package com.webank.webase.transaction.contract;

import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.contract.entity.CompileInfo;
import com.webank.webase.transaction.contract.entity.DeployInfoDto;
import com.webank.webase.transaction.contract.entity.ReqDeployInfo;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.entity.SignType;
import com.webank.webase.transaction.trans.TransService;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.ContractAbiUtil;
import com.webank.webase.transaction.util.JsonUtils;
import com.webank.webase.transaction.util.LogUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.web3j.abi.FunctionEncoder;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.crypto.EncryptType;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.AbiDefinition;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.solc.compiler.CompilationResult;
import org.fisco.solc.compiler.SolidityCompiler;
import org.fisco.solc.compiler.CompilationResult.ContractMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static org.fisco.solc.compiler.SolidityCompiler.Options.ABI;
import static org.fisco.solc.compiler.SolidityCompiler.Options.BIN;
/**
 * ContractService.
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
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        String path = new File("temp").getAbsolutePath();
        // clear temp folder
        CommonUtils.deleteFiles(path);
        // unzip
        CommonUtils.unZipFiles(zipFile, path);
        // get sol files
        File solFileList = new File(path);
        File[] solFiles = solFileList.listFiles();
        if (solFiles == null || solFiles.length == 0) {
            return response;
        }

        // whether use guomi to compile, 1-guomi, 0-ecdsa
        boolean useSM2 = EncryptType.encryptType == 1;
        List<CompileInfo> compileInfos = new ArrayList<>();
        for (File solFile : solFiles) {
            if (!solFile.getName().endsWith(".sol")) {
                continue;
            }
            String contractName =
                    solFile.getName().substring(0, solFile.getName().lastIndexOf("."));
            // compile
            SolidityCompiler.Result res =
                    SolidityCompiler.compile(solFile, useSM2, true, ABI, BIN);
            // check result
            if (res.isFailed()) {
                log.warn("compile fail. contract:{} compile error. {}", contractName, res.getErrors());
                throw new BaseException(ConstantCode.CONTRACT_COMPILE_ERROR.getCode(), res.getErrors());
            }
            // parse result
            CompilationResult result = CompilationResult.parse(res.getOutput());
            List<ContractMetadata> contracts = result.getContracts();
            if (contracts.size() > 0) {
                CompileInfo compileInfo = new CompileInfo();
                compileInfo.setContractName(contractName);
                compileInfo.setContractBin(result.getContract(contractName).bin);
                compileInfo
                        .setContractAbi(JsonUtils.toJavaObjectList(result.getContract(contractName).abi, Object.class));
                compileInfos.add(compileInfo);
            }
        }
        response.setData(compileInfos);
        return response;
    }

    /**
     * contract deploy.
     * 
     * @param req parameter
     * @return
     */
    public ResponseEntity deploy(ReqDeployInfo req) throws BaseException {
        long startTime = System.currentTimeMillis();
        // check groupId
        int groupId = req.getGroupId();
        if (!transService.checkGroupId(groupId)) {
            log.warn("deploy fail. groupId:{} has not been configured", groupId);
            throw new BaseException(ConstantCode.GROUPID_NOT_CONFIGURED);
        }
        // check deploy uuid
        String uuid = req.getUuidDeploy();
        DeployInfoDto deployInfos = contractMapper.selectDeployInfo(groupId, uuid);
        if (deployInfos != null) {
            log.error("deploy groupId:{} uuid:{} is exists", groupId, uuid);
            long endTime = System.currentTimeMillis();
            LogUtils.monitorBusinessLogger().info(ConstantProperties.CODE_BUSINESS_10003,
                    endTime - startTime, ConstantProperties.MSG_BUSINESS_10003);
            throw new BaseException(ConstantCode.UUID_IS_EXISTS);
        }
        // check sign type
        if (!SignType.isInclude(req.getSignType())) {
            log.warn("deploy fail. signType:{} is not existed", req.getSignType());
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
        // check parameters
        String contractAbi = JsonUtils.toJSONString(req.getContractAbi());
        List<Object> params = req.getFuncParam();
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
        deployInfoDto.setContractBin(req.getContractBin());
        deployInfoDto.setContractAbi(contractAbi);
        deployInfoDto.setFuncParam(JsonUtils.toJSONString(params));
        deployInfoDto.setSignType(req.getSignType());
        deployInfoDto.setSignUserId(req.getSignUserId());
        deployInfoDto.setGmtCreate(new Date());
        contractMapper.insertDeployInfo(deployInfoDto);

        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        log.info("deploy end. groupId:{} uuid:{}", groupId, uuid);
        long endTime = System.currentTimeMillis();
        LogUtils.monitorBusinessLogger().info(ConstantProperties.CODE_BUSINESS_10001,
                endTime - startTime, ConstantProperties.MSG_BUSINESS_10001);
        return response;
    }

    /**
     * get contract Address.
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
     * get getDeployInfo.
     * 
     * @param groupId groupId
     * @param uuidDeploy uuid
     * @return
     */
    public ResponseEntity getDeployInfo(int groupId, String uuidDeploy) throws BaseException {
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        // check if contract has been deployed
        DeployInfoDto deployInfo = contractMapper.selectDeployInfo(groupId, uuidDeploy);
        if (deployInfo == null) {
            log.warn("getDeployInfo fail. data not exists uuidDeploy:{}.", uuidDeploy);
            throw new BaseException(ConstantCode.DATA_NOT_EXISTS);
        }
        response.setData(deployInfo);
        return response;
    }

    /**
     * get contract Event.
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
        log.debug("deploySend deployInfoDto:{}", JsonUtils.toJSONString(deployInfoDto));
        Long id = deployInfoDto.getId();
        log.info("deploySend id:{}", id);
        int groupId = deployInfoDto.getGroupId();
        int requestCount = deployInfoDto.getRequestCount();
        int signType = deployInfoDto.getSignType();
        try {
            // check status
            int status = contractMapper.selectStatus(id, deployInfoDto.getGmtCreate());
            if (status == 1) {
                log.info("deploySend id:{} has successed.", id);
                return;
            }
            // requestCount + 1
            contractMapper.updateRequestCount(id, requestCount + 1, deployInfoDto.getGmtCreate());
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
            List<Object> params = JsonUtils.toJavaObjectList(deployInfoDto.getFuncParam(), Object.class);

            // get function abi
            AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(contractAbi);
            List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
            // Constructor encode
            String encodedConstructor = "";
            if (funcInputTypes.size() > 0) {
                List<Type> finalInputs = ContractAbiUtil.inputFormat(funcInputTypes, params);
                encodedConstructor = FunctionEncoder.encodeConstructor(finalInputs);
            }
            // data sign
            String data = contractBin + encodedConstructor;
            String signMsg = transService.signMessage(groupId, signType,
                    deployInfoDto.getSignUserId(), "", data);
            if (StringUtils.isBlank(signMsg)) {
                return;
            }
            // send transaction
            final CompletableFuture<TransactionReceipt> transFuture = new CompletableFuture<>();
            transService.sendMessage(groupId, signMsg, transFuture);
            TransactionReceipt receipt =
                    transFuture.get(properties.getTransMaxWait(), TimeUnit.SECONDS);
            deployInfoDto.setContractAddress(receipt.getContractAddress());
            deployInfoDto.setTransHash(receipt.getTransactionHash());
            deployInfoDto.setReceiptStatus(receipt.isStatusOK());
            if (receipt.isStatusOK()) {
                deployInfoDto.setHandleStatus(1);
            }
            contractMapper.updateHandleStatus(deployInfoDto);
        } catch (Exception e) {
            log.error("fail deploySend id:{}", id, e);
            LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0001,
                    ConstantProperties.MSG_ABNORMAL_S0001);
        }
    }

    /**
     * delete contract deploy data.
     */
    public void deleteDataSchedule() {
        contractMapper.deletePartData(properties.getKeepDays());
    }
}
