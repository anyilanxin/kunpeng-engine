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
package com.anyilanxin.kunpeng.broker.client.jobstream;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import java.util.List;

/**
 * job 流推送的 wire 消息集：订阅快照对账 + 单条推送应答。除本组消息外 job 数据面不再有专属协议——拉取/完成复用标准命令通道。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobStreamMessages {
  private JobStreamMessages() {}

  /**
   * 网关订阅快照：generation 单调递增，broker 端按代次丢弃迟到旧快照（对账语义下乱序无害）。
   *
   * @param generation 快照代次
   * @param aggregates 当前全部聚合注册（每 jobType 一条）
   */
  public record SubscriptionSnapshot(long generation, List<Aggregate> aggregates) {
    /**
     * 单个 jobType 的聚合注册：sessions 为**稳定会话 ID → worker 映射**——流上线分配 ID（网关内自增
     * long），下线只删自身一项、其他会话位置不动（无重排），随快照全量对账。 broker 视图与推送扇出为 O(type)，与消费者数量解耦。
     */
    public record Aggregate(String jobType, List<Session> sessions) {}

    /** 单条流会话：稳定 ID + 归属 worker */
    public record Session(long sessionId, String worker) {}
  }

  /**
   * 单条推送载荷：完整 {@link JobRecord} 随行——网关据此外发 gRPC，broker 侧推送失败时也据此直接追加 WITHDRAW 回退命令。
   *
   * @param sessionId 目标会话 ID（稳定，网关 map O(1) 定位；对账窗口内查无由网关同 worker 兜底）
   * @param worker 引擎选定的 worker（已随 ACTIVATED 事件落 lockOwner；网关兜底只投同 worker 的流）
   * @param jobKey job 日志键（回退命令寻址）
   * @param partitionId job 所属分区（失败回退时定位分区写入器）
   * @param deadline 激活截止（推送路径在事件外旁路携带）
   * @param record 完整 job 记录
   */
  public record StreamPush(
      long sessionId,
      String worker,
      long jobKey,
      int partitionId,
      long deadline,
      JobRecord record) {}

  /** 推送应答：delivered=false 表示网关未能送达客户端流（流断/写失败），broker 侧据此回退 */
  public record PushResult(boolean delivered, String reason) {
    public static PushResult ok() {
      return new PushResult(true, null);
    }

    public static PushResult fail(final String reason) {
      return new PushResult(false, reason);
    }
  }
}
