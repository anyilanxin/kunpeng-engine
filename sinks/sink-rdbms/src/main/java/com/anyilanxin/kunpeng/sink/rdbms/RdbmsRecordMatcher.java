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
package com.anyilanxin.kunpeng.sink.rdbms;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.sink.api.context.RecordMatcher;
import java.util.Map;

/**
 * 只放行「事件 + 本 sink 关心的负载类型」的匹配器。被拒绝的记录在引擎侧直接确认，零投递开销。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class RdbmsRecordMatcher implements RecordMatcher {

  private static final Map<ValueType, Boolean> HANDLED =
      Map.ofEntries(
          Map.entry(ValueType.PROCESS_DEFINITION, true),
          Map.entry(ValueType.PROCESS_INSTANCE, true),
          Map.entry(ValueType.ACTIVITY, true),
          Map.entry(ValueType.VARIABLE, true),
          Map.entry(ValueType.USER_TASK, true),
          Map.entry(ValueType.JOB, true),
          Map.entry(ValueType.INCIDENT, true),
          Map.entry(ValueType.TIMER, true),
          Map.entry(ValueType.MESSAGE_SUBSCRIPTION, true),
          Map.entry(ValueType.SIGNAL_SUBSCRIPTION, true),
          Map.entry(ValueType.HISTORY_CLEANUP, true));

  @Override
  public boolean acceptsRecordType(final RecordType recordType) {
    return recordType == RecordType.EVENT;
  }

  @Override
  public boolean acceptsValueType(final ValueType valueType) {
    return HANDLED.getOrDefault(valueType, false);
  }
}
