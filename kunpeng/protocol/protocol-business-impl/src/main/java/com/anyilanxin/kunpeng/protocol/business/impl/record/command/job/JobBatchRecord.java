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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.job;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobBatchRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.structpack.value.LongValue;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.agrona.DirectBuffer;

/**
 * 批量激活 job 的载体记录：一次激活请求所产出的已激活 job 列表。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobBatchRecord extends UnifiedRecordValue<JobBatchRecord>
    implements JobBatchRecordValue {
  private final LongProperty batchJobIdProp = new LongProperty(1, "BATCH_JOB_ID", -1);
  private final LongProperty timeoutProp = new LongProperty(2, "TIMEOUT_KEY", -1);
  private final StringProperty workerProp = new StringProperty(3, "WORKER", "");
  private final StringProperty jobTypeProp = new StringProperty(4, "JOB_TYPE");
  private final IntegerProperty maxJobsActivateProp =
      new IntegerProperty(5, "MAX_JOBS_ACTIVATE", -1);
  private final ArrayProperty<LongValue> jobKeysProp =
      new ArrayProperty<>(6, "JOB_KEYS_KEY", LongValue::new);
  private final ArrayProperty<StringValue> tenantIdsProp =
      new ArrayProperty<>(7, "TENANT_IDS", StringValue::new);
  private final ArrayProperty<StringValue> variablesProp =
      new ArrayProperty<>(8, "VARIABLES_KEY", StringValue::new);

  public JobBatchRecord() {
    super(8);
    declareProperty(batchJobIdProp)
        .declareProperty(timeoutProp)
        .declareProperty(workerProp)
        .declareProperty(jobTypeProp)
        .declareProperty(maxJobsActivateProp)
        .declareProperty(jobKeysProp)
        .declareProperty(tenantIdsProp)
        .declareProperty(variablesProp);
  }

  @Override
  public long getBatchJobId() {
    return batchJobIdProp.getValue();
  }

  public JobBatchRecord setBatchJobId(final long batchJobId) {
    batchJobIdProp.setValue(batchJobId);
    return this;
  }

  @Override
  public long getTimeout() {
    return timeoutProp.getValue();
  }

  public JobBatchRecord setDeadline(final long deadline) {
    timeoutProp.setValue(deadline);
    return this;
  }

  @Override
  public String getWorker() {
    return bufferAsString(workerProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getWorkerBuffer() {
    return workerProp.getValue();
  }

  public JobBatchRecord setWorker(final String worker) {
    workerProp.setValue(wrapString(worker));
    return this;
  }

  public JobBatchRecord setWorker(final DirectBuffer worker) {
    workerProp.setValue(worker);
    return this;
  }

  @Override
  public int getMaxJobsActivate() {
    return maxJobsActivateProp.getValue();
  }

  public JobBatchRecord setMaxJobsActivate(final int maxJobsActivate) {
    maxJobsActivateProp.setValue(maxJobsActivate);
    return this;
  }

  public ValueArray<LongValue> jobKeys() {
    return jobKeysProp;
  }

  @Override
  public List<Long> getJobKeys() {
    final List<Long> keys = new ArrayList<>();
    for (final LongValue jobKey : jobKeysProp) {
      keys.add(jobKey.getValue());
    }
    return keys;
  }

  @Override
  public List<String> getTenantIds() {
    final List<String> tenantIds = new ArrayList<>();
    for (final StringValue tenantId : tenantIdsProp) {
      tenantIds.add(BufferUtil.bufferAsString(tenantId.getValue()));
    }
    return tenantIds;
  }

  public JobBatchRecord setTenantIds(final Collection<String> tenantIds) {
    tenantIdsProp.reset();
    tenantIds.forEach(tenantId -> tenantIdsProp.add().wrap(BufferUtil.wrapString(tenantId)));
    return this;
  }

  public String getJobType() {
    return bufferAsString(jobTypeProp.getValue());
  }

  @JsonIgnore
  public DirectBuffer getJobTypeBuffer() {
    return jobTypeProp.getValue();
  }

  public JobBatchRecord setJobType(final String jobType) {
    jobTypeProp.setValue(wrapString(jobType));
    return this;
  }

  public JobBatchRecord setJobType(final DirectBuffer jobType) {
    jobTypeProp.setValue(jobType);
    return this;
  }

  public ValueArray<StringValue> variables() {
    return variablesProp;
  }

  @Override
  protected JobBatchRecord newRecord() {
    return new JobBatchRecord();
  }
}
