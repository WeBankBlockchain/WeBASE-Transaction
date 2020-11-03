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
package com.webank.webase.transaction.keystore;

import com.webank.webase.transaction.base.BaseController;
import com.webank.webase.transaction.base.exception.BaseException;
import com.webank.webase.transaction.keystore.entity.ReqNewUser;
import com.webank.webase.transaction.keystore.entity.ReqUserInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller.
 */
@Slf4j
@Api(value = "user", tags = "user interface")
@RestController
@RequestMapping("user")
public class KeyStoreController extends BaseController {

    @Autowired
    private KeyStoreService userService;

    /**
     * newUserByImportPrivateKey.
     * 
     * @param reqNewUser
     * @param result
     * @return
     */
    @ApiOperation(value = "import new user by private key", notes = "导入私钥用户(ecdsa或国密)，默认ecdas")
    @PostMapping("/newUser")
    public Object newUserByImportPrivateKey(@Valid @RequestBody ReqNewUser reqNewUser,
            BindingResult result) throws BaseException {
        checkBindResult(result);
        log.info("newUser start.");
        return userService.newUser(reqNewUser);
    }

    /**
     * get user by signUserId.
     */
    @ApiOperation(value = "check user info exist", notes = "check user info exist")
    @GetMapping("/{signUserId}/userInfo")
    public Object getUserInfo(@PathVariable("signUserId") String signUserId) throws BaseException {
        log.info("getUserInfo start.");
        return userService.getUserBySignUserId(signUserId);
    }

    /**
     * get user list by app id
     */
    @ApiOperation(value = "get user list by app id", notes = "根据appId获取user列表")
    @GetMapping("/list/{appId}/{pageNumber}/{pageSize}")
    public Object getUserListByAppId(@PathVariable("appId") String appId,
            @PathVariable("pageNumber") Integer pageNumber,
            @PathVariable("pageSize") Integer pageSize) throws BaseException {
        log.info("getUserListByAppId start.");
        return userService.getUserListByAppId(appId, pageNumber, pageSize);
    }

    /**
     * deleteUser.
     * 
     * @param req
     * @return
     */
    @DeleteMapping("")
    public Object deleteUser(@Valid @RequestBody ReqUserInfo req, BindingResult result)
            throws BaseException {
        checkBindResult(result);
        log.info("deleteUser start.");
        return userService.deleteUser(req);
    }

}
