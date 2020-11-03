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

import java.math.BigInteger;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ConstantProperties.
 *
 */
@Data
@Configuration
@ConfigurationProperties(prefix = Constants.CONSTANT_PREFIX)
public class Constants {
    public static final BigInteger GAS_PRICE = new BigInteger("100000000");
    public static final BigInteger GAS_LIMIT = new BigInteger("100000000");
    public static final BigInteger INITIAL_WEI_VALUE = new BigInteger("0");
    public static final BigInteger LIMIT_VALUE = new BigInteger("600");

    public static final String TYPE_CONSTRUCTOR = "constructor";
    public static final String TYPE_FUNCTION = "function";
    public static final String TYPE_EVENT = "event";

    // constant configuration from file
    public static final String CONSTANT_PREFIX = "constant";
    private String signServer = "127.0.0.1:5004";
    private int transMaxWait = 20;
    
    // front http request
    private String frontUrl;
    private Integer httpTimeOut = 5000;
    private Integer contractDeployTimeOut = 30000;
    private Integer maxRequestFail = 3;
    private Long sleepWhenHttpMaxFail = 60000L; // default 1min
}
