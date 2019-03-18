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
package com.webank.webase.transaction.front;

import com.alibaba.fastjson.JSON;
import com.webank.webase.transaction.base.BaseResponse;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.util.CommonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class FrontService {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ConstantProperties constantProperties;

    /**
     * random request front.
     * 
     * @param uri uri
     * @param httpType httpType
     * @param params request
     * @return
     */
    private BaseResponse randomRequestFront(String uri, RequestMethod httpType, Object params) {
        log.info("start randomRequestFront. uri:{} httpType:{} params:{}", uri, httpType,
                JSON.toJSONString(params));
        BaseResponse frontRsp = new BaseResponse(ConstantCode.SYSTEM_ERROR);
        HttpHeaders headers = CommonUtils.buildHeaders();

        List<String> nodeList =
                new ArrayList<>(Arrays.asList(constantProperties.getFrontIpPorts().split(",")));
        if (nodeList == null || nodeList.size() == 0) {
            return new BaseResponse(ConstantCode.IPPORT_NOT_CONFIGURED);
        }

        int nodeSize = nodeList.size();
        Random random = new Random();
        List<Integer> indexList = new ArrayList<>(nodeSize);

        while (true) {
            if (indexList.size() == nodeSize) {
                log.info("all node had used. return frontRsp:{}", JSON.toJSONString(frontRsp));
                return frontRsp;
            }

            int index = random.nextInt(nodeSize);
            if (indexList.contains(index)) {
                log.info(
                        "fail getFromNodeFront, nodeSize:{} indexList:{} currentIndex:{}.",
                        nodeSize, JSON.toJSONString(indexList), index);
                continue;
            }

            String nodeIpPort = nodeList.get(index);
            indexList.add(index);

            String url = String.format(ConstantProperties.FRONT_BASE_URI, nodeIpPort, uri);
            log.info("getFromNodeFront url: {}", url);
            try {
                if (httpType == null) {
                    log.info("httpType is empty.use default:get");
                    httpType = RequestMethod.GET;
                }
                // get
                if (httpType.equals(RequestMethod.GET)) {
                    frontRsp = restTemplate.getForObject(url, BaseResponse.class);
                }
                // post
                if (httpType.equals(RequestMethod.POST)) {
                    HttpEntity<String> formEntity =
                            new HttpEntity<String>(JSON.toJSONString(params), headers);
                    frontRsp = restTemplate.postForObject(url, formEntity, BaseResponse.class);
                }
            } catch (RuntimeException ex) {
                log.warn("fail getFromNodeFront nodeIpPort:{}", nodeIpPort, ex);
            } finally {
                if (frontRsp.getCode() != 0 && indexList.size() < nodeSize) {
                    log.warn("fail getFromNodeFront, nodeSize:{} indexList:{}. try ndex node",
                            nodeSize, JSON.toJSONString(indexList));
                    continue;
                }
            }
            log.info("end getFromNodeFront. url:{} frontRsp:{}", url, JSON.toJSONString(frontRsp));
            return frontRsp;
        }
    }

    /**
     * sendTransaction.
     * 
     * @param params request
     * @return
     */
    public BaseResponse sendTransaction(Object params) {
        return randomRequestFront(ConstantProperties.FRONT_SEND_TRANSACTION, RequestMethod.POST,
                params);
    }
}
