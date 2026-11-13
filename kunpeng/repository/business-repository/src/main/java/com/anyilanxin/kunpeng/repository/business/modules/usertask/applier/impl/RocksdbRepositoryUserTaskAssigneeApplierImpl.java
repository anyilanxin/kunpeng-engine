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
package com.anyilanxin.kunpeng.repository.business.modules.usertask.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.MutableUserTaskRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.applier.RepositoryUserTaskApplier;

/**
 * 用户任务改派事件应用器：行更新审批人（assignee 来自请求参数，管理员动作）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryUserTaskAssigneeApplierImpl
    implements RepositoryUserTaskApplier<UserTaskRecord> {
  private final MutableUserTaskRepository userTask;

  public RocksdbRepositoryUserTaskAssigneeApplierImpl(final BusinessRepository repository) {
    userTask = repository.userTaskRepository();
  }

  @Override
  public UserTaskLifeCycle valueState() {
    return UserTaskLifeCycle.ASSIGNEE;
  }

  @Override
  public void applyState(final long key, final UserTaskRecord recordValue) {
    userTask.update(key, recordValue);
  }
}
