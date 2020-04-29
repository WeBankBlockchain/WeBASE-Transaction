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

package com.webank.webase.transaction.trans.entity;

import java.util.Date;
import lombok.Data;

/**
 * TransInfoDto.
 * 
 */
@Data
public class TransInfoDto {
    private Long id;
    private int groupId;
    private String uuidStateless;
    private String uuidDeploy;
    private String contractAbi;
    private String contractAddress;
    private String funcName;
    private String funcParam;
    private int signType;
    private String signUserId;
    private int requestCount;
    private int handleStatus = 0;
    private String transHash;
    private String transOutput;
    private boolean receiptStatus;
    private Date gmtCreate;
}
