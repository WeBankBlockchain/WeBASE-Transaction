/**
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
package com.webank.webase.transaction.frontinterface;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.fisco.bcos.web3j.protocol.core.methods.response.NodeVersion.Version;
import org.fisco.bcos.web3j.protocol.core.methods.response.Transaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FrontInterfaceService {

    @Autowired
    private FrontRestTools frontRestTools;

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

    public TransactionReceipt sendSignedTransaction(Integer chainId, Integer groupId,
            String signMsg, Boolean sync) {
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        params.put("signedStr", signMsg);
        params.put("sync", sync);
        TransactionReceipt receipt = frontRestTools.postForEntity(chainId, groupId,
                FrontRestTools.URI_SIGNED_TRANSACTION, params, TransactionReceipt.class);
        return receipt;
    }

    public Object sendQueryTransaction(Integer chainId, Integer groupId, Object params) {
        Object result = frontRestTools.postForEntity(chainId, groupId,
                FrontRestTools.URI_QUERY_TRANSACTION, params, Object.class);
        return result;
    }
}
