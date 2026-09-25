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
package com.anyilanxin.kunpeng.broker.client.business.commandapi.job.request;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.JobAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.fail.JobFailRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobValueLifeCycle;

/**
 * job 失败请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobFailRequest extends JobAbstractRequest<JobFailRequestRecord> {

  public JobFailRequest(final long jobId, final int retries, final int retryBackOff) {
    super(CommandApiJobValueLifeCycle.FAIL_REQUEST, new JobFailRequestRecord());
    //    record.setJobId(jobId);
    //    record.setRetries(retries);
    //    record.setRetryBackOff(retryBackOff);
  }

  public JobFailRequest setErrorMessage(final String errorMessage) {
    //    record.setDeniedReason(errorMessage);
    return this;
  }
}
