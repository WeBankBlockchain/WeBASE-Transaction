/*
 * Copyright 2014-2019 the original author or authors.
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

    // general error
    RetCode IN_FUNCPARAM_ERROR = RetCode.mark(203001, "contract funcParam is error");
    RetCode GROUPID_NOT_CONFIGURED = RetCode.mark(203002, "request group id has not been configured");
    RetCode SIGN_USERID_ERROR = RetCode.mark(203003, "signUserId check failed");
    RetCode FUNCTION_NOT_EXISTS = RetCode.mark(203004, "function is not exists");
    RetCode WEB3JMAP_IS_EMPTY = RetCode.mark(203005, "web3jMap is empty");
    RetCode WEB3J_IS_NULL = RetCode.mark(203006, "web3j is null, please check groupId");
    RetCode DATA_SIGN_ERROR = RetCode.mark(203007, "data request sign error");
    RetCode NODE_REQUEST_FAILED = RetCode.mark(203008, "node request failed");
    RetCode TRANSACTION_FAILED = RetCode.mark(203009, "transaction failed!");
    RetCode IN_FUNCTION_ERROR = RetCode.mark(203010, "request function is error");
    RetCode FAIL_PARSE_JSON = RetCode.mark(203011, "Fail to parse json");
    RetCode ENCODE_STR_CANNOT_BE_NULL = RetCode.mark(203012, "encode string can not be empty!");
    // system error
    RetCode SYSTEM_ERROR = RetCode.mark(103001, "system error");
    RetCode PARAM_VAILD_FAIL = RetCode.mark(103002, "param valid fail");
}
