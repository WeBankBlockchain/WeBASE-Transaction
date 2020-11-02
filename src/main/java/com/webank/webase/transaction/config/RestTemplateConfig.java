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
package com.webank.webase.transaction.config;

import com.webank.webase.transaction.base.Constants;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Restful request template configuration
 */
@Data
@Configuration
public class RestTemplateConfig {
    
    @Autowired
    private Constants constantProperties;
    
    /**
     * new RestTemplate.
     * 
     * @param factory object
     * @return
     */
    @Bean
    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

    /**
     * resttemplate for generic http request.
     */
    @Bean(name = "genericRestTemplate")
    public RestTemplate getRestTemplate() {
        SimpleClientHttpRequestFactory factory = getHttpFactoryForDeploy();
        factory.setReadTimeout(constantProperties.getHttpTimeOut());// ms
        factory.setConnectTimeout(constantProperties.getHttpTimeOut());// ms
        return new RestTemplate(factory);
    }

    /**
     * resttemplate for deploy contract.
     */
    @Bean(name = "deployRestTemplate")
    public RestTemplate getDeployRestTemplate() {
        SimpleClientHttpRequestFactory factory = getHttpFactoryForDeploy();
        factory.setReadTimeout(constantProperties.getContractDeployTimeOut());// ms
        factory.setConnectTimeout(constantProperties.getContractDeployTimeOut());// ms
        return new RestTemplate(factory);
    }
    
    /**
     * factory for deploy.
     */
    @Bean()
    @Scope("prototype")
    public SimpleClientHttpRequestFactory getHttpFactoryForDeploy() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        return factory;
    }
}
