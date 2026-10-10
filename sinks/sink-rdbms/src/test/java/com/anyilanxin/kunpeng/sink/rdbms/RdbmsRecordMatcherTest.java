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

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import org.junit.jupiter.api.Test;

class RdbmsRecordMatcherTest {

  private final RdbmsRecordMatcher matcher = new RdbmsRecordMatcher();

  @Test
  void shouldAcceptOnlyEvents() {
    assertThat(matcher.acceptsRecordType(RecordType.EVENT)).isTrue();
    assertThat(matcher.acceptsRecordType(RecordType.COMMAND)).isFalse();
    assertThat(matcher.acceptsRecordType(RecordType.COMMAND_REJECTION)).isFalse();
  }

  @Test
  void shouldAcceptOnlyHandledValueTypes() {
    assertThat(matcher.acceptsValueType(ValueType.PROCESS_INSTANCE)).isTrue();
    assertThat(matcher.acceptsValueType(ValueType.VARIABLE)).isTrue();
    assertThat(matcher.acceptsValueType(ValueType.DEPLOYMENT)).isFalse();
  }

  @Test
  void shouldIgnoreUnknownLifecycle() {
    assertThat(matcher.acceptsIntent(ProcessInstanceLifeCycle.LISTENER_CREATE)).isTrue();
  }
}
