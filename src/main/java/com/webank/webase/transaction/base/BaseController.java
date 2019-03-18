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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

@Slf4j
public abstract class BaseController {
    /**
     * checkParamResult.
     * 
     * @param BindingResult bindingResult
     * @return
     */
    protected BaseResponse checkParamResult(BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            return null;
        }

        String errorMsg = getParamValidFaildMessage(bindingResult);
        if (StringUtils.isBlank(errorMsg)) {
            log.warn("OnWarning:param exception. errorMsg is empty");
            return new BaseResponse(ConstantCode.PARAM_VAILD_FAIL);
        }

        RetCode retCode = null;
        try {
            JSONObject jsonObject = JSON.parseObject(errorMsg);
            retCode = JSONObject.toJavaObject(jsonObject, RetCode.class);
        } catch (Exception ex) {
            log.warn("OnWarning:retCodeJson convert error:" + ex.getMessage());
            return new BaseResponse(ConstantCode.PARAM_VAILD_FAIL);
        }

        return new BaseResponse(retCode);
    }

    /**
     * getParamValidFaildMessage.
     * 
     * @param BindingResult bindingResult
     * @return
     */
    private String getParamValidFaildMessage(BindingResult bindingResult) {
        List<ObjectError> errorList = bindingResult.getAllErrors();
        log.info("errorList:{}", JSON.toJSONString(errorList));
        if (errorList == null) {
            log.warn("onWarning:errorList is empty!");
            return null;
        }

        ObjectError objectError = errorList.get(0);
        if (objectError == null) {
            log.warn("onWarning:objectError is empty!");
            return null;
        }

        return objectError.getDefaultMessage();
    }
}
