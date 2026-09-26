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

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.activate.JobBatchActivateResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.activate.JobInfoRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.BooleanProperty;
import com.anyilanxin.kunpeng.structpack.value.LongValue;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * job 批量激活响应 Record：激活的 job 列表。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobBatchActivateResponseRecord
    extends UnifiedRecordValue<JobBatchActivateResponseRecord>
    implements JobBatchActivateResponseRecordValue {
  private final ArrayProperty<JobInfoRecord> jobsProp =
      new ArrayProperty<>(1, "JOB_KEYS_KEY", JobInfoRecord::new);
  private final ArrayProperty<LongValue> jobKeysProp =
      new ArrayProperty<>(2, "JOB_KEYS", LongValue::new);
  private final BooleanProperty truncatedProp = new BooleanProperty(3, "TRUNCATED", false);

  public JobBatchActivateResponseRecord() {
    super(3);
    // formatting:off
    declareProperty(jobsProp)
        .declareProperty(jobKeysProp)
        .declareProperty(truncatedProp);
    // formatting:on
  }

  public ValueArray<JobInfoRecord> jobs() {
    return jobsProp;
  }

  public ValueArray<LongValue> keys() {
    return jobKeysProp;
  }

  @Override
  public List<JobInfoRecordValue> getJobs() {
    return StreamSupport.stream(jobsProp.spliterator(), false)
        .map(
            jobRecord -> {
              final byte[] bytes = new byte[jobRecord.getLength()];
              final UnsafeBuffer copyRecord = new UnsafeBuffer(bytes);
              final JobInfoRecord copiedRecord = new JobInfoRecord();

              jobRecord.write(copyRecord, 0);
              copiedRecord.wrap(copyRecord);
              return copiedRecord;
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<Long> getJobKeys() {
    return StreamSupport.stream(jobKeysProp.spliterator(), false)
        .map(LongValue::getValue)
        .collect(Collectors.toList());
  }

  public boolean getTruncated() {
    return truncatedProp.getValue();
  }

  public JobBatchActivateResponseRecord setTruncated(final boolean truncated) {
    truncatedProp.setValue(truncated);
    return this;
  }
}
