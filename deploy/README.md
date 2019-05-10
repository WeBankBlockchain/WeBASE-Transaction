# 一键部署说明

## 1、前提条件

| 环境   | 版本                   |
| ------ | ---------------------- |
| Java   | jdk1.8.0_121或以上版本 |
| python | 2.7                    |
| 数据库 | mysql-5.6或以上版本    |

## 2、拉取代码

获取安装包：
```shell
wget https://github.com/FISCO-BCOS/webase-transcation/releases/download/webase-transcation.zip
```
解压安装包：
```shell
unzip webase-transcation.zip
```

## 3、修改配置：

**注意：**1、数据库服务和数据需要提前准备。2、服务端口不能小于1024

（1）进入目录：
```shell
cd webase-transcation/dist/conf
```
**将本机构区块链节点sdk目录下的以下文件复制到当前目录：**
ca.crt、node.crt、node.key

（2）以下有注释的地方根据实际情况修改：
```shell
vi application.yml
```
```
server: 
  # 本工程服务端口，端口被占用则修改
  port: 8082
  context-path: /webase-transaction

spring: 
  datasource: 
    # 数据库连接信息
    url: jdbc:mysql://127.0.0.1:3306/testdb?useUnicode=true&characterEncoding=utf8
    # 数据库用户名
    username: root
    # 数据库密码（123456仅为样例，强烈建议设置成复杂密码）
    password: 123456
    driver-class-name: com.mysql.jdbc.Driver

sdk:
  # 机构名
  orgName: webank
  timeout: 10000
  # 群组信息，可配置多群组和多节点
  groupConfig:
    allChannelConnections:
    - groupId: 1
      connectionsStr:
      - 127.0.0.1:20200
      - 127.0.0.1:20201
    - groupId: 2
      connectionsStr:
      - 127.0.0.1:20200
      - 127.0.0.1:20201

constant: 
  # 签名服务url，需要调用签名服务进行签名的话则对应修改，使用本地签名的话可以不修改
  signServiceUrl: http://127.0.0.1:8085/webase-sign/sign
  # 本地配置私钥进行签名，使用这种模式则对应修改
  privateKey: edf02a4a69b14ee6b1650a95de71d5f50496ef62ae4213026bd8d6651d030995
  cronTrans: 0/1 * * * * ?
  requestCountMax: 6
  selectCount: 10
  intervalTime: 600
  sleepTime: 50
  # 使用分布式任务部署多活（true-是，false-否）
  ifDistributedTask: false

job:
  regCenter:  
    # 部署多活的话需配置zookeeper，支持集群
    serverLists: 127.0.0.1:2181
    namespace: elasticjob-trans
  dataflow:  
    # 分片数（如多活3个的话可分成3片）
    shardingTotalCount: 3
```

## 4、部署
进入工程根目录：
```shell
cd webase-transcation
```
使用以下命令进行部署：
```shell
python deploy.py start
```
相关说明可查看帮助：
```shell
python deploy.py help
```

## 5、日志路径
```
部署日志：log/
服务日志：dist/log/
```


