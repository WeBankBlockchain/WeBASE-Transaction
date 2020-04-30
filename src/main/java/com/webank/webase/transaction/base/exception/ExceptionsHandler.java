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

package com.webank.webase.transaction.base.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.RetCode;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * ExceptionsHandler.
 * 
 */
@ControllerAdvice
@Slf4j
public class ExceptionsHandler {
    @Autowired
    ObjectMapper mapper;

    /**
     * BaseException Handler.
     * 
     * @param baseException e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(value = BaseException.class)
    public ResponseEntity myExceptionHandler(BaseException baseException) throws Exception {
        log.warn("catch business exception", baseException);
        RetCode retCode = Optional.ofNullable(baseException).map(BaseException::getRetCode)
                .orElse(ConstantCode.SYSTEM_ERROR);

        ResponseEntity rep = new ResponseEntity(retCode);
        log.warn("business exception return:{}", mapper.writeValueAsString(rep));
        return rep;
    }

    /**
     * Exception Handler.
     * 
     * @param exc e
     * @return
     */
    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity exceptionHandler(Exception exc) {
        log.info("catch  exception", exc);
        RetCode retCode = ConstantCode.SYSTEM_ERROR;
        ResponseEntity rep = new ResponseEntity(retCode);
        try {
            log.warn("exceptionHandler system exception return:{}", mapper.writeValueAsString(rep));
        } catch (JsonProcessingException ex) {
            log.warn("exceptionHandler system exception");
        }
        return rep;
    }
}
