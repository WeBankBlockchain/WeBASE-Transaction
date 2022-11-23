package com.webank.webase.transaction.web3api.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

/**
 * for node-manager
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RspTransCountInfo {
   private BigInteger txSum;
   private BigInteger blockNumber;
   private BigInteger failedTxSum;
}