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

import com.anyilanxin.kunpeng.cluster.business.step.RaftJoinStep;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.BusinessPartitionStartupContext;

/**
 * 业务分区启动流程中的加入（join）步骤，负责让当前成员加入已存在的业务 Raft 分组。
 *
 * @author zxuanhong
 */
public class BusinessRaftJoinStep extends RaftJoinStep<BusinessPartitionStartupContext> {

  @Override
  public String getName() {
    return "Business Joining Raft Partition";
  }
}
