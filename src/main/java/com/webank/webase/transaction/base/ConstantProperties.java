package com.webank.webase.transaction.base;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
@Data
@Configuration
@ConfigurationProperties(prefix = ConstantProperties.CONSTANT_PREFIX)
public class ConstantProperties {

    public static final String FRONT_BASE_URI = "http://%s/webase-front/%s";
    public static final String FRONT_SEND_TRANSACTION = "trans/handle";

    // constant configuration from file
    public static final String CONSTANT_PREFIX = "constant";
    private String frontIpPorts = "10.0.0.1:8081";
    private String cronTrans = "0/1 * * * * ?";
    private int requestCountMax = 6;
    private int selectCount = 10;
    private int intervalTime = 600;
    private int sleepTime = 30;
}
