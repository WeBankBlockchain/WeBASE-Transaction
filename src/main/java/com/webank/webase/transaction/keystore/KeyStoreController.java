/**
 * Copyright 2014-2020  the original author or authors.
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
package com.webank.webase.transaction.keystore;


import com.webank.webase.transaction.base.ResponseEntity;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.entity.*;
import com.webank.webase.transaction.util.CommonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;


/**
 * Controller.
 */
@Slf4j
@Api(value = "user", tags = "user interface")
@RestController
@RequestMapping("user")
public class KeyStoreController {

    @Autowired
    private KeyStoreService userService;


    @ApiOperation(value = "import new user by private key",
            notes = "导入私钥用户(ecdsa或国密)，默认ecdas")
    @PostMapping("/newUser")
    public ResponseEntity newUserByImportPrivateKey(@Valid @RequestBody ReqNewUser reqNewUser, BindingResult result)
        throws BaseException {
        RspUserInfo userInfo = userService.newUser(reqNewUser);
        return CommonUtils.buildSuccessRsp(userInfo);
    }
    /**
     * get user.
     */
    @ApiOperation(value = "check user info exist", notes = "check user info exist")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "signUserId", value = "business id of user in system",
                    required = true, dataType = "String"),
    })
    @GetMapping("/{signUserId}/userInfo")
    public ResponseEntity getUserInfo(@PathVariable("signUserId") String signUserId) throws BaseException {
        RspUserInfo userInfo = userService.getUserBySignUserId(signUserId);
        return CommonUtils.buildSuccessRsp(userInfo);
    }

    /**
     * get user list by app id
     */
    @ApiOperation(value = "get user list by app id", notes = "根据appId获取user列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "app id that users belong to",
                    required = true, dataType = "String"),
    })
    @GetMapping("/list/{appId}/{pageNumber}/{pageSize}")
    public ResponseEntity getUserListByAppId(@PathVariable("appId") String appId,
                                        @PathVariable("pageNumber") Integer pageNumber,
                                        @PathVariable("pageSize") Integer pageSize) throws BaseException {
        //find user
        List<RspGetUserList> userList = userService.getUserListByAppId(appId,pageNumber,pageSize);
        if (!userList.isEmpty()) {
            userList.forEach(user -> user.setPrivateKey(""));
        }
        return CommonUtils.buildSuccessPageRspVo(userList, userList.size());
    }


    @DeleteMapping("")
    public ResponseEntity deleteUser(@RequestBody ReqUserInfo req) throws BaseException {
        // set as 0: SUSPENDED
        userService.deleteUser(req);
        return CommonUtils.buildSuccessRsp(null);
    }

}
