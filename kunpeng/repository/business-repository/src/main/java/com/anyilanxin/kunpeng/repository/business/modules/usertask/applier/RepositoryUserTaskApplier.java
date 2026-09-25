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
package com.anyilanxin.kunpeng.repository.business.modules.usertask.applier;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskRecordValue;
import com.anyilanxin.kunpeng.repository.business.BusinessApplier;

/**
 * 用户任务 Record 应用器接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface RepositoryUserTaskApplier<Record extends UserTaskRecordValue>
    extends BusinessApplier<Record> {

  @Override
  default ValueType valueType() {
    return ValueType.USER_TASK;
  }

  @Override
  UserTaskLifeCycle valueState();
}
