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
package com.anyilanxin.kunpeng.sink.api.context;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;

/**
 * 对记录静态形状的谓词，由引擎在记录交给 Sink 之前应用。
 *
 * <p>用它把不需要的记录挡在 {@code RecordSink#sink} 之外——被拒绝的记录在 Sink 侧零开销，并被自动确认。
 * 匹配规则的修改只对之后的记录生效；已确认的记录不会重新投递。
 */
public interface RecordMatcher {

  /**
   * @param recordType 记录的结构类型（命令、事件、拒绝……）
   * @return 该类型的记录是否允许投递
   */
  boolean acceptsRecordType(RecordType recordType);

  /**
   * @param valueType 记录负载的种类（流程实例、任务、事件……）
   * @return 携带该负载种类的记录是否允许投递
   */
  boolean acceptsValueType(ValueType valueType);

  /**
   * @param intent 记录的意图，例如活动事件的 {@code ACTIVATED}
   * @return 该意图的记录是否允许投递；默认全部接受
   */
  default boolean acceptsIntent(final ValueLifeCycle intent) {
    return true;
  }
}
