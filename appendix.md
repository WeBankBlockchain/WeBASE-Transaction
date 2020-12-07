# 附录

## 1. 安装示例

### 1.1 Java部署

此处给出OpenJDK安装简单步骤，供快速查阅。更详细的步骤，请参考[官网](https://openjdk.java.net/install/index.html)。

#### ① 安装包下载

从[官网](https://jdk.java.net/java-se-ri/11)下载对应版本的java安装包，并解压到服务器相关目录

```shell
mkdir /software
tar -zxvf openjdkXXX.tar.gz /software/
```

#### ② 配置环境变量

- 修改/etc/profile

```
sudo vi /etc/profile
```

- 在/etc/profile末尾添加以下信息

```shell
JAVA_HOME=/software/jdk-11
PATH=$PATH:$JAVA_HOME/bin
CLASSPATH==.:$JAVA_HOME/lib
export JAVA_HOME CLASSPATH PATH
```

- 重载/etc/profile

```
source /etc/profile
```

#### ③ 查看版本

```
java -version
```

## 2. 常见问题

### 2.1 脚本没权限

执行shell脚本报错误"permission denied"或格式错误

```
赋权限：chmod + *.sh
转格式：dos2unix *.sh
```

### 2.2 启动报错“Processing bcos message timeout”

```
 [main] ERROR SpringApplication() - Application startup failed
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'transService': Unsatisfied dependency expressed through field 'web3jMap'; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'web3j' defined in class path resource [com/webank/webase/transaction/config/Web3Config.class]: Bean instantiation via factory method failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [java.util.HashMap]: Factory method 'web3j' threw exception; nested exception is java.io.IOException: Processing bcos message timeout
```

答：一些Oracle JDK版本缺少相关包，导致节点连接异常。推荐使用OpenJDK，从[OpenJDK网站](https://jdk.java.net/java-se-ri/11)自行下载。

## 3. application.yml配置项说明

| 配置项                                             | 说明                         |
| -------------------------------------------------- | ---------------------------- |
| server.port                                        | 工程服务端口                 |
| server.servlet.context-path                        | 工程根URI                    |
| server.connection-timeout                          | url连接超时时间              |
| server.tomcat.max-threads                          | tomcat最大的工作线程数       |
| server.tomcat.max-connections                      | tomcat最大连接数             |
| server.tomcat.accept-count                         | tomcat最大等待数             |
| server.tomcat.min-spare-threads                    | tomcat最小的工作线程数       |
| spring.datasource.driver-class-name                | mysql驱动                    |
| spring.datasource.url                              | mysql连接地址                |
| spring.datasource.username                         | mysql账号                    |
| spring.datasource.password                         | mysql密码                    |
| constant.signServer                                | WeBASE-Sign签名服务ip端口    |
| constant.httpReadTimeOut                           | 传递数据的超时时间           |
| constant.httpConnectTimeOut                        | 建立连接的超时时间           |
| constant.maxRequestFail                            | 前置最大失败数               |
| constant.sleepWhenHttpMaxFail                      | 最多失败后sleep时间          |
| constant.frontGroupCacheClearTaskCron              | 前置群组缓存信息清除任务时间 |
| mybatis.typeAliasesPackage                         | mapper类扫描路径             |
| mybatis.configuration.map-underscore-to-camel-case | mybatis支持是否支持驼峰      |
| logging.config                                     | 日志配置文件目录             |
| logging.level                                      | 日志级别                     |
