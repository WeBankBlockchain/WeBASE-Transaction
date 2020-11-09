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

package com.webank.webase.transaction.base;

/**
 * ConstantCode.
 *
 */
public interface ConstantCode {
    // return success
    RetCode RET_SUCCEED = RetCode.mark(0, "success");
    
    // system error
    RetCode SYSTEM_ERROR = RetCode.mark(103001, "system error");
    RetCode PARAM_VAILD_FAIL = RetCode.mark(103002, "param valid fail");
    
    // contract
    RetCode IN_FUNCPARAM_ERROR = RetCode.mark(203101, "contract funcParam is error");
    RetCode FUNCTION_NOT_EXISTS = RetCode.mark(203102, "function is not exists");
    RetCode ABI_PARSE_ERROR = RetCode.mark(203103, "abi parse error");
    
    // sign
    RetCode SIGN_USERID_ERROR = RetCode.mark(203201, "signUserId check failed");
    RetCode DATA_SIGN_ERROR = RetCode.mark(203202, "data request sign error");
    RetCode REQUEST_SIGN_EXCEPTION = RetCode.mark(203203, "request sign server exception");
    
    // front
    RetCode REQUEST_NODE_EXCEPTION = RetCode.mark(203301, "request node exception");
    RetCode REQUEST_FRONT_FAIL = RetCode.mark(203302, "request front fail");
    RetCode FRONT_LIST_NOT_FOUNT = RetCode.mark(203303, "not fount any front");
}
