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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.client.core;

/**
 * broker 传输层失败应答错误码（{@code BrokerResponse.code} int 通道）：流控/背压/leader 切换类拒绝。各码是否可重试见常量说明。
 *
 * <p>与 record 级业务拒绝（RejectionType）分属两层，不携带业务语义；wire 格式与编号池零改动。多数码表达"这条命令没能进入日志"；例外是 {@link
 * #LEADER_STEPPED_DOWN}，命令可能已进入日志。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BrokerResponseCodes {
  /** 日志写入被拒（在途窗口耗尽/事件日志拒绝追加） */
  public static final int APPEND_REJECTED = 1;

  /** sink 滞后背压闸门拒绝（导出积压越过配置阈值，落库排空后自动恢复） */
  public static final int SINK_BACKPRESSURE = 2;

  /**
   * 分区冻结拒绝（数据手术窗口，writer 级权威拒绝）：手术计划 RESUME 后自动恢复，调用方可重试退避。 与 SWIM
   * 拓扑冻结通告（建议层）同义——通告让调用方根本不发，本码接住漏网的。
   */
  public static final int PARTITION_FROZEN = 3;

  /**
   * leader 转移窗口拒绝：分区正在把 leader 交给别的节点，秒级窗口，命令没有进入日志。调用方可立即重试，网关会自动重试到新 leader。 与 {@link
   * #PARTITION_FROZEN} 分开，因为数据手术持续分钟到小时，不该原地重试。
   */
  public static final int NOT_LEADER = 4;

  /** 在途请求因 leader 变更被终止：命令可能已进入日志并被新 leader 执行，结果未知。调用方不应盲目重试非幂等命令（重试创建流程实例会建出第二个）。 */
  public static final int LEADER_STEPPED_DOWN = 5;

  private BrokerResponseCodes() {}
}
