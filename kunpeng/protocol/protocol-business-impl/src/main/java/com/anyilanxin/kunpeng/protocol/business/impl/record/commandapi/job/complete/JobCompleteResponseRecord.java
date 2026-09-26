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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.job.complete;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.complete.JobCompleteResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;

/**
 * job 完成响应 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobCompleteResponseRecord extends UnifiedRecordValue<JobCompleteResponseRecord>
    implements JobCompleteResponseRecordValue {
  private final LongProperty jobIdProp = new LongProperty(1, "JOB_ID", -1);

  public JobCompleteResponseRecord() {
    super(1);
    declareProperty(jobIdProp);
  }

  public long getJobId() {
    return jobIdProp.getValue();
  }

  public JobCompleteResponseRecord setJobId(final long jobId) {
    jobIdProp.setValue(jobId);
    return this;
  }
}
