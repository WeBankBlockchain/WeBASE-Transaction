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

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

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
import org.fisco.bcos.web3j.utils.Numeric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.contract.ContractMapper;
import com.webank.webase.transaction.keystore.EncodeInfo;
import com.webank.webase.transaction.keystore.KeyStoreInfo;
import com.webank.webase.transaction.keystore.KeyStoreService;
import com.webank.webase.transaction.keystore.SignType;
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
public class TransService {
	@Autowired
	Map<Integer,Web3j> web3jMap;
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
     * @throws BaseException 
     */
    public ResponseEntity save(ReqTransSendInfo req) throws BaseException {
    	long startTime = System.currentTimeMillis();
        int groupId = req.getGroupId();
        String uuid = req.getUuidStateless();
        String funcName = req.getFuncName();
        List<Object> abiList = req.getContractAbi();
        List<Object> params = req.getFuncParam();
        try {
        	// check sign type
        	if (!SignType.isInclude(req.getSignType())) {
        		log.warn("save fail. signType:{} is not existed", req.getSignType());
        		throw new BaseException(ConstantCode.SIGN_TYPE_ERROR);
        	}
        	// check if contract has been deployed
        	String contractAddress = contractMapper.selectContractAddress(groupId, req.getUuidDeploy());
    		if (StringUtils.isBlank(contractAddress)) {
    			log.warn("save fail. contract has not been deployed", contractAddress);
    			throw new BaseException(ConstantCode.CONTRACT_NOT_DEPLOED);
    		}
        	// check contractAbi
        	String contractAbi = "";
        	if (abiList == null || abiList.size() == 0) {
        		contractAbi = contractMapper.selectContractAbi(groupId, contractAddress);
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
            transInfoDto.setUuidStateless(uuid);
            transInfoDto.setUuidDeploy(req.getUuidDeploy());
            transInfoDto.setContractAbi(contractAbi);
            transInfoDto.setContractAddress(contractAddress);
            transInfoDto.setFuncName(funcName);
            transInfoDto.setFuncParam(JSON.toJSONString(params));
            transInfoDto.setSignType(req.getSignType());
            transMapper.insertTransInfo(transInfoDto);
        } catch (DuplicateKeyException e) {
            log.error("save groupId:{} uuid:{} DuplicateKeyException:{}", groupId, uuid,
                    e);
            long endTime = System.currentTimeMillis();
            LogUtils.monitorBusinessLogger().info(ConstantProperties.CODE_BUSINESS_10004, 
            		endTime - startTime, ConstantProperties.MSG_BUSINESS_10004);
            throw new BaseException(ConstantCode.UUID_IS_EXISTS);
        }
        log.info("save end. groupId:{} uuid:{}", groupId, uuid);
        ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
        long endTime = System.currentTimeMillis();
        LogUtils.monitorBusinessLogger().info(ConstantProperties.CODE_BUSINESS_10002, 
        		endTime - startTime, ConstantProperties.MSG_BUSINESS_10002);
        return response;
    }
    
    /**
     * transaction query
     * @param req parameter
     * @return
     * @throws BaseException
     */
    public ResponseEntity call(ReqTransCallInfo req) throws BaseException {
    	int groupId = req.getGroupId();
    	String funcName = req.getFuncName();
    	List<Object> abiList = req.getContractAbi();
    	List<Object> params = req.getFuncParam();
    	try {
    		// check if contract has been deployed
    		String contractAddress = contractMapper.selectContractAddress(groupId, req.getUuidDeploy());
    		if (StringUtils.isBlank(contractAddress)) {
    			log.warn("save fail. contract is not deploed", contractAddress);
    			throw new BaseException(ConstantCode.CONTRACT_NOT_DEPLOED);
    		}
    		// check contractAbi
    		String contractAbi = "";
    		if (abiList == null || abiList.size() == 0) {
    			contractAbi = contractMapper.selectContractAbi(groupId, contractAddress);
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
            ResponseEntity response = new ResponseEntity(ConstantCode.RET_SUCCEED);
            if (typeList.size() > 0) {
                response = ContractAbiUtil.callResultParse(funOutputTypes, typeList, response);
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
    	log.debug("transSend transInfoDto:{}", JSON.toJSONString(transInfoDto));
    	Long id = transInfoDto.getId();
    	log.info("transSend id:{}", id);
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
                LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0004, 
                		ConstantProperties.MSG_ABNORMAL_S0004);
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
                String signDataStr = keyStoreService.getSignDate(encodeInfo);
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
            transInfoDto.setTransHash(receipt.getTransactionHash());
            transInfoDto.setReceiptStatus(receipt.isStatusOK());
            transMapper.updateHandleStatus(transInfoDto);
        } catch (Exception e) {
            log.error("fail transSend id:{}", id, e);
            LogUtils.monitorAbnormalLogger().error(ConstantProperties.CODE_ABNORMAL_S0002, 
            		ConstantProperties.MSG_ABNORMAL_S0002);
        }
    }
    
    /**
     * send message to node.
     * 
     * @param signMsg signMsg
     * @param future future
     * @throws IOException
     */
    public void sendMessage(int groupId, String signMsg, final CompletableFuture<TransactionReceipt> future) throws IOException {
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
}
