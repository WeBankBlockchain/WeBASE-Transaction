package com.webank.webase.transaction.config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import com.alibaba.fastjson.JSON;
import io.shardingsphere.api.algorithm.sharding.ListShardingValue;
import io.shardingsphere.api.algorithm.sharding.ShardingValue;
import io.shardingsphere.api.algorithm.sharding.complex.ComplexKeysShardingAlgorithm;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm {
    @Override
    public Collection<String> doSharding(Collection<String> collection, Collection<ShardingValue> shardingValues) {
        log.debug("collection:" + JSON.toJSONString(collection) + ",shardingValues:" + JSON.toJSONString(shardingValues));
        // 根据id和gmt_create双分片键来进行分表
        Collection<Long> idValues = getLongShardingValue(shardingValues, "id");
        Collection<Date> gmtValues = getDateShardingValue(shardingValues, "gmt_create");
        List<String> shardingSuffix = new ArrayList<>();
        for (Long idValue : idValues) {
            for (Date gmtValue : gmtValues) {
                Calendar cld = Calendar.getInstance();
                cld.setTime(gmtValue);
                int year = cld.get(Calendar.YEAR);
                String suffix = "_" + year + "_" + idValue % 2;
                collection.forEach(x -> {
                    if (x.endsWith(suffix)) {
                        shardingSuffix.add(x);
                    }
                });
            }
        }

        return shardingSuffix;
    }
    
    private Collection<Long> getLongShardingValue(Collection<ShardingValue> shardingValues, final String key) {
        Collection<Long> valueSet = new ArrayList<>();
        Iterator<ShardingValue> iterator = shardingValues.iterator();
        while (iterator.hasNext()) {
            ShardingValue next = iterator.next();
            if (next instanceof ListShardingValue) {
                ListShardingValue value = (ListShardingValue) next;
                if (value.getColumnName().equals(key)) {
                    return value.getValues();
                }
            }
        }
        return valueSet;
    }

    private Collection<Date> getDateShardingValue(Collection<ShardingValue> shardingValues, final String key) {
        Collection<Date> valueSet = new ArrayList<>();
        Iterator<ShardingValue> iterator = shardingValues.iterator();
        while (iterator.hasNext()) {
            ShardingValue next = iterator.next();
            if (next instanceof ListShardingValue) {
                ListShardingValue value = (ListShardingValue) next;
                if (value.getColumnName().equals(key)) {
                    return value.getValues();
                }
            }
        }
        return valueSet;
    }
}