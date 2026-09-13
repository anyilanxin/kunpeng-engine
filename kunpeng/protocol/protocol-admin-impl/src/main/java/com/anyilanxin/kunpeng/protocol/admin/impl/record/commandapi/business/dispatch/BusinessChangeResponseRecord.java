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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.dispatch.BusinessChangeResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ObjectProperty;

/**
 * 业务面调度响应记录，携带生成的调度计划与业务集群元数据快照。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class BusinessChangeResponseRecord extends UnifiedRecordValue<BusinessChangeResponseRecord>
    implements BusinessChangeResponseRecordValue {
  // structpack-ids[BusinessChangeResponseRecord]: 1,2
  private final ObjectProperty<BusinessDispatchPlanRecord> dispatchPlanProp =
      new ObjectProperty<>(1, "DISPATCH_PLAN", new BusinessDispatchPlanRecord());
  private final ObjectProperty<BusinessClusterMetaRecord> clusterMetaProp =
      new ObjectProperty<>(2, "CLUSTER_META", new BusinessClusterMetaRecord());

  public BusinessChangeResponseRecord() {
    super(2);
    // formatting:off
    declareProperty(dispatchPlanProp)
      .declareProperty(clusterMetaProp);
    // formatting:on
  }

  /** 生成的调度计划 */
  @Override
  public BusinessDispatchPlanRecord getDispatchPlan() {
    return dispatchPlanProp.getValue();
  }

  public BusinessChangeResponseRecord setDispatchPlan(
      final BusinessDispatchPlanRecord dispatchPlan) {
    copyInto(dispatchPlan, dispatchPlanProp);
    return this;
  }

  /** 调度所基于的业务集群元数据快照 */
  @Override
  public BusinessClusterMetaRecord getClusterMeta() {
    return clusterMetaProp.getValue();
  }

  public BusinessChangeResponseRecord setClusterMeta(final BusinessClusterMetaRecord clusterMeta) {
    copyInto(clusterMeta, clusterMetaProp);
    return this;
  }

  @Override
  protected BusinessChangeResponseRecord newRecord() {
    return new BusinessChangeResponseRecord();
  }
}
