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

package com.webank.webase.transaction.scheduler;

import com.webank.webase.transaction.base.ConstantProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * SchedulerConfig.
 *
 */
@Component
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ConstantProperties constants;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (constants.isIfDeleteData()) {
            taskRegistrar.addTriggerTask(() -> scheduleService.deleteDataSchedule(),
                (context) -> new CronTrigger(constants.getCronDeleteData())
                    .nextExecutionTime(context));
        }
        if (!constants.isIfDistributedTask()) {
            taskRegistrar.addTriggerTask(() -> scheduleService.deploySchedule(),
                (context) -> new CronTrigger(constants.getCronTrans())
                    .nextExecutionTime(context));
            taskRegistrar.addTriggerTask(() -> scheduleService.transSchedule(),
                (context) -> new CronTrigger(constants.getCronTrans())
                    .nextExecutionTime(context));
        }
    }

}
