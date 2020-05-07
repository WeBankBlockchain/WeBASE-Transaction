# 部署说明

## 1. 前提条件

| 环境        | 版本              |
| ----------- | ----------------- |
| FISCO-BCOS  | v2.0.0 及以上版本 |
| Java        | JDK8或以上版本    |
| WeBASE-Sign | bsn               |

**备注：**

-  Java推荐使用[OpenJDK](./appendix.html#java )，建议从[OpenJDK网站](https://jdk.java.net/java-se-ri/11) 自行下载（CentOS的yum仓库的OpenJDK缺少JCE(Java Cryptography Extension)，导致Web3SDK无法正常连接区块链节点）

- 安装说明请参看 [安装示例](./appendix.md#1-安装示例)


## 2. 拉取代码

执行命令：

```
git clone https://github.com/WeBankFinTech/WeBASE-Transaction.git -b proxy
```

进入目录：

```
cd WeBASE-Transaction
```

## 3. 编译代码

使用以下方式编译构建，如果出现问题可以查看 [常见问题](./appendix.md#2-常见问题)</br>
方式一：如果服务器已安装Gradle，且版本为gradle-4.10或以上

```shell
gradle build -x test
```

方式二：如果服务器未安装Gradle，或者版本不是gradle-4.10或以上，使用gradlew编译

```shell
chmod +x ./gradlew && ./gradlew build -x test
```

构建完成后，会在根目录WeBASE-Transaction下生成已编译的代码目录dist。

## 4. 修改配置

### 4.1 复制模板

进入编译目录dist：

```
cd dist
```

dist目录提供了一份配置模板conf_template：

```
根据配置模板生成一份实际配置conf。初次部署可直接拷贝。
例如：cp conf_template conf -r
```

### 4.2 复制证书

进入配置目录conf：

```shell
cd conf
```

将节点所在目录`nodes/${ip}/sdk`下的`ca.crt`、`node.crt`和`node.key`文件拷贝到当前conf目录，供SDK与节点建立连接时使用。

### 4.3 修改配置

**说明：** 根据实际情况修改，完整配置项说明请查看 [配置说明](./appendix.md#3-applicationyml配置项说明)

```shell
vi application.yml
```

```
server:
  port: 5003
  servlet:
    context-path: /WeBASE-Transaction

sdk:
  # 机构名
  orgName: webank
  timeout: 10000
  # 群组信息，可配置多群组和多节点
  groupConfig:
    allChannelConnections:
    - groupId: 1
      connectionsStr:
      - 127.0.0.1:23200
      - 127.0.0.1:23200
    - groupId: 2
      connectionsStr:
      - 127.0.0.1:23200
      - 127.0.0.1:23200

constant: 
  # WeBASE-Sign签名服务ip端口
  signServer: 127.0.0.1:5004

logging: 
  config: classpath:log4j2.xml
```

## 5. 服务启停

返回到dist目录执行：

```shell
启动：bash start.sh
停止：bash stop.sh
检查：bash status.sh
```

**备注**：服务进程起来后，需通过日志确认是否正常启动，出现以下内容表示正常；如果服务出现异常，确认修改配置后，重启提示服务进程在运行，则先执行stop.sh，再执行start.sh。

```
...
	Application() - main run success...
```

## 6. 查看日志

在dist目录查看：

```shell
交易服务日志：tail -f log/transaction.log
web3连接日志：tail -f log/web3sdk.log
```
