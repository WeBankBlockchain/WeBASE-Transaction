# 交易服务子系统说明

# 目录
> * [功能说明](#chapter-1)
> * [前提条件](#chapter-2)
> * [部署说明](#chapter-3)
> * [接口说明](#chapter-4)
> * [问题排查](#chapter-5)
> * [附录](#chapter-6)

# 1. <a id="chapter-1"></a>功能说明

本工程为交易服务子系统。功能：处理无状态交易，缓存上链。
交易请求分为同步请求和异步请求。同步请求会直接请求前置交易接口，返回处理结果。异步请求会将交易请求信息缓存到数据库，通过轮询服务向对应节点前置交易接口发送请求，确保交易成功上链。


# 2. <a id="chapter-2"></a>前提条件

| 环境     | 版本              |
| ------ | --------------- |
| Java   | jdk1.8.0_121或以上版本    |
| gradle | gradle-5.0或以上版本 |
| 数据库    | mysql-5.6或以上版本  |
备注：安装说明请参看附录。

# 3. <a id="chapter-3"></a>部署说明

## 3.1 拉取代码
执行命令：
```
git clone https://github.com/WeBankFinTech/webase-transcation.git
```

## 3.2 编译代码

（1）进入目录：
```shell
cd webase-transcation
```

（2）执行构建命令：
```shell
gradle build
```
构建完成后，会在根目录webase-transcation下生成已编译的代码目录dist。

## 3.3 修改配置

（1）进入目录：
```shell
cd dist/conf
```

（2）修改服务配置（没变化可以不修改）：
```shell
修改当前服务端口：sed -i "s/8082/${your_server_port}/g" application.yml
修改数据库IP：sed -i "s/10.0.0.1/${your_db_ip}/g" application.yml
修改数据库名称：sed -i "s/testDB/${your_db_name}/g" application.yml
修改数据库用户名：sed -i "s/root/${your_db_account}/g" application.yml
修改数据库密码：sed -i "s/123456/${your_db_password}/g" application.yml
修改前置服务ip端口：sed -i "s/10.0.0.1:8081/${your_ip_port}/g" application.yml
例子（将端口由8082改为8090）：sed -i "s/8082/8090/g" application.yml
```

## 3.4 服务启停
进入到已编译的代码根目录：
```shell
cd dist
```
```shell
启动：sh start.sh
停止：sh stop.sh
检查：sh status.sh
```

## 3.5 查看日志

进入到已编译的代码根目录：
```shell
cd dist
```

查看
```shell
tail -f log/webase-transcation.log
```
# 4. <a id="chapter-4"></a>接口说明

## 4.1 接口描述

通过合约信息进行调用，交易请求分为同步请求和异步请求。同步请求会直接请求前置交易接口，返回处理结果。异步请求会将交易请求信息缓存到数据库，通过轮询服务向对应节点前置交易接口发送请求，确保交易成功上链。

## 4.2 接口URL
```
http://localhost:8082/webase-transcation/trans/handle
```

## 4.3 调用方法
```
HTTP POST
```

## 4.4 请求参数

（1）参数表

| **序号** | **中文**         | **参数名**   | **类型**       | **最大长度** | **必填** | **说明**             |
| -------- | ---------------- | ------------ | -------------- | ------------ | -------- | -------------------- |
| 1        | 业务流水号       | uuid         | String         | 64           | 是       |                      |
| 2        | 用户编号         | userID       | Integer        | 16           | 是       |                      |
| 3        | 是否异步发送交易 | ifAsync      | Bool           |              | 是       | true-异步 false-同步 |
| 4        | 合约名称         | contractName | String         | 32           | 是       |                      |
| 5        | 合约版本         | version      | String         | 32           | 是       |                      |
| 6        | 方法名           | funcName     | String         | 32           | 是       |                      |
| 7        | 方法参数         | funcParam    | List\<Object\> |              |          | JSONArray，对应合约方法参数，多个参数以“,”分隔|

（2）数据格式
```
{
    "uuid":"1811221K702190000000000006402198",
    "userID":700001,
    "ifAsync":true,
    "contractName":"HelloWorld",
    "version":"1.0",
    "funcName":"set",
    "funcParam":["hello"]
}
```
（3）示例
```
curl -l -H "Content-type: application/json" -X POST -d '{"uuid": "1811221K702190000000000006402198", "userID": 700001, "ifAsync":true, "contractName": "HelloWorld", "version": "1.0", "funcName": "set", "funcParam": ["hello"]}' http://localhost:8082/webase-transcation/trans/handle
```

## 4.5 响应参数

（1）参数表

| **序号** | **中文** | **参数名** | **类型** | **最大长度** | **必填** | **说明**            |
| -------- | -------- | ---------- | -------- | ------------ | -------- | ------------------- |
| 1        | 返回码   | code       | String   |              | 是       |                     |
| 2        | 提示信息 | message    | String   |              | 是       |                     |
| 3        | 返回数据 | data       | Object   |              |          |                     |

（2）数据格式

a.异步请求正常返回结果
```
{
    "code": 0,
    "message": "success",
    "data": null
}
```

b.同步请求正常返回结果
```
{
"code": 0,
"message": "success",
"data": {
    "blockHash": "0x57080aae71f3f17ffc5fd924aaaa86dd0d8948548539752b6394a46d7fd85188",
    "gasUsed": 32734,
    "transactionIndexRaw": "0",
    "blockNumberRaw": "80",
    "blockNumber": 80,
    "contractAddress": "0x0000000000000000000000000000000000000000",
    "cumulativeGasUsed": 32734,
    "transactionIndex": 0,
    "gasUsedRaw": "0x7fde",
    "logs": [],
    "cumulativeGasUsedRaw": "0x7fde",
    "transactionHash": "0xf7290cb31483c792b8fb10175e92274148d10e4162588f05305aa85b246e8bed"
    }
}
```

c.异常返回结果示例
```
{
    "code": 203008,
    "message": "uuid is already exists",
    "data": null
}
```
# 5. <a id="chapter-5"></a>问题排查

## 5.1 编译错误
配置一下lombok，lombok的配置和使用请在网上查询。
```
> Task :compileJava
E:\webase-transcation\src\main\java\com\webank\webase\transaction\Application.java:21: 错误: 找不到符号
        log.info("start success...");
        ^
  符号:   变量 log
  位置: 类 Application
```

# 6. <a id="chapter-6"></a>附录

## 6.1 Java环境部署

此处给出简单步骤，供快速查阅。更详细的步骤，请参考[官网](http://www.oracle.com/technetwork/java/javase/downloads/index.html)。

（1）从[官网](http://www.oracle.com/technetwork/java/javase/downloads/index.html)下载对应版本的java安装包，并解压到相应目录

```shell
mkdir /software
tar -zxvf jdkXXX.tar.gz /software/
```

（2）配置环境变量

```shell
export JAVA_HOME=/software/jdk1.8.0_121
export PATH=$JAVA_HOME/bin:$PATH 
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
```

## 6.2 gradle环境部署

此处给出简单步骤，供快速查阅。更详细的步骤，请参考[官网](http://www.gradle.org/downloads)。

（1）从[官网](http://www.gradle.org/downloads)下载5.0或以上版本的gradle安装包，并解压到相应目录

```shell
mkdir /software/
unzip -d /software/ gradleXXX.zip
```

（2）配置环境变量

```shell
export GRADLE_HOME=/software/gradle-XXX
export PATH=$GRADLE_HOME/bin:$PATH
```

## 6.3 数据库部署

此处以Centos/Fedora为例。

（1）切换到root

```shell
sudo -s
```

（2）安装mysql

```shell
yum install mysql*
#某些版本的linux，需要安装mariadb，mariadb是mysql的一个分支
yum install mariadb*
```

（3）启动mysql

```shell
service mysqld start
#若安装了mariadb，则使用下面的命令启动
systemctl start mariadb.service
```

（4）初始化数据库用户

初次登录
```shell
mysql -u root
```

给root设置密码和授权远程访问
```sql
mysql > SET PASSWORD FOR 'root'@'localhost' = PASSWORD('123456');
mysql > GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '123456' WITH GRANT OPTION;
```

授权test用户本地访问数据库
```sql
mysql > create user 'test'@'localhost' identified by 'test1234';
```

（5）测试连接

另开一个ssh测试本地用户test是否可以登录数据库

```shell
mysql -utest -ptest1234 -h 10.0.0.1 -P 3306
```

登陆成功后，执行以下sql语句，若出现错误，则用户授权不成功

```sql
mysql > show databases;
mysql > use test;
```
（6）创建数据库

登录数据库

```shell
mysql -utest -ptest1234 -h 10.0.0.1 -P 3306
```

创建数据库

```sql
mysql > create database testDB;
```

