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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask;

import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.assignee.UserTaskAssigneeRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.assignee.UserTaskAssigneeResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.cancel.UserTaskCancelRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.cancel.UserTaskCancelResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.claim.UserTaskClaimRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.claim.UserTaskClaimResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.create.UserTaskCreateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.create.UserTaskCreateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delegate.UserTaskDelegateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delegate.UserTaskDelegateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delete.UserTaskDeleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delete.UserTaskDeleteResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.own.UserTaskOwnRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.own.UserTaskOwnResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.update.UserTaskUpdateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.update.UserTaskUpdateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapperRegister;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;

/**
 * 用户任务域 API Record 注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskApiRecordRegister {
  private UserTaskApiRecordRegister() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(
            CommandApiUserTaskValueLifeCycle.ASSIGNEE_REQUEST, UserTaskAssigneeRequestRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.ASSIGNEE_RESPONSE, UserTaskAssigneeResponseRecord::new)
        .register(CommandApiUserTaskValueLifeCycle.CANCEL_REQUEST, UserTaskCancelRequestRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.CANCEL_RESPONSE, UserTaskCancelResponseRecord::new)
        .register(CommandApiUserTaskValueLifeCycle.CLAIM_REQUEST, UserTaskClaimRequestRecord::new)
        .register(CommandApiUserTaskValueLifeCycle.CLAIM_RESPONSE, UserTaskClaimResponseRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.COMPLETE_REQUEST, UserTaskCompleteRequestRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.COMPLETE_RESPONSE, UserTaskCompleteResponseRecord::new)
        .register(CommandApiUserTaskValueLifeCycle.CREATE_REQUEST, UserTaskCreateRequestRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.CREATE_RESPONSE, UserTaskCreateResponseRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.DELEGATE_REQUEST, UserTaskDelegateRequestRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.DELEGATE_RESPONSE, UserTaskDelegateResponseRecord::new)
        .register(CommandApiUserTaskValueLifeCycle.DELETE_REQUEST, UserTaskDeleteRequestRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.DELETE_RESPONSE, UserTaskDeleteResponseRecord::new)
        .register(CommandApiUserTaskValueLifeCycle.OWN_REQUEST, UserTaskOwnRequestRecord::new)
        .register(CommandApiUserTaskValueLifeCycle.OWN_RESPONSE, UserTaskOwnResponseRecord::new)
        .register(CommandApiUserTaskValueLifeCycle.UPDATE_REQUEST, UserTaskUpdateRequestRecord::new)
        .register(
            CommandApiUserTaskValueLifeCycle.UPDATE_RESPONSE, UserTaskUpdateResponseRecord::new);
  }
}
