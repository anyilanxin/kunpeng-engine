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

import com.anyilanxin.kunpeng.broker.client.business.commandapi.job.JobBatchAbstractRequest;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate.JobBatchActivateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.CommandApiJobBatchValueLifeCycle;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import java.util.Collection;
import java.util.List;

/**
 * job 批量激活（拉取）请求。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class JobBatchActivateRequest
    extends JobBatchAbstractRequest<JobBatchActivateRequestRecord> {
  public JobBatchActivateRequest(final String jobType) {
    super(CommandApiJobBatchValueLifeCycle.ACTIVATE_REQUEST, new JobBatchActivateRequestRecord());
    getValue().setJobType(jobType);
  }

  public JobBatchActivateRequest setMaxJobsToActivate(final int maxJobsToActivate) {
    getValue().setMaxJobsActivate(maxJobsToActivate);
    return this;
  }

  public JobBatchActivateRequest setWorker(final String worker) {
    getValue().setWorker(worker);
    return this;
  }

  public JobBatchActivateRequest setTimeout(final long timeout) {
    getValue().setTimeout(timeout);
    return this;
  }

  public JobBatchActivateRequest setTenantIds(final Collection<String> tenantIds) {
    getValue().setTenantIds(tenantIds);
    return this;
  }

  public JobBatchActivateRequest setVariables(final List<String> fetchVariables) {
    final ValueArray<StringValue> variables = getValue().variables();
    fetchVariables.stream()
        .map(BufferUtil::wrapString)
        .forEach(buffer -> variables.add().wrap(buffer));
    return this;
  }
}
