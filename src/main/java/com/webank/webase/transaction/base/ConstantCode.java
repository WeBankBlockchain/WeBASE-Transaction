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

package com.webank.webase.transaction.base;

public interface ConstantCode {

    // return success
    RetCode RET_SUCCEED = RetCode.mark(0, "success");

    // paramaters check
    String PARAM_FAIL_UUID_IS_EMPTY = "{\"code\":203001,\"msg\":\"uuid cannot be empty\"}";
    String PARAM_FAIL_USERID_IS_EMPTY = "{\"code\":203002,\"msg\":\"userId cannot be empty\"}";
    String PARAM_FAIL_CONTRACTNAME_IS_EMPTY =
            "{\"203003\":1003,\"msg\":\"contractName cannot be empty\"}";
    String PARAM_FAIL_VERSION_IS_EMPTY = "{\"code\":203004,\"msg\":\"version cannot be empty\"}";
    String PARAM_FAIL_FUNCNAME_IS_EMPTY = "{\"code\":203005,\"msg\":\"funcName cannot be empty\"}";
    String PARAM_FAIL_ABIINFO_IS_EMPTY = "{\"code\":203006,\"msg\":\"abiInfo cannot be empty\"}";
    String PARAM_FAIL_IFASYNC_IS_EMPTY = "{\"code\":203007,\"msg\":\"ifAsync cannot be empty\"}";

    // general error
    RetCode UUID_IS_EXISTS = RetCode.mark(303001, "uuid is already exists");
    RetCode IPPORT_NOT_CONFIGURED = RetCode.mark(303002, "front's ipPort isn't configured");

    // system error
    RetCode SYSTEM_ERROR = RetCode.mark(103001, "system error");
    RetCode PARAM_VAILD_FAIL = RetCode.mark(103002, "param valid fail");
}
