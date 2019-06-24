/*
 * Copyright 2012-2019 the original author or authors.
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

package com.webank.webase.transaction.contract;

import com.webank.webase.transaction.base.ConstantCode;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * contract deploy parameters.
 * 
 */
@Data
public class ReqDeployInfo {
    @NotNull(message = ConstantCode.GROUP_ID_IS_EMPTY)
    private int groupId;
    @NotBlank(message = ConstantCode.UUID_DEPLOY_IS_EMPTY)
    private String uuidDeploy;
    @NotNull(message = ConstantCode.SIGN_TYPE_IS_EMPTY)
    private int signType;
    @NotBlank(message = ConstantCode.CONTRACT_BIN_IS_EMPTY)
    private String contractBin;
    @NotEmpty(message = ConstantCode.CONTRACT_ABI_IS_EMPTY)
    private List<Object> contractAbi;
    private List<Object> funcParam = new ArrayList<>();
}
