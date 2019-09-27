-- ----------------------------
-- table template
-- ----------------------------
-- "部署交易信息表"：缓存部署请求信息，再轮询上链
-- ----------------------------
-- CREATE TABLE tb_deploy_transaction (
--     id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
--     group_id int(11) NOT NULL COMMENT '群组编号',
--     uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
--     contract_bin text NOT NULL COMMENT '合约bin',
--     contract_abi text NOT NULL COMMENT '合约abi',
--     contract_address varchar(64) COMMENT '合约地址',
--     func_param text COMMENT '构造方法的入参json',
--     sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
--     sign_user_id int(11) COMMENT '调用签名服务的用户编号',
--     request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
--     handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
--     trans_hash varchar(128) COMMENT '交易hash值',
--     receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
--     gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
--     gmt_modify datetime NOT NULL COMMENT '修改时间',
--     PRIMARY KEY (id),
--     KEY idx_gu (group_id,uuid_deploy),
--     KEY idx_hs (handle_status),
--     KEY idx_gc (gmt_create)
-- ) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;
-- ----------------------------
-- "合约无状态交易请求信息表"：缓存调用合约的无状态方法请求信息，再轮询上链
-- ----------------------------
-- CREATE TABLE  tb_stateless_transaction (
--     id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
--     group_id int(11) NOT NULL COMMENT '群组编号',
--     uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
--     uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
--     contract_abi text NOT NULL COMMENT '合约abi',
--     contract_address varchar(64) NOT NULL COMMENT '合约地址',
--     func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
--     func_param text COMMENT '合约方法的入参json',
--     sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
--     sign_user_id int(11) COMMENT '调用签名服务的用户编号',
--     request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
--     handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
--     trans_hash varchar(128) COMMENT '交易hash值',
--     trans_output text COMMENT '交易返回值',
--     receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
--     gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
--     gmt_modify datetime NOT NULL COMMENT '修改时间',
--     PRIMARY KEY (id),
--     KEY idx_gu (group_id,uuid_stateless),
--     KEY idx_hs (handle_status),
--     KEY idx_gc (gmt_create)
-- ) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;
-- ----------------------------


-- ----------------------------
-- create database
-- ----------------------------
CREATE DATABASE IF NOT EXISTS webasetransaction0;
CREATE DATABASE IF NOT EXISTS webasetransaction1;

-- ----------------------------
-- use webasetransaction0
-- ----------------------------
use webasetransaction0;
-- ----------------------------
-- create table
-- ----------------------------
DROP TABLE IF EXISTS tb_deploy_transaction_2019_0;
CREATE TABLE tb_deploy_transaction_2019_0 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
    contract_bin text NOT NULL COMMENT '合约bin',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) COMMENT '合约地址',
    func_param text COMMENT '构造方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_deploy),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_deploy_transaction_2019_1;
CREATE TABLE tb_deploy_transaction_2019_1 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
    contract_bin text NOT NULL COMMENT '合约bin',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) COMMENT '合约地址',
    func_param text COMMENT '构造方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_deploy),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_deploy_transaction_2020_0;
CREATE TABLE tb_deploy_transaction_2020_0 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
    contract_bin text NOT NULL COMMENT '合约bin',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) COMMENT '合约地址',
    func_param text COMMENT '构造方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_deploy),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_deploy_transaction_2020_1;
CREATE TABLE tb_deploy_transaction_2020_1 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
    contract_bin text NOT NULL COMMENT '合约bin',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) COMMENT '合约地址',
    func_param text COMMENT '构造方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_deploy),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_stateless_transaction_2019_0;
CREATE TABLE  tb_stateless_transaction_2019_0 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
    uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) NOT NULL COMMENT '合约地址',
    func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
    func_param text COMMENT '合约方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    trans_output text COMMENT '交易返回值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_stateless),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_stateless_transaction_2019_1;
CREATE TABLE tb_stateless_transaction_2019_1 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
    uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) NOT NULL COMMENT '合约地址',
    func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
    func_param text COMMENT '合约方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    trans_output text COMMENT '交易返回值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_stateless),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_stateless_transaction_2020_0;
CREATE TABLE  tb_stateless_transaction_2020_0 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
    uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) NOT NULL COMMENT '合约地址',
    func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
    func_param text COMMENT '合约方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    trans_output text COMMENT '交易返回值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_stateless),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_stateless_transaction_2020_1;
CREATE TABLE tb_stateless_transaction_2020_1 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
    uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) NOT NULL COMMENT '合约地址',
    func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
    func_param text COMMENT '合约方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    trans_output text COMMENT '交易返回值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_stateless),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;


-- use webasetransaction1
use webasetransaction1;
-- create table
DROP TABLE IF EXISTS tb_deploy_transaction_2019_0;
CREATE TABLE tb_deploy_transaction_2019_0 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
    contract_bin text NOT NULL COMMENT '合约bin',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) COMMENT '合约地址',
    func_param text COMMENT '构造方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_deploy),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_deploy_transaction_2019_1;
CREATE TABLE tb_deploy_transaction_2019_1 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
    contract_bin text NOT NULL COMMENT '合约bin',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) COMMENT '合约地址',
    func_param text COMMENT '构造方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_deploy),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_deploy_transaction_2020_0;
CREATE TABLE tb_deploy_transaction_2020_0 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
    contract_bin text NOT NULL COMMENT '合约bin',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) COMMENT '合约地址',
    func_param text COMMENT '构造方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_deploy),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_deploy_transaction_2020_1;
CREATE TABLE tb_deploy_transaction_2020_1 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_deploy varchar(64) NOT NULL COMMENT '部署交易业务流水号',
    contract_bin text NOT NULL COMMENT '合约bin',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) COMMENT '合约地址',
    func_param text COMMENT '构造方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_deploy),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='部署交易信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_stateless_transaction_2019_0;
CREATE TABLE  tb_stateless_transaction_2019_0 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
    uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) NOT NULL COMMENT '合约地址',
    func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
    func_param text COMMENT '合约方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    trans_output text COMMENT '交易返回值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_stateless),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_stateless_transaction_2019_1;
CREATE TABLE tb_stateless_transaction_2019_1 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
    uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) NOT NULL COMMENT '合约地址',
    func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
    func_param text COMMENT '合约方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    trans_output text COMMENT '交易返回值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_stateless),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_stateless_transaction_2020_0;
CREATE TABLE  tb_stateless_transaction_2020_0 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
    uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) NOT NULL COMMENT '合约地址',
    func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
    func_param text COMMENT '合约方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    trans_output text COMMENT '交易返回值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_stateless),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;

DROP TABLE IF EXISTS tb_stateless_transaction_2020_1;
CREATE TABLE tb_stateless_transaction_2020_1 (
    id bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    group_id int(11) NOT NULL COMMENT '群组编号',
    uuid_stateless varchar(64) NOT NULL COMMENT '合约无状态交易业务流水号',
    uuid_deploy varchar(64) COMMENT '部署交易业务流水号',
    contract_abi text NOT NULL COMMENT '合约abi',
    contract_address varchar(64) NOT NULL COMMENT '合约地址',
    func_name varchar(32) NOT NULL COMMENT '调用的合约方法名',
    func_param text COMMENT '合约方法的入参json',
    sign_type tinyint(4) DEFAULT '0' COMMENT '签名 类型(0-本地配置私钥，1-本地随机私钥，2-调用签名服务)',
    sign_user_id int(11) COMMENT '调用签名服务的用户编号',
    request_count tinyint(4) DEFAULT '0' COMMENT '请求次数',
    handle_status tinyint(4) DEFAULT '0' COMMENT '处理状态(0-待处理，1-处理成功)',
    trans_hash varchar(128) COMMENT '交易hash值',
    trans_output text COMMENT '交易返回值',
    receipt_status tinyint(1) DEFAULT NULL COMMENT '交易回执状态(0-异常，1-正常)',
    gmt_create timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    gmt_modify datetime NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    KEY idx_gu (group_id,uuid_stateless),
    KEY idx_hs (handle_status),
    KEY idx_gc (gmt_create)
) COMMENT='合约无状态交易请求信息表' ENGINE=InnoDB CHARSET=utf8;
