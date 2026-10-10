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
package com.anyilanxin.kunpeng.sink.rdbms.write;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.sink.rdbms.mapper.Tables;
import com.anyilanxin.kunpeng.sink.rdbms.model.UserTaskDbModel;
import com.anyilanxin.kunpeng.sink.rdbms.model.VariableDbModel;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChangeBufferTest {

  @Test
  void shouldKeepOnlyLastUpdatePerKey() {
    final var buffer = new ChangeBuffer();
    buffer.offer(RowChange.update(Tables.USER_TASK, List.of(1L), task(1L, "alice")));
    buffer.offer(RowChange.update(Tables.USER_TASK, List.of(1L), task(1L, "bob")));

    assertThat(buffer.size()).isOne();
    final var change = buffer.view().iterator().next();
    assertThat(change.kind()).isEqualTo(RowChange.Kind.UPDATE);
    assertThat(((UserTaskDbModel) change.model()).getAssignee()).isEqualTo("bob");
  }

  @Test
  void shouldKeepInsertAndUpdateInOrderForSameKey() {
    final var buffer = new ChangeBuffer();
    buffer.offer(RowChange.insert(Tables.USER_TASK, List.of(1L), task(1L, "alice")));
    buffer.offer(RowChange.update(Tables.USER_TASK, List.of(1L), task(1L, "bob")));

    // insert 与 update 各占一个键位并保持先后：同事务内先插后更，数据库收敛到最终状态
    final var changes = List.copyOf(buffer.view());
    assertThat(changes).hasSize(2);
    assertThat(changes.get(0).kind()).isEqualTo(RowChange.Kind.INSERT);
    assertThat(((UserTaskDbModel) changes.get(0).model()).getAssignee()).isEqualTo("alice");
    assertThat(changes.get(1).kind()).isEqualTo(RowChange.Kind.UPDATE);
    assertThat(((UserTaskDbModel) changes.get(1).model()).getAssignee()).isEqualTo("bob");
  }

  @Test
  void shouldTrackDeletesIndependentlyFromWrites() {
    final var buffer = new ChangeBuffer();
    final var variable = new VariableDbModel();
    variable.setScopeId(7L);
    variable.setName("amount");
    buffer.offer(RowChange.insert(Tables.VARIABLE, List.of(7L, "amount"), variable));
    buffer.offer(RowChange.deleteByColumn(Tables.VARIABLE, "process_instance_id", 42L));

    assertThat(buffer.size()).isEqualTo(2);
  }

  @Test
  void shouldKeepViewUntilCleared() {
    final var buffer = new ChangeBuffer();
    buffer.offer(RowChange.insert(Tables.TIMER, List.of(5L), task(5L, null)));
    buffer.advancePosition(99L);

    final var view = List.copyOf(buffer.view());
    buffer.clear();

    assertThat(buffer.isEmpty()).isTrue();
    assertThat(view).hasSize(1);
    assertThat(buffer.lastPosition()).isEqualTo(99L);
  }

  private static UserTaskDbModel task(final long id, final String assignee) {
    final var model = new UserTaskDbModel();
    model.setTaskId(id);
    model.setAssignee(assignee);
    return model;
  }
}
