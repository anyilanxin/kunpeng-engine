/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.client.command.job;

import com.anyilanxin.kunpeng.client.command.CommandWithOneOrMoreTenantsStep;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * 流式 job 订阅命令的分层构建接口
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface StreamJobsCommandStep1 {

  /**
   * 订阅的 job 类型；只有该类型的 job 会被激活并推送到消费回调
   *
   * @param jobType job 类型（必填）
   */
  StreamJobsCommandStep2 jobType(String jobType);

  /** 第二步：指定消费回调 */
  interface StreamJobsCommandStep2 {

    /**
     * 设置推送回调（线程安全，由流式响应线程调用）
     *
     * @param consumer 每条激活 job 的消费者
     */
    StreamJobsCommandStep3 consumer(final Consumer<ActivatedJob> consumer);
  }

  /** 第三步：可选参数与发送 */
  interface StreamJobsCommandStep3
      extends CommandWithOneOrMoreTenantsStep<StreamJobsCommandStep3>,
          FinalCommandStep<StreamJobsResponse> {

    /**
     * job 激活独占超时
     *
     * @param timeout 超时时长
     */
    StreamJobsCommandStep3 timeout(Duration timeout);

    /**
     * 执行该类 job 的 worker 名称（用于审计与监控）
     *
     * @param workerName worker 名称
     */
    StreamJobsCommandStep3 workerName(String workerName);

    /**
     * 推送中需要一并获取的流程变量白名单
     *
     * @param fetchVariables 变量名列表
     */
    StreamJobsCommandStep3 fetchVariables(List<String> fetchVariables);

    /**
     * 推送中需要一并获取的流程变量白名单
     *
     * @param fetchVariables 变量名
     */
    StreamJobsCommandStep3 fetchVariables(String... fetchVariables);
  }
}
