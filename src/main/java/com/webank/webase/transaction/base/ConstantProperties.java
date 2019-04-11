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

import lombok.Data;

import java.math.BigInteger;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = ConstantProperties.CONSTANT_PREFIX)
public class ConstantProperties {
	public static final BigInteger GAS_PRICE = new BigInteger("100000000");
    public static final BigInteger GAS_LIMIT = new BigInteger("100000000");
    public static final BigInteger INITIAL_WEI_VALUE = new BigInteger("0");
    
    public static final String TYPE_CONSTRUCTOR = "constructor";
    public static final String TYPE_FUNCTION = "function";

    // constant configuration from file
    public static final String CONSTANT_PREFIX = "constant";
    private String signServiceUrl = "http://10.0.0.1:8081/sign-service/sign";
    private String privateKey = "edf02a4a69b14ee6b1650a95de71d5f50496ef62ae4213026bd8d6651d030995";
    private String solTempDir = "./conf/temp";
    private String cronTrans = "0/1 * * * * ?";
    private int requestCountMax = 6;
    private int selectCount = 10;
    private int intervalTime = 600;
    private int sleepTime = 30;
    private int transMaxWait = 20;
    private boolean ifDistributedTask = false;
}
