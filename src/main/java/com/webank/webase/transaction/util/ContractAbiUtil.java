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

package com.webank.webase.transaction.util;

import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.base.exception.BaseException;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.wrapper.ABIDefinition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * ContractAbiUtil.
 * 
 */
public class ContractAbiUtil {

    /**
     * saveSolFile.
     * 
     * @param toolDir path
     * @param contractName name
     * @param contractArr content
     */
    public static void saveSolFile(String toolDir, String contractName, byte[] contractArr)
            throws IOException {
        File file = new File("");
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        } else {
            file.createNewFile();
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(contractArr);
            outputStream.flush();
        } catch (IOException e) {
            System.out.println("saveSolFile IOException:" + e.toString());
            throw e;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ex) {
                System.out.println("saveSolFile IOException:" + ex.toString());
            }
        }
        return;
    }

    /**
     * get constructor abi info.
     * 
     * @param contractAbi contractAbi
     * @return
     */
    public static ABIDefinition getAbiDefinition(String contractAbi) {
        List<ABIDefinition> abiArr = JsonUtils.toJavaObjectList(contractAbi, ABIDefinition.class);
        ABIDefinition result = null;
        for (ABIDefinition abiDefinition : abiArr) {
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
    public static ABIDefinition getAbiDefinition(String name, String contractAbi) {
        List<ABIDefinition> abiArr = JsonUtils.toJavaObjectList(contractAbi, ABIDefinition.class);
        ABIDefinition result = null;
        for (ABIDefinition abiDefinition : abiArr) {
            if (ConstantProperties.TYPE_FUNCTION.equals(abiDefinition.getType())
                && name.equals(abiDefinition.getName())) {
                result = abiDefinition;
                break;
            }
        }
        return result;
    }

    /**
     * get event abi info.
     * 
     * @param contractAbi contractAbi
     * @return
     */
    public static List<ABIDefinition> getEventAbiDefinitions(String contractAbi) {
        List<ABIDefinition> abiArr = JsonUtils.toJavaObjectList(contractAbi, ABIDefinition.class);
        List<ABIDefinition> result = new ArrayList<>();
        for (ABIDefinition abiDefinition : abiArr) {
            if (ConstantProperties.TYPE_EVENT.equals(abiDefinition.getType())) {
                result.add(abiDefinition);
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
    public static List<String> getFuncInputType(ABIDefinition abiDefinition) {
        List<String> inputList = new ArrayList<>();
        if (abiDefinition != null) {
            List<ABIDefinition.NamedType> inputs = abiDefinition.getInputs();
            for (ABIDefinition.NamedType input : inputs) {
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
    public static List<String> getFuncOutputType(ABIDefinition abiDefinition) {
        List<String> outputList = new ArrayList<>();
        List<ABIDefinition.NamedType> outputs = abiDefinition.getOutputs();
        for (ABIDefinition.NamedType output : outputs) {
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
            if (funcInputTypes.get(i).contains("[")
                    && funcInputTypes.get(i).contains("]")) {
                List<Object> arrList =
                        new ArrayList<>(Arrays.asList(params.get(i).toString().split(",")));
                inputType = ContractTypeUtil.getType(
                        funcInputTypes.get(i).substring(0, funcInputTypes.get(i).indexOf("[")));
                List<Type> arrParams = new ArrayList<>();
                for (int j = 0; j < arrList.size(); j++) {
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
            if (funOutputTypes.get(i).contains("[")
                    && funOutputTypes.get(i).contains("]")) {
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
     * @return
     */
    public static Object callResultParse(List<String> funOutputTypes, List<Type> typeList)
            throws BaseException {
        if (funOutputTypes.size() == typeList.size()) {
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < funOutputTypes.size(); i++) {
                Class<? extends Type> outputType = null;
                Object value = null;
                if (funOutputTypes.get(i).contains("[")
                        && funOutputTypes.get(i).contains("]")) {
                    List<Object> values = new ArrayList<>();
                    List<Type> results = (List<Type>) typeList.get(i).getValue();
                    for (int j = 0; j < results.size(); j++) {
                        outputType = ContractTypeUtil.getType(funOutputTypes.get(i).substring(0,
                                funOutputTypes.get(i).indexOf("[")));
                        value = ContractTypeUtil.decodeResult(results.get(j), outputType);
                        values.add(value);
                    }
                    result.add(values);
                } else {
                    outputType = ContractTypeUtil.getType(funOutputTypes.get(i));
                    value = ContractTypeUtil.decodeResult(typeList.get(i), outputType);
                    result.add(value);
                }
            }
            return JsonUtils.toJavaObject(JsonUtils.toJSONString(result), Object.class);
        }
        return null;
    }

    /**
     * receiptParse.
     * 
     * @param receipt info
     * @param abiList info
     * @return
     */
//    public static Object receiptParse(TransactionReceipt receipt, List<ABIDefinition> abiList)
//            throws BaseException {
//        Map<String, Object> resultMap = new HashMap<>();
//        List<TransactionReceipt.Logs> logList = receipt.getLogEntries();
//        for (ABIDefinition abiDefinition : abiList) {
//            String eventName = abiDefinition.getName();
//            List<String> funcInputTypes = getFuncInputType(abiDefinition);
//            List<TypeReference<?>> finalOutputs = outputFormat(funcInputTypes);
//            Event event = new Event(eventName, finalOutputs);
//            Object result = null;
//            for (TransactionReceipt.Logs logInfo : logList) {
//                EventValues eventValues = Contract.extractEventParameters(event, logInfo);
//                if (eventValues != null) {
//                    result = callResultParse(funcInputTypes, eventValues.getNonIndexedValues());
//                    break;
//                }
//            }
//            if (result != null) {
//                resultMap.put(eventName, result);
//            }
//        }
//        return resultMap;
//    }
}
