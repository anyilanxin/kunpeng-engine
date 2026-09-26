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

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.LOCAL_VARIABLES;
import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.VARIABLES;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapArray;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.complete.JobCompleteRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * job 完成请求 Record。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class JobCompleteRequestRecord extends UnifiedRecordValue<JobCompleteRequestRecord>
    implements JobCompleteRequestRecordValue {
  private final LongProperty jobIdProp = new LongProperty(1, "JOB_ID");
  private final DocumentProperty variablesProperty = new DocumentProperty(2, VARIABLES);
  private final DocumentProperty localVariablesProperty = new DocumentProperty(3, LOCAL_VARIABLES);

  public JobCompleteRequestRecord() {
    super(3);
    declareProperty(jobIdProp)
        .declareProperty(variablesProperty)
        .declareProperty(localVariablesProperty);
  }

  @Override
  public JobCompleteRequestRecordValue setJobId(final long jobId) {
    jobIdProp.setValue(jobId);
    return this;
  }

  public long getJobId() {
    return jobIdProp.getValue();
  }

  @Override
  public JobCompleteRequestRecordValue setVariables(final Map<String, Object> variables) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(variables)));
    return this;
  }

  @Override
  public JobCompleteRequestRecordValue setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  @Override
  public JobCompleteRequestRecordValue setLocalVariables(final Map<String, Object> localVariables) {
    localVariablesProperty.setValue(wrapArray(convertToMsgPack(localVariables)));
    return this;
  }

  @Override
  public JobCompleteRequestRecordValue setLocalVariables(final DirectBuffer localVariables) {
    localVariablesProperty.setValue(localVariables);
    return this;
  }

  public Map<String, Object> getLocalVariables() {
    return convertToMap(localVariablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getLocalVariablesBuffer() {
    return localVariablesProperty.getValue();
  }
}
