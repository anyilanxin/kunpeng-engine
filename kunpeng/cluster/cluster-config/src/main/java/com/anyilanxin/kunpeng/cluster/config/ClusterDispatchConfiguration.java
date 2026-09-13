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
package com.anyilanxin.kunpeng.cluster.config;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import java.io.Serial;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 管理分区（admin partition）的配置信息，记录配置版本、是否本地管理模式、管理分区元数据以及当前节点是否为集群组建发起者。
 *
 * @author zxuanhong
 * @since
 */
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class ClusterDispatchConfiguration implements Serializable {
  @Serial private static final long serialVersionUID = 1787880551833L;

  private DispatchMeta dispatchMeta;

  private boolean haveDispatch;

  private long dispatchPlanId = -1;
  private long dispatchPlanExecutionId = -1;

  public ClusterDispatchConfiguration haveDispatch(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final PartitionExecutionType executionType,
      final byte[] bytes) {
    haveDispatch = true;
    this.dispatchPlanId = dispatchPlanId;
    this.dispatchPlanExecutionId = dispatchPlanExecutionId;
    dispatchMeta = new DispatchMeta(executionType, bytes);
    return this;
  }

  public ClusterDispatchConfiguration notDispatch() {
    haveDispatch = false;
    return this;
  }

  public ClusterDispatchConfiguration ackOk() {
    dispatchPlanId = -1;
    dispatchPlanExecutionId = -1;
    dispatchMeta = null;
    return this;
  }

  public boolean haveAck() {
    return dispatchPlanId > 0 && !haveDispatch;
  }

  public boolean haveDispatch() {
    return haveDispatch;
  }
}
