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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job;

import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate.JobBatchActivateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate.JobBatchActivateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.complete.JobCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.complete.JobCompleteResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapperRegister;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobBatchValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobValueLifeCycle;

/**
 * job 域 API Record 注册器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobApiRecordRegister {
  private JobApiRecordRegister() {}

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void register(final RecordValueMapperRegister valueMapper) {
    valueMapper
        .register(CommandApiJobValueLifeCycle.COMPLETE_REQUEST, JobCompleteRequestRecord::new)
        .register(CommandApiJobValueLifeCycle.COMPLETE_RESPONSE, JobCompleteResponseRecord::new)
        .register(
            CommandApiJobBatchValueLifeCycle.ACTIVATE_REQUEST, JobBatchActivateRequestRecord::new)
        .register(
            CommandApiJobBatchValueLifeCycle.ACTIVATE_RESPONSE,
            JobBatchActivateResponseRecord::new);
  }
}
