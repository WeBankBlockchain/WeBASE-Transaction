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
package com.webank.webase.transaction.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fisco.bcos.web3j.abi.TypeReference;
import org.fisco.bcos.web3j.abi.datatypes.DynamicArray;
import org.fisco.bcos.web3j.abi.datatypes.Type;
import org.fisco.bcos.web3j.protocol.core.methods.response.AbiDefinition;
import org.fisco.bcos.web3j.protocol.core.methods.response.AbiDefinition.NamedType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.exception.BaseException;

/**
 * ContractAbiUtil.
 * 
 */
public class ContractAbiUtil {
	
	public static void saveSolFile(String toolDir, String contractName, byte[] contractArr) throws Exception {
		File file = new File("");
		if (file.exists()) {
			file.delete();
			file.createNewFile();
		} else {
			file.createNewFile();
		}
		
		FileOutputStream outputStream = new FileOutputStream(file);
		outputStream.write(contractArr);
		outputStream.flush();
		outputStream.close();
		return;
	}

	/**
     * get constructor abi info.
     * 
     * @param contractAbi contractAbi
     * @return
     */
    public static AbiDefinition getAbiDefinition(String contractAbi) {
    	JSONArray abiArr = JSONArray.parseArray(contractAbi);
    	AbiDefinition result = null;
    	for (Object object : abiArr) {
            AbiDefinition abiDefinition = JSON.parseObject(object.toString(), AbiDefinition.class);
            if (ConstantProperties.TYPE_CONSTRUCTOR.equals(abiDefinition.getType())) {
                result = abiDefinition;
                break;
            }
        }
        return result;
    }
    
    /**
     * get function abi info.
     * 
     * @param name name
     * @param contractAbi contractAbi
     * @return
     */
    public static AbiDefinition getAbiDefinition(String name, String contractAbi) {
    	JSONArray abiArr = JSONArray.parseArray(contractAbi);
    	AbiDefinition result = null;
    	for (Object object : abiArr) {
    		AbiDefinition abiDefinition = JSON.parseObject(object.toString(), AbiDefinition.class);
    		if (ConstantProperties.TYPE_FUNCTION.equals(abiDefinition.getType())
    				&& name.equals(abiDefinition.getName())) {
    			result = abiDefinition;
    			break;
    		}
    	}
    	return result;
    }
    
    /**
     * getFuncInputType.
     * 
     * @param abiDefinition abiDefinition
     * @return
     */
    public static List<String> getFuncInputType(AbiDefinition abiDefinition) {
    	List<String> inputList = new ArrayList<>();
    	if (abiDefinition != null) {
    		List<NamedType> inputs = abiDefinition.getInputs();
    		for (NamedType input : inputs) {
    			inputList.add(input.getType());
    		}
    	}
        return inputList;
    }
    
    /**
     * getFuncOutputType.
     * 
     * @param abiDefinition abiDefinition
     * @return
     */
    public static List<String> getFuncOutputType(AbiDefinition abiDefinition) {
    	List<String> outputList = new ArrayList<>();
    	List<NamedType> outputs = abiDefinition.getOutputs();
        for (NamedType output : outputs) {
            outputList.add(output.getType());
        }
    	return outputList;
    }
    
    /**
     * input parameter format.
     * 
     * @param funcInputTypes list
     * @param params list
     * @return
     */
    public static List<Type> inputFormat(List<String> funcInputTypes, List<Object> params)
            throws BaseException {
        List<Type> finalInputs = new ArrayList<>();
        for (int i = 0; i < funcInputTypes.size(); i++) {
            Class<? extends Type> inputType = null;
            Object input = null;
            if (funcInputTypes.get(i).indexOf("[") != -1
                    && funcInputTypes.get(i).indexOf("]") != -1) {
                List<Object> arrList =
                        new ArrayList<>(Arrays.asList(params.get(i).toString().split(",")));
                List<Type> arrParams = new ArrayList<>();
                for (int j = 0; j < arrList.size(); j++) {
                    inputType = ContractTypeUtil.getType(
                            funcInputTypes.get(i).substring(0, funcInputTypes.get(i).indexOf("[")));
                    input = ContractTypeUtil.parseByType(
                            funcInputTypes.get(i).substring(0, funcInputTypes.get(i).indexOf("[")),
                            arrList.get(j).toString());
                    arrParams.add(ContractTypeUtil.encodeString(input.toString(), inputType));
                }
                finalInputs.add(new DynamicArray<>(arrParams));
            } else {
                inputType = ContractTypeUtil.getType(funcInputTypes.get(i));
                input = ContractTypeUtil.parseByType(funcInputTypes.get(i),
                        params.get(i).toString());
                finalInputs.add(ContractTypeUtil.encodeString(input.toString(), inputType));
            }
        }
        return finalInputs;
    }
    
    /**
     * output parameter format.
     * 
     * @param funOutputTypes list
     * @return
     */
    public static List<TypeReference<?>> outputFormat(List<String> funOutputTypes)
            throws BaseException {
        List<TypeReference<?>> finalOutputs = new ArrayList<>();
        for (int i = 0; i < funOutputTypes.size(); i++) {
            Class<? extends Type> outputType = null;
            TypeReference<?> typeReference = null;
            if (funOutputTypes.get(i).indexOf("[") != -1
                    && funOutputTypes.get(i).indexOf("]") != -1) {
                typeReference = ContractTypeUtil.getArrayType(
                        funOutputTypes.get(i).substring(0, funOutputTypes.get(i).indexOf("[")));
            } else {
                outputType = ContractTypeUtil.getType(funOutputTypes.get(i));
                typeReference = TypeReference.create(outputType);
            }
            finalOutputs.add(typeReference);
        }
        return finalOutputs;
    }
    
    /**
     * ethCall Result Parse.
     * 
     * @param funOutputTypes list
     * @param typeList list
     * @param response response
     * @return
     */
    public static ResponseEntity callResultParse(List<String> funOutputTypes, List<Type> typeList,
            ResponseEntity response) throws BaseException {
        if (funOutputTypes.size() == typeList.size()) {
            List<Object> ressult = new ArrayList<>();
            for (int i = 0; i < funOutputTypes.size(); i++) {
                Class<? extends Type> outputType = null;
                Object value = null;
                if (funOutputTypes.get(i).indexOf("[") != -1
                        && funOutputTypes.get(i).indexOf("]") != -1) {
                    List<Object> values = new ArrayList<>();
                    List<Type> results = (List<Type>) typeList.get(i).getValue();
                    for (int j = 0; j < results.size(); j++) {
                        outputType = ContractTypeUtil.getType(funOutputTypes.get(i).substring(0,
                                funOutputTypes.get(i).indexOf("[")));
                        value = ContractTypeUtil.decodeResult(results.get(j), outputType);
                        values.add(value);
                    }
                    ressult.add(values);
                } else {
                    outputType = ContractTypeUtil.getType(funOutputTypes.get(i));
                    value = ContractTypeUtil.decodeResult(typeList.get(i), outputType);
                    ressult.add(value);
                }
            }
            response.setData(JSON.parse(JSON.toJSONString(ressult)));
        }
        return response;
    }
}
