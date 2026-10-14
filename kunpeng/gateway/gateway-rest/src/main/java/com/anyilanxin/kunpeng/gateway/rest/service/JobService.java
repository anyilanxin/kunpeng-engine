/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.gateway.rest.service;

import com.anyilanxin.kunpeng.gateway.rest.controller.dto.JobCompleteDto;
import com.anyilanxin.kunpeng.gateway.rest.controller.dto.JobFailDto;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.JobVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.PullVo;
import java.util.List;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * job 业务：命令面（完成/失败）经标准命令通道进 raft；消费面为 SSE 推送流与长轮询拉取。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface JobService {

  /** 完成 job，写入完成变量。 */
  Mono<Void> complete(long jobKey, JobCompleteDto dto);

  /** 标记 job 失败并设置剩余重试次数与退避。 */
  Mono<Void> fail(long jobKey, JobFailDto dto);

  /** 修改 job 重试次数（引擎侧尚无支撑，当前恒抛未实现）。 */
  Mono<Void> updateRetries(long jobKey, int retries);

  /** 对 job 抛出业务错误（引擎侧尚无支撑，当前恒抛未实现）。 */
  Mono<Void> throwError(long jobKey, String errorCode, String errorMessage);

  /** 修改 job 处理超时（引擎侧尚无支撑，当前恒抛未实现）。 */
  Mono<Void> updateTimeout(long jobKey, long timeout);

  /**
   * 长轮询拉取 job：窗口内等到任务立即返回首批，到期无任务返回空批（不是失败）。
   *
   * @param type 消费类型
   * @param worker worker 名称
   * @param maxBatch 最多拉取数
   * @param deadlineMs 挂起窗口（ms），0 使用网关默认窗口
   * @param tenantIds 租户过滤
   */
  Mono<PullVo> pull(
      String type, String worker, int maxBatch, long deadlineMs, List<String> tenantIds);

  /**
   * SSE 推送流：注册即订阅（快照经 SWIM 元数据通道扩散到 broker），job 逐条以 {@code event:job} 推送， 周期性注释帧保活；客户端断开即注销会话，在途推送按
   * WITHDRAW 重派。
   *
   * @param jobType 消费类型
   * @param worker worker 名称
   * @param capacity 容量（轻量背压）
   * @param timeoutMs 单条 job 处理超时（ms）——推送路径的激活租期来源
   * @param tenantIds 租户范围
   * @param fetchVariables 期望接收的变量名
   * @param heartbeatMs 保活间隔（ms）
   */
  Flux<ServerSentEvent<JobVo>> stream(
      String jobType,
      String worker,
      int capacity,
      long timeoutMs,
      List<String> tenantIds,
      List<String> fetchVariables,
      long heartbeatMs);
}
