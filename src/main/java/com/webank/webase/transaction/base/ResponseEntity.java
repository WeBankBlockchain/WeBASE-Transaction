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

import lombok.Data;

/**
 * ResponseEntity.
 *
 */
@Data
public class ResponseEntity {
    private int code;
    private String message;
    private Object data;

    public ResponseEntity() {}

    public ResponseEntity(int code) {
        this.code = code;
    }

    public ResponseEntity(RetCode rsc) {
        this.code = rsc.getCode();
        this.message = rsc.getMsg();
    }

    /**
     * constructor.
     * 
     * @param rsc not null
     * @param obj result
     */
    public ResponseEntity(RetCode rsc, Object obj) {
        this.code = rsc.getCode();
        this.message = rsc.getMsg();
        this.data = obj;
    }

    /**
     * constructor.
     * 
     * @param code not null
     * @param message not null
     * @param obj result
     */
    public ResponseEntity(int code, String message, Object obj) {
        this.code = code;
        this.message = message;
        this.data = obj;
    }
}
