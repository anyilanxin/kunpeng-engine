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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step;

import com.anyilanxin.kunpeng.cluster.business.step.RaftFailureListenerStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.AdminPartitionStartupContext;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import java.util.List;

/**
 * 业务分区启动流程中的引导（bootstrap）步骤，负责在初始成员上创建并引导业务 Raft 分组。
 *
 * @author zxuanhong
 */
public final class AdminFailureListenerStep
    extends RaftFailureListenerStep<AdminPartitionStartupContext> {

  public AdminFailureListenerStep(final List<FailureListener> failureListeners) {
    super(failureListeners);
  }

  @Override
  public String getName() {
    return "Admin Failure Listener Step";
  }
}
