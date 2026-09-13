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
package com.anyilanxin.kunpeng.cluster.manager.business.raft.step;

import com.anyilanxin.kunpeng.cluster.business.step.RaftBootstrapStep;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.BusinessPartitionStartupContext;

/**
 * 业务分区启动流程中的引导（bootstrap）步骤，负责在初始成员上创建并引导业务 Raft 分组。
 *
 * @author zxuanhong
 */
public final class BusinessRaftBootstrapStep
    extends RaftBootstrapStep<BusinessPartitionStartupContext> {

  @Override
  public String getName() {
    return "Business Bootstrapped Raft Partition";
  }
}
