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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.fail;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.fail.JobFailRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.IntegerProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.DirectBuffer;

/**
 * job 失败请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobFailRequestRecord extends UnifiedRecordValue<JobFailRequestRecord>
    implements JobFailRequestRecordValue {
  private final LongProperty jobIdProp = new LongProperty(1, "JOB_ID");
  private final IntegerProperty retriesProp = new IntegerProperty(2, "RETRIES", -1);
  private final IntegerProperty retryBackOffProp = new IntegerProperty(3, "RETRY_BACK_OFF", 0);
  private final StringProperty deniedReasonProp = new StringProperty(4, "DENIED_REASON", "");

  public JobFailRequestRecord() {
    super(4);
    declareProperty(jobIdProp)
        .declareProperty(retriesProp)
        .declareProperty(retryBackOffProp)
        .declareProperty(deniedReasonProp);
  }

  public long getJobId() {
    return jobIdProp.getValue();
  }

  @Override
  public JobFailRequestRecordValue setJobId(final long jobId) {
    jobIdProp.setValue(jobId);
    return this;
  }

  public int getRetries() {
    return retriesProp.getValue();
  }

  @Override
  public JobFailRequestRecordValue setRetries(final int retries) {
    retriesProp.setValue(retries);
    return this;
  }

  public int getRetryBackOff() {
    return retryBackOffProp.getValue();
  }

  @Override
  public JobFailRequestRecordValue setRetryBackOff(final int retryBackOff) {
    retryBackOffProp.setValue(retryBackOff);
    return this;
  }

  public String getDeniedReason() {
    return bufferAsString(deniedReasonProp.getValue());
  }

  public DirectBuffer getDeniedReasonBuffer() {
    return deniedReasonProp.getValue();
  }

  @Override
  public JobFailRequestRecordValue setDeniedReason(final String deniedReason) {
    deniedReasonProp.setValue(wrapString(deniedReason));
    return this;
  }

  @Override
  public JobFailRequestRecordValue setDeniedReason(final DirectBuffer deniedReason) {
    deniedReasonProp.setValue(deniedReason);
    return this;
  }
}
