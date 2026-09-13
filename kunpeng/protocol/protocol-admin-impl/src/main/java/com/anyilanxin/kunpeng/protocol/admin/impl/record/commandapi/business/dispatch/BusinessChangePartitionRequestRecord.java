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

import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.business.dispatch.BusinessChangePartitionRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.BooleanProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;

/**
 * 业务面分区变更调度请求记录，携带期望分区数与是否执行。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class BusinessChangePartitionRequestRecord
    extends UnifiedRecordValue<BusinessChangePartitionRequestRecord>
    implements BusinessChangePartitionRequestRecordValue {
  // structpack-ids[BusinessChangePartitionRequestRecord]: 2,5
  private final BooleanProperty applyPlanProp = new BooleanProperty(2, "APPLY_PLAN", false);
  private final IntegerProperty expectPartitionsCountProp =
      new IntegerProperty(5, "EXPECT_PARTITIONS_COUNT", 0);

  public BusinessChangePartitionRequestRecord() {
    super(2);
    // formatting:off
    declareProperty(applyPlanProp)
      .declareProperty(expectPartitionsCountProp);
    // formatting:on
  }

  /** 计划是否需要执行 */
  @Override
  public boolean isApplyPlan() {
    return applyPlanProp.getValue();
  }

  public BusinessChangePartitionRequestRecord setApplyPlan(final boolean applyPlan) {
    applyPlanProp.setValue(applyPlan);
    return this;
  }

  /** 期望分区数 */
  @Override
  public int getExpectPartitionsCount() {
    return expectPartitionsCountProp.getValue();
  }

  public BusinessChangePartitionRequestRecord setExpectPartitionsCount(
      final int expectPartitionsCount) {
    expectPartitionsCountProp.setValue(expectPartitionsCount);
    return this;
  }

  @Override
  protected BusinessChangePartitionRequestRecord newRecord() {
    return new BusinessChangePartitionRequestRecord();
  }
}
