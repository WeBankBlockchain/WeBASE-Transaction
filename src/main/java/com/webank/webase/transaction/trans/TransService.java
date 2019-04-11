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
package com.webank.webase.transaction.trans;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.channel.client.TransactionSucCallback;
import org.fisco.bcos.web3j.abi.FunctionEncoder;
import org.fisco.bcos.web3j.abi.FunctionReturnDecoder;
import org.fisco.bcos.web3j.abi.TypeReference;
import org.fisco.bcos.web3j.abi.datatypes.Function;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.crypto.Credentials;
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
import org.fisco.bcos.web3j.solidity.compiler.CompilationResult;
import org.fisco.bcos.web3j.solidity.compiler.CompilationResult.ContractMetadata;
import org.fisco.bcos.web3j.solidity.compiler.SolidityCompiler;
import org.fisco.bcos.web3j.solidity.compiler.SolidityCompiler.Options;
import org.fisco.bcos.web3j.utils.Numeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.webank.webase.transaction.base.BaseResponse;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.KeyStoreInfo;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.ContractAbiUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * TransService.
 * 
 */
@Slf4j
@Service
public class TransService {
	@Autowired
	Map<Integer,Web3j> web3jMap;
	@Autowired
    RestTemplate restTemplate;
    @Autowired
    private TransMapper transMapper;
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
    	String path = properties.getSolTempDir();
    	// unzip
    	CommonUtils.unZipFiles(zipFile, path);
    	// get sol files
    	File solFileList = new File(path);
        File[] solFiles = solFileList.listFiles();
        List<CompileInfo> compileInfos = new ArrayList<>();
        for (File solFile : solFiles) {
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
        // delete sol files
        CommonUtils.deleteFiles(path);
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
	public BaseResponse deploy(ReqContractDeploy req) throws BaseException {
    	int groupId = req.getGroupId();
        String uuid = req.getUuid();
        String contractAddress = "";
        try {
        	// check uuid
        	int count = transMapper.checkUuid(groupId, uuid);
        	if (count > 0) {
        		log.warn("deploy fail. uuid:{} is already exists", uuid);
        		throw new BaseException(ConstantCode.UUID_IS_EXISTS);
        	}
        	// inputs format
        	String contractAbi = JSON.toJSONString(req.getContractAbi());
            List<Object> params = req.getFuncParam();
            AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(contractAbi);
            List<String> funcInputTypes = ContractAbiUtil.getFuncInputType(abiDefinition);
            // Constructor encode
            String encodedConstructor = "";
            String contractBin = req.getContractBin();
            if (funcInputTypes != null && funcInputTypes.size() > 0) {
                if (funcInputTypes.size() == params.size()) {
                	List<Type> finalInputs = ContractAbiUtil.inputFormat(funcInputTypes, params);
                    encodedConstructor = FunctionEncoder.encodeConstructor(finalInputs);
                } else {
                    log.warn("deploy fail. funcInputTypes:{}, params:{}", funcInputTypes, params);
                    throw new BaseException(ConstantCode.IN_FUNCPARAM_ERROR);
                }
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
            if (req.getSignType() == SignType.LOCALCONFIG.getValue()) {
            	Credentials credentials = Credentials.create(properties.getPrivateKey());
            	byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            	signMsg = Numeric.toHexString(signedMessage);
            } else if (req.getSignType() == SignType.LOCALRANDOM.getValue()) {
            	KeyStoreInfo keyStoreInfo = keyStoreService.getKey();
            	Credentials credentials = Credentials.create(keyStoreInfo.getPrivateKey());
            	byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            	signMsg = Numeric.toHexString(signedMessage);
            } else if (req.getSignType() == SignType.CLOUDCALL.getValue()) {
                byte[] encodedTransaction = TransactionEncoder.encode(rawTransaction);
                String encodedDataStr = new String(encodedTransaction);
                
                EncodeInfo encodeInfo = new EncodeInfo();
                encodeInfo.setEncodedDataStr(encodedDataStr);
                String signDataStr = getSignDate(encodeInfo);
                if (StringUtils.isBlank(signDataStr)) {
        			log.warn("get sign data error.");
        			throw new BaseException(ConstantCode.GET_SIGN_DATA_ERROR);
        		}
                
                SignatureData signData = CommonUtils.stringToSignatureData(signDataStr);
                byte[] signedMessage = TransactionEncoder.encode(rawTransaction, signData);
                signMsg = Numeric.toHexString(signedMessage);
            } else {
            	throw new BaseException(ConstantCode.SIGN_TYPE_ERROR);
            }
            // send transaction
            final CompletableFuture<TransactionReceipt> transFuture = new CompletableFuture<>();
            sendMessage(groupId, signMsg, transFuture);
            TransactionReceipt receipt = transFuture.get(properties.getTransMaxWait(), TimeUnit.SECONDS);
            contractAddress = receipt.getContractAddress();
            // insert db
        	ContractInfoDto contractInfoDto = new ContractInfoDto();
        	contractInfoDto.setGroupId(groupId);
            contractInfoDto.setUuid(uuid);
            contractInfoDto.setContractBin(contractBin);
            contractInfoDto.setContractAbi(contractAbi);
            contractInfoDto.setContractAddress(contractAddress);
            transMapper.insertConstractInfo(contractInfoDto);
        } catch (IOException | InterruptedException | 
        		ExecutionException | TimeoutException e) {
            log.error("deploy groupId:{} uuid:{} Exception:{}", groupId, uuid, e);
            throw new BaseException(ConstantCode.DEPLOY_FAILED);
        } 
        BaseResponse baseRsp = new BaseResponse(ConstantCode.RET_SUCCEED);
        baseRsp.setData(contractAddress);
        log.info("deploy end. groupId:{} uuid:{} baseRsp:{}", groupId, uuid, baseRsp);
        return baseRsp;
    }

    /** 
     * getAddress.
     * 
     * @param groupId groupId
     * @param uuid uuid
     * @return
     */
    public BaseResponse getAddress(int groupId, String uuid) {
    	BaseResponse baseRsp = new BaseResponse(ConstantCode.RET_SUCCEED);
    	baseRsp.setData(transMapper.selectContractAddress(groupId, uuid));
    	return baseRsp;
    }
    
    /**
     * save transaction request data.
     * 
     * @param req parameter
     * @return
     * @throws BaseException 
     */
    public BaseResponse save(ReqTransSend req) throws BaseException {
        int groupId = req.getGroupId();
        String uuid = req.getUuid();
        String contractAddress = req.getContractAddress();
        String funcName = req.getFuncName();
        List<Object> abiList = req.getContractAbi();
        List<Object> params = req.getFuncParam();
        try {
        	// check sign type
        	if (!SignType.isInclude(req.getSignType())) {
        		log.warn("save fail. uuid:{} is already exists", uuid);
        		throw new BaseException(ConstantCode.SIGN_TYPE_ERROR);
        	}
        	// check contractAbi
        	String contractAbi = "";
        	if (abiList == null || abiList.size() == 0) {
        		contractAbi = transMapper.selectContractAbi(groupId, contractAddress);
        		if (StringUtils.isBlank(contractAbi)) {
        			log.warn("save fail. contractAddress:{} abi is not exists", contractAddress);
        			throw new BaseException(ConstantCode.CONTRACT_ABI_ERROR);
        		}
        	} else {
        		contractAbi = JSON.toJSONString(abiList);
        	}
        	// check function
        	AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(funcName, contractAbi);
        	if (abiDefinition.isConstant()) {
        		log.warn("save fail. func:{} is constant", funcName);
                throw new BaseException(ConstantCode.FUNCTION_NOT_CONSTANT);
            }
        	// check function parameter
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
            transInfoDto.setUuid(uuid);
            transInfoDto.setContractAbi(contractAbi);
            transInfoDto.setContractAddress(contractAddress);
            transInfoDto.setFuncName(funcName);
            transInfoDto.setFuncParam(JSON.toJSONString(params));
            transInfoDto.setSignType(req.getSignType());
            transMapper.insertTransInfo(transInfoDto);
        } catch (DuplicateKeyException e) {
            log.error("save groupId:{} uuid:{} DuplicateKeyException:{}", groupId, uuid,
                    e);
            throw new BaseException(ConstantCode.UUID_IS_EXISTS);
        }
        log.info("save end. groupId:{} uuid:{}", groupId, uuid);
        BaseResponse baseRsp = new BaseResponse(ConstantCode.RET_SUCCEED);
        return baseRsp;
    }
    
    /**
     * transaction query
     * @param req parameter
     * @return
     * @throws BaseException
     */
    public BaseResponse call(ReqTransCall req) throws BaseException {
    	int groupId = req.getGroupId();
    	String contractAddress = req.getContractAddress();
    	String funcName = req.getFuncName();
    	List<Object> abiList = req.getContractAbi();
    	List<Object> params = req.getFuncParam();
    	try {
    		// check contractAbi
    		String contractAbi = "";
    		if (abiList == null || abiList.size() == 0) {
    			contractAbi = transMapper.selectContractAbi(groupId, contractAddress);
    			if (StringUtils.isBlank(contractAbi)) {
    				log.warn("call fail. contractAddress:{} abi is not exists", contractAddress);
    				throw new BaseException(ConstantCode.CONTRACT_ABI_ERROR);
    			}
    		} else {
    			contractAbi = JSON.toJSONString(abiList);
    		}
    		// check function
    		AbiDefinition abiDefinition = ContractAbiUtil.getAbiDefinition(funcName, contractAbi);
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
            KeyStoreInfo keyStoreInfo = keyStoreService.getKey();
            String callOutput = web3jMap.get(groupId).call(
            		Transaction.createEthCallTransaction(keyStoreInfo.getAddress(), 
            				contractAddress, encodedFunction), DefaultBlockParameterName.LATEST)
            		.send().getValue().getOutput();
            List<Type> typeList = FunctionReturnDecoder.decode(callOutput, function.getOutputParameters());
            BaseResponse baseRsp = new BaseResponse(ConstantCode.RET_SUCCEED);
            if (typeList.size() > 0) {
                baseRsp = ContractAbiUtil.callResultParse(funOutputTypes, typeList, baseRsp);
            } else {
                baseRsp.setData(typeList);
            }
            return baseRsp;
    	} catch (IOException e) {
    		log.error("call contractAddress:{} funcName:{} Exception:{}", contractAddress, funcName, e);
    		throw new BaseException(ConstantCode.TRANSACTION_QUERY_FAILED);
    	}
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
     * transSend.
     * 
     * @param transInfoDto transInfoDto
     */
    public void transSend(TransInfoDto transInfoDto) {
    	log.info("sendTrans transInfoDto:{}", JSON.toJSONString(transInfoDto));
    	Long id = transInfoDto.getId();
    	int groupId = transInfoDto.getGroupId();
    	int requestCount = transInfoDto.getRequestCount();
    	int signType = transInfoDto.getSignType();
        try {
            // requestCount + 1
            transMapper.updateRequestCount(id, requestCount + 1);
            // check requestCount
            if (requestCount == properties.getRequestCountMax()) {
                log.warn("transSend id:{} has reached limit:{}", id,
                        properties.getRequestCountMax());
                return;
            }

        	String contractAbi = transInfoDto.getContractAbi();
        	String contractAddress = transInfoDto.getContractAddress();
        	String funcName = transInfoDto.getFuncName();
        	List<Object> params = JSONArray.parseArray(transInfoDto.getFuncParam());
        	
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
            // create transaction
            Random r = new Random();
            BigInteger randomid = new BigInteger(250, r);
            BigInteger blockLimit = web3jMap.get(groupId).getBlockNumberCache();
            RawTransaction rawTransaction = RawTransaction.createTransaction(
            		randomid, ConstantProperties.GAS_PRICE, ConstantProperties.GAS_LIMIT, 
            		blockLimit, contractAddress, BigInteger.ZERO, encodedFunction);
            
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
                String signDataStr = getSignDate(encodeInfo);
                if (StringUtils.isBlank(signDataStr)) {
        			log.warn("transSend get sign data error.");
        			return;
        		}
                
                SignatureData signData = CommonUtils.stringToSignatureData(signDataStr);
                byte[] signedMessage = TransactionEncoder.encode(rawTransaction, signData);
                signMsg = Numeric.toHexString(signedMessage);
            }
            // send transaction
            final CompletableFuture<TransactionReceipt> transFuture = new CompletableFuture<>();
            sendMessage(groupId, signMsg, transFuture);
            TransactionReceipt receipt = transFuture.get(properties.getTransMaxWait(), TimeUnit.SECONDS);
            if (receipt.isStatusOK()) {
            	transMapper.updateTransStatus(id);
            }
        } catch (Exception e) {
            log.error("fail transSend id:{}", id, e);
        }
    }
    
    /**
     * send message to node.
     * 
     * @param signMsg signMsg
     * @param future future
     * @throws IOException
     */
    private void sendMessage(int groupId, String signMsg, final CompletableFuture<TransactionReceipt> future) throws IOException {
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
     * getSignDate from sign service.
     * 
     * @param params params
     * @return
     * @throws BaseException
     */
    public String getSignDate(EncodeInfo params) throws BaseException {
    	try {
    		SignInfo signInfo = new SignInfo();
            String url = properties.getSignServiceUrl();
            log.info("getSignDate url:{}", url);
            HttpHeaders headers = CommonUtils.buildHeaders();
            HttpEntity<String> formEntity =
                    new HttpEntity<String>(JSON.toJSONString(params), headers);
            BaseResponse response = restTemplate.postForObject(url, formEntity, BaseResponse.class);
            log.info("getSignDate response:{}", JSON.toJSONString(response));
            if (response.getCode() == 0) {
            	signInfo =CommonUtils.object2JavaBean(response.getData(), SignInfo.class);
            }
            return signInfo.getSignDataStr();
        } catch (Exception e) {
            log.error("getSignDate exception", e);
        }
		return null;
    }
}
