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

package com.webank.webase.transaction.chain.entity;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QueryConfigInfo.
 * 
 */
@Data
@NoArgsConstructor
public class QueryConfigInfo {
    private Integer chainId;
    private List<Integer> groupList;
    
    public QueryConfigInfo(Integer chainId, List<Integer> groupList) {
        this.chainId = chainId;
        this.groupList = groupList;
    }
}
