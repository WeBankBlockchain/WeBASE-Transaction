/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.webank.webase.transaction.base.BaseResponse;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.EncodeInfo;
import com.webank.webase.transaction.keystore.KeyStoreInfo;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.SignType;
import com.webank.webase.transaction.trans.TransService;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.ContractAbiUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * TransService.
 * 
 */
@Slf4j
@Service
public class ContractService {
	@Autowired
	Map<Integer,Web3j> web3jMap;
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
     * @param req parameter
     * @return
     * @throws BaseException
     * @throws IOException 
     */
    public BaseResponse compile(MultipartFile zipFile) throws BaseException, IOException {
    	BaseResponse baseRsp = new BaseResponse(ConstantCode.RET_SUCCEED);
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
        	String contractName = solFile.getName().substring(0, solFile.getName().lastIndexOf("."));
        	// compile
        	SolidityCompiler.Result res =
        	        SolidityCompiler.compile(solFile, true, Options.ABI, Options.BIN);
        	// compile result
        	CompilationResult result = CompilationResult.parse(res.output);
        	List<ContractMetadata> contracts = result.getContracts();
        	CompileInfo compileInfo = new CompileInfo();
        	compileInfo.setContractName(contractName);
			compileInfo.setContractBin(result.getContract(contractName).bin);
			compileInfo.setContractAbi(JSONArray.parseArray(result.getContract(contractName).abi));
    		compileInfos.add(compileInfo);
        }
    	baseRsp.setData(compileInfos);
		return baseRsp;
    }
    
	/**
	 * contract deploy.
	 * 
	 * @param req parameter
	 * @return
	 * @throws BaseException
	 */
	public BaseResponse deploy(ReqDeploy req) throws BaseException {
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
            log.error("save groupId:{} uuid:{} DuplicateKeyException:{}", groupId, uuid,
                    e);
            throw new BaseException(ConstantCode.UUID_DEPLOY_IS_EXISTS);
        }
        BaseResponse baseRsp = new BaseResponse(ConstantCode.RET_SUCCEED);
        log.info("deploy end. groupId:{} uuid:{}", groupId, uuid);
        return baseRsp;
    }

    /** 
     * getAddress.
     * 
     * @param groupId groupId
     * @param uuid uuid
     * @return
     * @throws BaseException 
     */
    public BaseResponse getAddress(int groupId, String uuidDeploy) throws BaseException {
    	BaseResponse baseRsp = new BaseResponse(ConstantCode.RET_SUCCEED);
    	// check if contract has been deployed
    	String contractAddress = contractMapper.selectContractAddress(groupId, uuidDeploy);
		if (StringUtils.isBlank(contractAddress)) {
			log.warn("getAddress fail. contract has not been deployed", contractAddress);
			throw new BaseException(ConstantCode.CONTRACT_NOT_DEPLOED);
		}
    	baseRsp.setData(contractAddress);
    	return baseRsp;
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
    	log.info("sendTrans deployInfoDto:{}", JSON.toJSONString(deployInfoDto));
    	Long id = deployInfoDto.getId();
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
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    randomid, ConstantProperties.GAS_PRICE, ConstantProperties.GAS_LIMIT, blockLimit,
                    "", BigInteger.ZERO, data);
            
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
            TransactionReceipt receipt = transFuture.get(properties.getTransMaxWait(), TimeUnit.SECONDS);
            deployInfoDto.setContractAddress(receipt.getContractAddress());
            deployInfoDto.setTransHash(receipt.getTransactionHash());
            deployInfoDto.setReceiptStatus(receipt.isStatusOK());
            contractMapper.updateHandleStatus(deployInfoDto);
        } catch (Exception e) {
            log.error("fail deploySend id:{}", id, e);
        }
    }
}
