/**
 * Copyright 2014-2021 the original author or authors.
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
package com.webank.webase.transaction.frontinterface;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.fisco.bcos.web3j.protocol.core.methods.response.BcosBlock.Block;
import org.fisco.bcos.web3j.protocol.core.methods.response.NodeVersion.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.frontinterface.entity.PeerInfo;
import com.webank.webase.transaction.frontinterface.entity.SyncStatus;
import com.webank.webase.transaction.frontinterface.entity.TransactionCount;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.JsonUtils;

import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;

/**
 * Controller for node data.
 */
@Api(value = "/front", tags = "front interface")
@Log4j2
@RestController
@RequestMapping("front")
public class FrontInterfaceController extends BaseController {

    @Autowired
    private FrontInterfaceService frontInterfaceService;
    
    /**
     * getClientVersion.
     */
    @GetMapping("/getClientVersion/{chainId}/{groupId}")
    public ResponseEntity getClientVersion(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getClientVersion startTime:{} chainId:{} groupId:{}",
                startTime.toEpochMilli(), groupId, groupId);
        
        Version result =
                frontInterfaceService.getClientVersion(chainId, groupId);
        
        log.info("end getClientVersion useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(result));
        return CommonUtils.buildSuccessRsp(result);
    }

    /**
     * get block number.
     */
    @GetMapping("/getBlockNumber/{chainId}/{groupId}")
    public ResponseEntity getBlockNumber(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getBlockNumber startTime:{} chainId:{} groupId:{}",
                startTime.toEpochMilli(), groupId, groupId);

        BigInteger blockNumber = frontInterfaceService.getLatestBlockNumber(chainId, groupId);

        log.info("end getBlockNumber useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(blockNumber));
        return CommonUtils.buildSuccessRsp(blockNumber);
    }

    /**
     * get block by number.
     */
    @GetMapping("/getBlockByNumber/{chainId}/{groupId}/{blockNumber}")
    public ResponseEntity getBlockByNumber(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId,
            @PathVariable("blockNumber") BigInteger blockNumber) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getBlockByNumber startTime:{} groupId:{} blockNumber:{}",
                startTime.toEpochMilli(), groupId, blockNumber);

        Block blockInfo = frontInterfaceService.getBlockByNumber(chainId, groupId, blockNumber);

        log.info("end getBlockByNumber useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(blockInfo));
        return CommonUtils.buildSuccessRsp(blockInfo);
    }

    /**
     * get total transaction count.
     */
    @GetMapping("/getTotalTransactionCount/{chainId}/{groupId}")
    public ResponseEntity getTotalTransactionCount(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getTotalTransactionCount startTime:{} chainId:{} groupId:{}",
                startTime.toEpochMilli(), groupId, groupId);

        TransactionCount transactionCount =
                frontInterfaceService.getTotalTransactionCount(chainId, groupId);

        log.info("end getTotalTransactionCount useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(transactionCount));
        return CommonUtils.buildSuccessRsp(transactionCount);
    }
    
    /**
     * getPeers.
     */
    @GetMapping("/getPeers/{chainId}/{groupId}")
    public ResponseEntity getPeers(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getPeers startTime:{} chainId:{} groupId:{}",
                startTime.toEpochMilli(), groupId, groupId);
        
        List<PeerInfo> result =
                frontInterfaceService.getPeers(chainId, groupId);
        
        log.info("end getPeers useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(result));
        return CommonUtils.buildSuccessRsp(result);
    }
    
    /**
     * getNodeIdList.
     */
    @GetMapping("/getNodeIdList/{chainId}/{groupId}")
    public ResponseEntity getNodeIdList(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getNodeIdList startTime:{} chainId:{} groupId:{}",
                startTime.toEpochMilli(), groupId, groupId);
        
        List<String> result =
                frontInterfaceService.getNodeIdList(chainId, groupId);
        
        log.info("end getNodeIdList useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(result));
        return CommonUtils.buildSuccessRsp(result);
    }
    
    /**
     * getGroupPeers.
     */
    @GetMapping("/getGroupPeers/{chainId}/{groupId}")
    public ResponseEntity getGroupPeers(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getGroupPeers startTime:{} chainId:{} groupId:{}",
                startTime.toEpochMilli(), groupId, groupId);
        
        List<String> result =
                frontInterfaceService.getGroupPeers(chainId, groupId);
        
        log.info("end getGroupPeers useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(result));
        return CommonUtils.buildSuccessRsp(result);
    }
    
    /**
     * getObserverList.
     */
    @GetMapping("/getObserverList/{chainId}/{groupId}")
    public ResponseEntity getObserverList(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getObserverList startTime:{} chainId:{} groupId:{}",
                startTime.toEpochMilli(), groupId, groupId);
        
        List<String> result =
                frontInterfaceService.getObserverList(chainId, groupId);
        
        log.info("end getObserverList useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(result));
        return CommonUtils.buildSuccessRsp(result);
    }
    
    /**
     * getSyncStatus.
     */
    @GetMapping("/getSyncStatus/{chainId}/{groupId}")
    public ResponseEntity getSyncStatus(@PathVariable("chainId") Integer chainId,
            @PathVariable("groupId") Integer groupId) throws BaseException {
        Instant startTime = Instant.now();
        log.info("start getSyncStatus startTime:{} chainId:{} groupId:{}",
                startTime.toEpochMilli(), groupId, groupId);
        
        SyncStatus result =
                frontInterfaceService.getSyncStatus(chainId, groupId);
        
        log.info("end getSyncStatus useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JsonUtils.toJSONString(result));
        return CommonUtils.buildSuccessRsp(result);
    }
}
