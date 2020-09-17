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

package com.webank.webase.transaction.job;

import javax.annotation.Resource;

import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.lite.api.bootstrap.impl.ScheduleJobBootstrap;
import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.webank.webase.transaction.util.CommonUtils;

import io.shardingsphere.core.keygen.DefaultKeyGenerator;
import lombok.Data;

/**
 * DataflowJobConfig.
 * 
 */
@Data
@Configuration
@ConditionalOnProperty(value = {"constant.ifDistributedTask"}, havingValue = "true", matchIfMissing = false)
@EnableTransactionManagement(proxyTargetClass = true)
@ConfigurationProperties(prefix = "job.dataflow")
public class DataflowJobConfig implements InitializingBean {
	
	@Resource
	private CoordinatorRegistryCenter regCenter;
	@Autowired
	private DeployHandleDataflowJob deployHandleDataflowJob;
	@Autowired
	private TransHandleDataflowJob transHandleDataflowJob;

	private int shardingTotalCount;
	@Value("${constant.cronTrans}")
	private String cronMonitor;
	
	static {
        DefaultKeyGenerator.setWorkerId(CommonUtils.getWorkerId());
    }

	@Override
	public void afterPropertiesSet() throws Exception {
		ScheduleJobBootstrap deploySchedule = new ScheduleJobBootstrap(regCenter, deployHandleDataflowJob,
				deployDataflowConfig());
		deploySchedule.schedule();
		ScheduleJobBootstrap transSchedule = new ScheduleJobBootstrap(regCenter, transHandleDataflowJob,
				transDataflowConfig());
		transSchedule.schedule();
	}

	/**
	 * deployDataflowConfig.
	 * 
	 * @return
	 */
	private JobConfiguration deployDataflowConfig() {
		JobConfiguration jobConfiguration = JobConfiguration
				.newBuilder(DeployHandleDataflowJob.class.getName(), shardingTotalCount).cron(cronMonitor).build();
		return jobConfiguration;
	}

	/**
	 * transDataflowConfig.
	 * 
	 * @return
	 */
	private JobConfiguration transDataflowConfig() {
		JobConfiguration jobConfiguration = JobConfiguration
				.newBuilder(TransHandleDataflowJob.class.getName(), shardingTotalCount).cron(cronMonitor).build();
		return jobConfiguration;
	}
}
