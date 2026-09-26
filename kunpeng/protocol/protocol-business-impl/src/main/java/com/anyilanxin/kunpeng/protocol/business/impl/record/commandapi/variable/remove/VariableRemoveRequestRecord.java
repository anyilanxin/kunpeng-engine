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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.remove;

import static com.anyilanxin.kunpeng.protocol.business.impl.BusinessRecordConstant.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.variable.remove.VariableRemoveRequestRecordValue;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.property.ArrayProperty;
import com.anyilanxin.kunpeng.structpack.property.BooleanProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.value.StringValue;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
@AutoDeclareProperties
public class VariableRemoveRequestRecord extends UnifiedRecordValue<VariableRemoveRequestRecord>
    implements VariableRemoveRequestRecordValue {
  private final LongProperty processInstanceIdProp = new LongProperty(3, PROCESS_INSTANCE_ID, -1);
  private final LongProperty activityInstanceIdProp = new LongProperty(4, ACTIVITY_INSTANCE_ID, -1);
  private final LongProperty taskIdProp = new LongProperty(5, TASK_ID, -1);
  private final BooleanProperty removeAllProp = new BooleanProperty(1, "REMOVE_ALL", false);
  private final ArrayProperty<StringValue> removeVariableProp =
      new ArrayProperty<>(2, "REMOVE_VARIABLE", StringValue::new);

  public VariableRemoveRequestRecord() {
    super(5);
    declareProperty(processInstanceIdProp)
        .declareProperty(activityInstanceIdProp)
        .declareProperty(taskIdProp)
        .declareProperty(removeAllProp)
        .declareProperty(removeVariableProp);
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceIdProp.getValue();
  }

  public VariableRemoveRequestRecord setProcessInstanceId(final long processInstanceId) {
    processInstanceIdProp.setValue(processInstanceId);
    return this;
  }

  @Override
  public long getActivityInstanceId() {
    return activityInstanceIdProp.getValue();
  }

  public VariableRemoveRequestRecord setActivityInstanceId(final long activityInstanceId) {
    activityInstanceIdProp.setValue(activityInstanceId);
    return this;
  }

  @Override
  public long getTaskId() {
    return taskIdProp.getValue();
  }

  public VariableRemoveRequestRecord setTaskId(final long taskId) {
    taskIdProp.setValue(taskId);
    return this;
  }

  @Override
  public boolean isRemoveAll() {
    return removeAllProp.getValue();
  }

  public VariableRemoveRequestRecord setRemoveAll(final boolean removeAll) {
    removeAllProp.setValue(removeAll);
    removeVariableProp.reset();
    return this;
  }

  /** 待移除变量名列表(原始数组访问,引擎处理器使用) */
  public ArrayProperty<StringValue> removeVariable() {
    return removeVariableProp;
  }

  @Override
  public List<String> getRemoveVariable() {
    return StreamSupport.stream(removeVariableProp.spliterator(), false)
        .map(v -> bufferAsString(v.getValue()))
        .collect(Collectors.toList());
  }

  public VariableRemoveRequestRecord setRemoveVariable(final List<String> removeVariable) {
    removeVariableProp.reset();
    if (removeVariable == null || removeVariable.isEmpty()) {
      return this;
    }
    for (final String name : removeVariable) {
      removeVariableProp.add().wrap(wrapString(name));
    }
    return this;
  }

  @Override
  protected VariableRemoveRequestRecord newRecord() {
    return new VariableRemoveRequestRecord();
  }
}
