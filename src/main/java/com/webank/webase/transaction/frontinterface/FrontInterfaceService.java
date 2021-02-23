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

import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.contract.entity.ReqContractCompile;
import com.webank.webase.transaction.contract.entity.RspContractCompile;
import com.webank.webase.transaction.frontinterface.entity.PeerInfo;
import com.webank.webase.transaction.frontinterface.entity.SyncStatus;
import com.webank.webase.transaction.frontinterface.entity.TransactionCount;
import com.webank.webase.transaction.util.JsonUtils;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.web3j.protocol.core.methods.response.BcosBlock.Block;
import org.fisco.bcos.web3j.protocol.core.methods.response.NodeVersion.Version;
import org.fisco.bcos.web3j.protocol.core.methods.response.Transaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FrontInterfaceService {

    @Autowired
    private FrontRestTools frontRestTools;

    @Cacheable(cacheNames = "clientVersion")
    public Version getClientVersion(Integer chainId, Integer groupId) {
        Version result = frontRestTools.getForEntity(chainId, groupId,
                FrontRestTools.URI_CLIENT_VERSION, Version.class);
        return result;
    }

    public BigInteger getLatestBlockNumber(Integer chainId, Integer groupId) {
        BigInteger latestBlockNmber = frontRestTools.getForEntity(chainId, groupId,
                FrontRestTools.URI_BLOCK_NUMBER, BigInteger.class);
        return latestBlockNmber;
    }

    public Block getBlockByNumber(Integer chainId, Integer groupId, BigInteger blockNmber) {
        String uri = String.format(FrontRestTools.URI_BLOCK_BY_NUMBER, blockNmber);
        Block result = frontRestTools.getForEntity(chainId, groupId, uri, Block.class);
        return result;
    }

    public TransactionCount getTotalTransactionCount(Integer chainId, Integer groupId) {
        TransactionCount result = frontRestTools.getForEntity(chainId, groupId,
                FrontRestTools.URI_TRANS_TOTAL, TransactionCount.class);
        return result;
    }

    public Transaction getTransactionByHash(Integer chainId, Integer groupId, String transHash) {
        String uri = String.format(FrontRestTools.URI_TRANS_BY_HASH, transHash);
        Transaction result = frontRestTools.getForEntity(chainId, groupId, uri, Transaction.class);
        return result;
    }

    public TransactionReceipt getTransactionReceipt(Integer chainId, Integer groupId,
            String transHash) {
        String uri = String.format(FrontRestTools.URI_TRANS_RECEIPT, transHash);
        TransactionReceipt result =
                frontRestTools.getForEntity(chainId, groupId, uri, TransactionReceipt.class);
        return result;
    }

    public String getContractCode(Integer chainId, Integer groupId, String address,
            BigInteger blockNumber) throws BaseException {
        String uri = String.format(FrontRestTools.URI_CODE, address, blockNumber);
        String contractCode = frontRestTools.getForEntity(chainId, groupId, uri, String.class);
        return contractCode;
    }

    public List<PeerInfo> getPeers(Integer chainId, Integer groupId) {
        PeerInfo[] result = frontRestTools.getForEntity(chainId, groupId, FrontRestTools.URI_PEERS,
                PeerInfo[].class);
        return Arrays.asList(result);
    }

    @SuppressWarnings("unchecked")
    public List<String> getNodeIdList(Integer chainId, Integer groupId) {
        List<String> list = frontRestTools.getForEntity(chainId, groupId,
                FrontRestTools.URI_NODEID_LIST, List.class);
        return list;
    }

    @SuppressWarnings("unchecked")
    public List<String> getGroupPeers(Integer chainId, Integer groupId) {
        log.debug("start getGroupPeers. groupId:{}", groupId);
        List<String> groupPeers = frontRestTools.getForEntity(chainId, groupId,
                FrontRestTools.URI_GROUP_PEERS, List.class);
        log.debug("end getGroupPeers. groupPeers:{}", JsonUtils.toJSONString(groupPeers));
        return groupPeers;
    }

    @SuppressWarnings("unchecked")
    public List<String> getObserverList(Integer chainId, Integer groupId) {
        log.debug("start getObserverList. groupId:{}", groupId);
        List<String> observers = frontRestTools.getForEntity(chainId, groupId,
                FrontRestTools.URI_GET_OBSERVER_LIST, List.class);
        log.info("end getObserverList. observers:{}", JsonUtils.toJSONString(observers));
        return observers;
    }

    public String getConsensusStatus(Integer chainId, Integer groupId) {
        log.debug("start getConsensusStatus. groupId:{}", groupId);
        String consensusStatus = frontRestTools.getForEntity(chainId, groupId,
                FrontRestTools.URI_CONSENSUS_STATUS, String.class);
        log.debug("end getConsensusStatus. consensusStatus:{}", consensusStatus);
        return consensusStatus;
    }

    public SyncStatus getSyncStatus(Integer chainId, Integer groupId) {
        log.debug("start getSyncStatus. groupId:{}", groupId);
        SyncStatus ststus = frontRestTools.getForEntity(chainId, groupId,
                FrontRestTools.URI_CSYNC_STATUS, SyncStatus.class);
        log.debug("end getSyncStatus. ststus:{}", JsonUtils.toJSONString(ststus));
        return ststus;
    }

    public TransactionReceipt sendSignedTransaction(Integer chainId, Integer groupId,
            String signMsg, Boolean sync) {
        Instant startTime = Instant.now();
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("signedStr", signMsg);
        params.put("sync", sync);
        TransactionReceipt receipt = frontRestTools.postForEntity(chainId, groupId,
                FrontRestTools.URI_SIGNED_TRANSACTION, params, TransactionReceipt.class);
        log.info("sendSignedTransaction to front useTime: {}",
                Duration.between(startTime, Instant.now()).toMillis());
        return receipt;
    }

    public Object sendQueryTransaction(Integer chainId, Integer groupId, Object params) {
        Object result = frontRestTools.postForEntity(chainId, groupId,
                FrontRestTools.URI_QUERY_TRANSACTION, params, Object.class);
        return result;
    }

    public RspContractCompile contractCompile(ReqContractCompile req) {
        Map<String, Object> params = new HashMap<>();
        params.put("contractName", req.getContractName());
        params.put("solidityBase64", req.getSolidityBase64());
        RspContractCompile result = frontRestTools.postForEntity(req.getChainId(), req.getGroupId(),
                FrontRestTools.URI_MULTI_CONTRACT_COMPILE, params, RspContractCompile.class);
        return result;
    }
}
