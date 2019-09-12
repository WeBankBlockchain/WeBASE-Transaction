### V1.1.0

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
- 支持FISCO-BCOS v2.0.0 版本
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