/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.update;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapArray;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMap;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToMsgPack;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.variable.update.VariableUpdateRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.DocumentProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class VariableUpdateRequestRecord extends UnifiedRecordValue<VariableUpdateRequestRecord>
    implements VariableUpdateRequestRecordValue {
  // structpack-ids[VariableUpdateRequestRecord]: 1,2,3,4

  private final LongProperty processInstanceIdProp = new LongProperty(1, PROCESS_INSTANCE_ID, -1);
  private final LongProperty activityInstanceIdProp = new LongProperty(2, ACTIVITY_INSTANCE_ID, -1);
  private final LongProperty taskIdProp = new LongProperty(3, TASK_ID, -1);
  private final DocumentProperty variablesProperty = new DocumentProperty(4, VARIABLES);

  public VariableUpdateRequestRecord() {
    super(4);
    declareProperty(processInstanceIdProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(taskIdProp)
        .declareProperty(variablesProperty);
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public VariableUpdateRequestRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public VariableUpdateRequestRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  @Override
  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public VariableUpdateRequestRecord setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  @Override
  public Map<String, Object> getVariables() {
    return convertToMap(variablesProperty.getValue());
  }

  @JsonIgnore
  public DirectBuffer getVariablesBuffer() {
    return variablesProperty.getValue();
  }

  public VariableUpdateRequestRecord setVariables(final DirectBuffer variables) {
    variablesProperty.setValue(variables);
    return this;
  }

  public VariableUpdateRequestRecord setVariables(final Map<String, Object> variables) {
    variablesProperty.setValue(wrapArray(convertToMsgPack(variables)));
    return this;
  }

  @Override
  protected VariableUpdateRequestRecord newRecord() {
    return new VariableUpdateRequestRecord();
  }
}
