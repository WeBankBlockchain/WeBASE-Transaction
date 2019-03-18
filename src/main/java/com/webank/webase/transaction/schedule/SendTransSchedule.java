package com.webank.webase.transaction.schedule;

import com.webank.webase.transaction.base.ConstantProperties;
import com.webank.webase.transaction.trans.TransInfo;
import com.webank.webase.transaction.trans.TransMapper;
import com.webank.webase.transaction.trans.TransService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@Slf4j
@Service
public class SendTransSchedule {
    @Autowired
    private TransMapper transMapper;
    @Autowired
    private TransService transService;
    @Autowired
    private ConstantProperties properties;

    /**
     * schedule.
     */
    public void schedule() {
        log.debug("SendTransSchedule start...");
        List<TransInfo> transInfoList =
                transMapper.selectUnStatTrans(properties.getRequestCountMax(),
                        properties.getSelectCount(), properties.getIntervalTime());
        if (transInfoList == null || transInfoList.size() == 0) {
            log.info("no data was found in this schedule.");
        } else {
            transService.handleTransInfo(transInfoList);
        }
    }
}
