/*
 * Copyright 2012-2019 the original author or authors.
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

package com.webank.webase.transaction.trans;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

/**
 * SignType.
 *
 */
@Service
public interface TransMapper {

    void createTbStatelessTrans();

    void insertTransInfo(TransInfoDto transInfoDto);
    
    TransInfoDto selectTransInfo(@Param("groupId") int groupId,
            @Param("uuidStateless") String uuidStateless);

    List<TransInfoDto> selectUnStatTrans(@Param("requestCountMax") int requestCountMax,
            @Param("selectCount") int selectCount, @Param("intervalTime") int intervalTime);

    List<TransInfoDto> selectUnStatTransByJob(@Param("requestCountMax") int requestCountMax,
            @Param("selectCount") int selectCount, @Param("intervalTime") int intervalTime,
            @Param("shardingTotalCount") int shardingTotalCount,
            @Param("shardingItem") int shardingItem);

    void updateRequestCount(@Param("id") Long id, @Param("requestCount") int requestCount);

    void updateHandleStatus(TransInfoDto transInfoDto);
}
