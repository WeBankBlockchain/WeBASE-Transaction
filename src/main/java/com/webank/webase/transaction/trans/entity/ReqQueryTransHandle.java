package com.webank.webase.transaction.trans.entity;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReqQueryTransHandle {
    @NotNull
    private Integer chainId;
    @NotNull
    private Integer groupId;
    @NotBlank
    private String encodeStr;
    @NotBlank
    private String contractAddress;
    @NotBlank
    private String funcName;
    @NotEmpty
    private List<Object> functionAbi = new ArrayList<>();
}
