/**
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
package com.webank.webase.transaction.frontinterface;

import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.frontinterface.entity.TransactionCount;
import com.webank.webase.transaction.util.CommonUtils;
import com.webank.webase.transaction.util.JsonUtils;
import io.swagger.annotations.Api;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;
import org.fisco.bcos.web3j.protocol.core.methods.response.BcosBlock.Block;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
