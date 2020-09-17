### v1.4.1

 (2020-09-25)

**Fix**

- 多活时分库分表空指针
- 升级依赖包：spring:4.3.28.RELEASE；elasticjob:3.0.0-alpha；zookeeper :3.6.2；web3sdk:2.5.1

**兼容性**

- 支持FISCO-BCOS v2.0.0-rc1 版本
- 支持FISCO-BCOS v2.0.0-rc2 版本
- 支持FISCO-BCOS v2.0.0-rc3 版本
- 支持FISCO-BCOS v2.0.0 及以上版本
- WeBASE-Sign v1.4.0+

详细了解，请阅读[**技术文档**](https://webasedoc.readthedocs.io/zh_CN/latest/)。

### v1.4.0

 (2020-08-06)


**Add**

- 增加返回 Version 版本接口

**兼容性**

- 支持FISCO-BCOS v2.0.0-rc1 版本
- 支持FISCO-BCOS v2.0.0-rc2 版本
- 支持FISCO-BCOS v2.0.0-rc3 版本
- 支持FISCO-BCOS v2.0.0 及以上版本
- WeBASE-Sign v1.4.0+

详细了解，请阅读[**技术文档**](https://webasedoc.readthedocs.io/zh_CN/latest/)。


### v1.3.2

 (2020-06-17)

**Fix**
- 移除Fastjson，替换为Jackson 2.11.0; web3sdk升级为2.4.1
- 升级依赖包：spring: 4.3.27; log4j: 2.13.3; slf4j: 1.7.30;

**兼容性**

- 支持FISCO-BCOS v2.0.0-rc1 版本
- 支持FISCO-BCOS v2.0.0-rc2 版本
- 支持FISCO-BCOS v2.0.0-rc3 版本
- 支持FISCO-BCOS v2.0.0 及以上版本
- WeBASE-Sign v1.3.0+

详细了解，请阅读[**技术文档**](https://webasedoc.readthedocs.io/zh_CN/latest/)。

### v1.3.1

 (2020-06-01)

**Add**
- 引入fisco-solcJ jar包，支持自动切换国密后台编译

**兼容性**

- 支持FISCO-BCOS v2.0.0-rc1 版本
- 支持FISCO-BCOS v2.0.0-rc2 版本
- 支持FISCO-BCOS v2.0.0-rc3 版本
- 支持FISCO-BCOS v2.0.0 及以上版本
- WeBASE-Sign v1.3.0+

详细了解，请阅读[**技术文档**](https://webasedoc.readthedocs.io/zh_CN/latest/)。

### v1.3.0

 (2020-04-29)

**Add**
- 使用签名服务进行交易签名时，传入`Integer userId`改为`String signUserId`（国密或非国密由对应user的加密类型决定）

**Fix**
- 升级fastjson, jackson, log4j

**兼容性**

- 支持FISCO-BCOS v2.0.0-rc1 版本
- 支持FISCO-BCOS v2.0.0-rc2 版本
- 支持FISCO-BCOS v2.0.0-rc3 版本
- 支持FISCO-BCOS v2.0.0 及以上版本
- WeBASE-Sign v1.3.0

详细了解，请阅读[**技术文档**](https://webasedoc.readthedocs.io/zh_CN/latest/)。

### v1.2.2

 (2020-01-02)

**Add**

- 支持国密
  - 发送交易、部署合约(sm2, sm3, solc-gm)
  - 新增`/encrypt`接口判断国密
- 新增查询接口支持查询部署信息与交易信息

**Fix**

- 优化：支持sdk线程池可配置
- 优化：web3sdk升级至v2.2.0
- 优化：start.sh启动脚本优化
- 优化：CommonUtils的`SignatureData`序列化支持国密


**兼容性**

- 支持FISCO-BCOS v2.0.0-rc1 版本
- 支持FISCO-BCOS v2.0.0-rc2 版本
- 支持FISCO-BCOS v2.0.0-rc3 版本
- 支持FISCO-BCOS v2.0.0 及以上版本
- WeBASE-Sign v1.2.2

详细了解，请阅读[**技术文档**](https://webasedoc.readthedocs.io/zh_CN/latest/)。


### v1.2.0

 (2019-09-30)

**Add**

- 分库分表
- 支持删除数据

详细了解，请阅读[**技术文档**](https://webasedoc.readthedocs.io/zh_CN/latest/)。



### v1.1.0

 (2019-09-12)

**Fix**

- bugfix：调用WeBASE-Sign时签名用户地址不一致
- bugfix：方法调用时校验方法名是否存在
- 优化：调用WeBASE-Sign时支持传入用户编号
- 优化：启停脚本通过程序名和端口校验进程

**兼容性**

- 支持FISCO-BCOS v2.0.0-rc1 版本
- 支持FISCO-BCOS v2.0.0-rc2 版本
- 支持FISCO-BCOS v2.0.0-rc3 版本
- 支持FISCO-BCOS v2.0.0 及以上版本
- WeBASE-Sign V1.1.0

详细了解，请阅读[**技术文档**](https://webasedoc.readthedocs.io/zh_CN/latest/)。



### V1.0.0

(2019-06-27)

**Add**

1. 适配FISCO-BCOS 2.0.0版本
2. 支持多群组功能
3. 支持合约编译，合约部署，合约调用
4. 可以查询交易的event和output
5. 缓存无状态交易，轮询上链
6. 支持本地配置私钥或随机私钥签名
7. 支持单机或集群部署
