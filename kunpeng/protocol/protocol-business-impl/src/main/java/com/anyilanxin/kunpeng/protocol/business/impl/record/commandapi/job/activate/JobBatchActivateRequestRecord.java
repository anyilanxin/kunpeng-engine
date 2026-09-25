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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.activate;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.activate.JobBatchActivateRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.agrona.DirectBuffer;

/**
 * job 批量激活请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobBatchActivateRequestRecord extends UnifiedRecordValue<JobBatchActivateRequestRecord>
    implements JobBatchActivateRequestRecordValue {
  // structpack-ids[JobBatchActivateRequestRecord]: 1,2,3,4,5,6
  private final LongProperty timeoutProp = new LongProperty(1, "TIMEOUT_KEY", -1);
  private final StringProperty jobTypeProp = new StringProperty(2, "JOB_TYPE");
  private final StringProperty workerProp = new StringProperty(3, "WORKER", "");
  private final IntegerProperty maxJobsActivateProp =
      new IntegerProperty(4, "MAX_JOBS_ACTIVATE", -1);
  private final ArrayProperty<StringValue> tenantIdsProp =
      new ArrayProperty<>(5, "TENANT_IDS", StringValue::new);
  private final ArrayProperty<StringValue> variablesProp =
      new ArrayProperty<>(6, "VARIABLES_KEY", StringValue::new);

  public JobBatchActivateRequestRecord() {
    super(6);
    declareProperty(timeoutProp)
        .declareProperty(jobTypeProp)
        .declareProperty(workerProp)
        .declareProperty(maxJobsActivateProp)
        .declareProperty(tenantIdsProp)
        .declareProperty(variablesProp);
  }

  @Override
  public JobBatchActivateRequestRecordValue setWorker(final String worker) {
    workerProp.setValue(wrapString(worker));
    return this;
  }

  @Override
  public JobBatchActivateRequestRecordValue setWorker(final DirectBuffer worker) {
    workerProp.setValue(worker);
    return this;
  }

  public String getWorker() {
    return bufferAsString(workerProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getWorkerBuffer() {
    return workerProp.getValue();
  }

  @Override
  public JobBatchActivateRequestRecordValue setJobType(final String jobType) {
    jobTypeProp.setValue(wrapString(jobType));
    return this;
  }

  @Override
  public JobBatchActivateRequestRecordValue setJobType(final DirectBuffer jobType) {
    jobTypeProp.setValue(jobType);
    return this;
  }

  public String getJobType() {
    return bufferAsString(jobTypeProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getJobTypeBuffer() {
    return jobTypeProp.getValue();
  }

  @Override
  public JobBatchActivateRequestRecordValue setMaxJobsActivate(final int maxJobsActivate) {
    maxJobsActivateProp.setValue(maxJobsActivate);
    return this;
  }

  public int getMaxJobsActivate() {
    return maxJobsActivateProp.getValue();
  }

  @Override
  public JobBatchActivateRequestRecordValue setTimeout(final long timeout) {
    timeoutProp.setValue(timeout);
    return this;
  }

  public long getTimeout() {
    return timeoutProp.getValue();
  }

  @Override
  public JobBatchActivateRequestRecordValue setTenantIds(final Collection<String> tenantIds) {
    tenantIdsProp.reset();
    tenantIds.forEach(tenantId -> tenantIdsProp.add().wrap(wrapString(tenantId)));
    return this;
  }

  public List<String> getTenantIds() {
    return StreamSupport.stream(tenantIdsProp.spliterator(), false)
        .map(StringValue::getValue)
        .map(BufferUtil::bufferAsString)
        .collect(Collectors.toList());
  }

  public ValueArray<StringValue> variables() {
    return variablesProp;
  }
}
