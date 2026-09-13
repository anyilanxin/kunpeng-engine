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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.query;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.query.AdminDispatchQueryResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;

/**
 * 管理面调度查询响应记录，携带当前调度计划与集群元数据快照。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class AdminDispatchQueryResponseRecord
    extends UnifiedRecordValue<AdminDispatchQueryResponseRecord>
    implements AdminDispatchQueryResponseRecordValue {
  // structpack-ids[AdminDispatchQueryResponseRecord]: 1,2
  private final ObjectProperty<AdminDispatchPlanRecord> dispatchPlanProp =
      new ObjectProperty<>(1, "DISPATCH_PLAN", new AdminDispatchPlanRecord());
  private final ObjectProperty<AdminClusterMetaRecord> clusterMetaProp =
      new ObjectProperty<>(2, "CLUSTER_META", new AdminClusterMetaRecord());

  public AdminDispatchQueryResponseRecord() {
    super(2);
    // formatting:off
    declareProperty(dispatchPlanProp)
      .declareProperty(clusterMetaProp);
    // formatting:on
  }

  /** 当前调度计划 */
  @Override
  public AdminDispatchPlanRecord getDispatchPlan() {
    return dispatchPlanProp.getValue();
  }

  public AdminDispatchQueryResponseRecord setDispatchPlan(
      final AdminDispatchPlanRecord dispatchPlan) {
    copyInto(dispatchPlan, dispatchPlanProp);
    return this;
  }

  /** 当前集群元数据快照 */
  @Override
  public AdminClusterMetaRecord getClusterMeta() {
    return clusterMetaProp.getValue();
  }

  public AdminDispatchQueryResponseRecord setClusterMeta(final AdminClusterMetaRecord clusterMeta) {
    copyInto(clusterMeta, clusterMetaProp);
    return this;
  }

  @Override
  protected AdminDispatchQueryResponseRecord newRecord() {
    return new AdminDispatchQueryResponseRecord();
  }
}
