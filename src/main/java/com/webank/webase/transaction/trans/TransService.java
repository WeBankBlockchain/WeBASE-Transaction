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
package com.webank.webase.transaction.trans;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.webank.webase.transaction.base.BaseResponse;
import com.webank.webase.transaction.base.ConstantCode;
import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.front.FrontService;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * TransService.
 * 
 */
@Slf4j
@Service
public class TransService {
    @Autowired
    private TransMapper transMapper;
    @Autowired
    private FrontService frontService;
    @Autowired
    private ThreadPoolTaskExecutor transExecutor;
    @Autowired
    private ConstantProperties properties;

    /**
     * transHandle.
     * 
     * @param req parameter
     * @return
     */
    public BaseResponse transHandle(ReqTransHandle req) {
        BaseResponse baseRsp = new BaseResponse(ConstantCode.RET_SUCCEED);
        boolean ifAsync = req.isIfAsync();
        int userId = req.getUserId();
        String uuid = req.getUuid();
        if (ifAsync) {
            try {
                TransInfo transInfoDto = new TransInfo();
                transInfoDto.setUserId(userId);
                transInfoDto.setUuid(uuid);
                transInfoDto.setContractName(req.getContractName());
                transInfoDto.setVersion(req.getVersion());
                transInfoDto.setFuncName(req.getFuncName());
                transInfoDto.setFuncParam(JSON.toJSONString(req.getFuncParam()));
                transMapper.insertTransInfo(transInfoDto);
            } catch (DuplicateKeyException e) {
                log.error("transHandle userId:{} uuid:{} DuplicateKeyException:{}", userId, uuid,
                        e);
                return new BaseResponse(ConstantCode.UUID_IS_EXISTS);
            } catch (Exception e) {
                log.error("transHandle userId:{} uuid:{} Exception:{}", userId, uuid, e);
                return new BaseResponse(ConstantCode.SYSTEM_ERROR);
            }
        } else {
            baseRsp = frontService.sendTransaction(req);
        }
        log.info("transHandle end. userId:{} uuid:{}", userId, uuid);
        return baseRsp;
    }

    /**
     * handleTransInfo.
     * 
     * @param transInfoList transInfoList
     */
    public void handleTransInfo(List<TransInfo> transInfoList) {
        for (TransInfo transInfoDto : transInfoList) {
            try {
                Thread.sleep(properties.getSleepTime());
                transExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        sendTrans(transInfoDto);
                    }
                });
            } catch (RejectedExecutionException e) {
                log.error("schedule threadPool is full", e);
            } catch (InterruptedException e) {
                log.error("schedule InterruptedException", e);
                Thread.currentThread().interrupt();
            }
        }

    }

    /**
     * sendTrans.
     * 
     * @param transInfoDto transInfoDto
     */
    public void sendTrans(TransInfo transInfoDto) {
        try {
            log.info("sendTrans transInfoDto:{}", JSON.toJSONString(transInfoDto));
            int requestCount = transInfoDto.getRequestCount();
            int userId = transInfoDto.getUserId();

            // requestCount + 1
            transMapper.updateRequestCount(transInfoDto.getId(), requestCount + 1);

            if (requestCount == properties.getRequestCountMax()) {
                log.warn("trans id:{} has reached limit:{}", transInfoDto.getId(),
                        properties.getRequestCountMax());
                return;
            }

            ReqTransHandle sendTransInfo = new ReqTransHandle();
            sendTransInfo.setUserId(userId);
            sendTransInfo.setContractName(transInfoDto.getContractName());
            sendTransInfo.setVersion(transInfoDto.getVersion());
            sendTransInfo.setFuncName(transInfoDto.getFuncName());
            sendTransInfo.setFuncParam(JSONArray.parseArray(transInfoDto.getFuncParam()));

            BaseResponse response = frontService.sendTransaction(sendTransInfo);
            if (response != null && response.getCode() == 0) {
                transMapper.updateTransStatus(transInfoDto.getId());
            }
        } catch (Exception e) {
            log.error("fail sendTrans id:{}", transInfoDto.getId(), e);
        }
    }
}
