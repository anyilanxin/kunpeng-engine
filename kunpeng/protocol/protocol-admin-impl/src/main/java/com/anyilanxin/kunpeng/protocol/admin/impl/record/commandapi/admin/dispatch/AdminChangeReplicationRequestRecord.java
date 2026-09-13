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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch;

import com.anyilanxin.kunpeng.protocol.admin.record.commandapi.admin.dispatch.AdminChangeReplicationRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.BooleanProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;

/**
 * 管理面调度请求记录，携带期望副本数与是否执行。
 *
 * @author zxuanhong
 * @since
 */
@AutoDeclareProperties
public class AdminChangeReplicationRequestRecord
    extends UnifiedRecordValue<AdminChangeReplicationRequestRecord>
    implements AdminChangeReplicationRequestRecordValue {
  // structpack-ids[AdminChangeReplicationRequestRecord]: 2,5
  private final BooleanProperty applyPlanProp = new BooleanProperty(2, "APPLY_PLAN", false);
  private final IntegerProperty expectReplicationFactorProp =
      new IntegerProperty(5, "EXPECT_REPLICATION_FACTOR", 0);

  public AdminChangeReplicationRequestRecord() {
    super(2);
    // formatting:off
    declareProperty(applyPlanProp)
      .declareProperty(expectReplicationFactorProp);
    // formatting:on
  }

  /** 计划是否需要执行 */
  @Override
  public boolean isApplyPlan() {
    return applyPlanProp.getValue();
  }

  public AdminChangeReplicationRequestRecord setApplyPlan(final boolean applyPlan) {
    applyPlanProp.setValue(applyPlan);
    return this;
  }

  /** 期望副本数 */
  @Override
  public int getExpectReplicationFactor() {
    return expectReplicationFactorProp.getValue();
  }

  public AdminChangeReplicationRequestRecord setExpectReplicationFactor(
      final int expectReplicationFactor) {
    expectReplicationFactorProp.setValue(expectReplicationFactor);
    return this;
  }

  @Override
  protected AdminChangeReplicationRequestRecord newRecord() {
    return new AdminChangeReplicationRequestRecord();
  }
}
