# 交易服务子系统说明

# 目录
> * [功能说明](#chapter-1)
> * [前提条件](#chapter-2)
> * [部署说明](#chapter-3)
> * [接口说明](#chapter-4)
> * [问题排查](#chapter-5)
> * [附录](#chapter-6)

# 1. <a id="chapter-1"></a>功能说明

本工程为交易服务子系统。功能：合约编译；交易请求处理，交易分为合约部署和普通的合约调用请求。<br>

合约编译：上传合约文件zip压缩包，返回合约编译信息<br>

合约部署：交易服务子系统会同步向节点发送交易请求，处理完成后返回部署的合约地址。<br>

合约调用：分为无状态交易上链（非constant方法）和交易结果查询（constant方法）。<br>
无状态交易上链是交易服务子系统会将交易请求信息缓存到数据库，通过轮询服务向节点发送交易请求，确保交易成功上链。支持分布式任务多活部署（使用分布式任务的话需部署zookeeper）。<br>
交易结果查询是交易服务子系统会同步向节点发送交易请求，返回结果。<br>

无状态交易上链数据签名支持以下模式：<br>
1、本地配置私钥签名<br>
2、本地随机私钥签名<br>
3、调用云端签名服务签名<br>


# 2. <a id="chapter-2"></a>前提条件

| 环境     | 版本              |
| ------ | --------------- |
| Java   | jdk1.8.0_121或以上版本    |
| gradle | gradle-5.0或以上版本 |
| zookeeper | zookeeper-3.4.10或以上版本 |
| 数据库    | mysql-5.6或以上版本  |
备注：安装说明请参看附录，不使用分布式任务可以不部署zookeeper。

# 3. <a id="chapter-3"></a>部署说明

## 3.1 拉取代码
执行命令：
```
git clone https://github.com/WeBankFinTech/webase-transcation.git
```

**注意**：代码拉取后，可以切换到相应分支（如：dev）。

```shell
cd webase-transcation
git checkout dev
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
修改数据库名称：sed -i "s/testdb/${your_db_name}/g" application.yml
修改数据库用户名：sed -i "s/root/${your_db_account}/g" application.yml
修改数据库密码：sed -i "s/123456/${your_db_password}/g" application.yml
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
**备注**：如果脚本执行错误，尝试以下命令:
```
赋权限：chmod + *.sh
转格式：dos2unix *.sh
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

- [接口说明请点击](interface.md)

# 5. <a id="chapter-5"></a>问题排查

## 5.1 gradle build失败

如果出现以下错误，**请配置一下lombok**，lombok的配置和使用请在网上查询。
```
> Task :compileJava
E:\webase-transcation\src\main\java\com\webank\webase\transaction\Application.java:21: 错误: 找不到符号
        log.info("start success...");
        ^
  符号:   变量 log
  位置: 类 Application
```

如果出现以下错误，**请检查gradle版本，需要使用5.0或以上版本。**
```
> Could not find method annotationProcessor() for arguments [org.projectlombok:lombok:1.18.2] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.
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
mysql > create database testdb;
```

