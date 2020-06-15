/*
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

package com.webank.webase.transaction.config;

import com.webank.webase.transaction.util.JsonUtils;
import io.shardingsphere.api.algorithm.sharding.ListShardingValue;
import io.shardingsphere.api.algorithm.sharding.ShardingValue;
import io.shardingsphere.api.algorithm.sharding.complex.ComplexKeysShardingAlgorithm;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * ComplexShardingAlgorithm.
 *
 */
@Slf4j
public class MyComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm {
    @Override
    public Collection<String> doSharding(Collection<String> collection,
            Collection<ShardingValue> shardingValues) {
        log.debug("collection:" + JsonUtils.toJSONString(collection) + ",shardingValues:"
                + JsonUtils.toJSONString(shardingValues));
        // table sharding by id and gmt_create
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

    private Collection<Long> getLongShardingValue(Collection<ShardingValue> shardingValues,
            final String key) {
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

    private Collection<Date> getDateShardingValue(Collection<ShardingValue> shardingValues,
            final String key) {
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
