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
package com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchType;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 管理面调度查询响应序列化往返：节点 source 注册表（NODE_SOURCE_META key3）经真实 write/wrap 编码解码双向验证。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("AdminDispatchQueryResponseRecord 序列化往返")
class AdminDispatchQueryResponseRecordTest {

  @Test
  void nodeSourceMetaSurvivesFullFrameRoundTrip() {
    final var source = new AdminDispatchQueryResponseRecord();
    source.getDispatchPlan().setDispatchPlanType(AdminDispatchType.CHANGE_REPLICATION);
    final NodeSourceMetaRecord meta = source.getNodeSourceMeta();
    meta.setVersion(3);
    meta.setMaxNodeSourceId(7);
    meta.sources().add().setMemberId("broker@node01").setSourceId(1);
    meta.sources().add().setMemberId("broker@node02").setSourceId(2);

    final var bytes = new byte[source.getLength()];
    source.write(new UnsafeBuffer(bytes), 0);

    final var decoded = new AdminDispatchQueryResponseRecord();
    decoded.wrap(new UnsafeBuffer(bytes));
    assertThat(decoded.getNodeSourceMeta().getVersion()).isEqualTo(3);
    assertThat(decoded.getNodeSourceMeta().getMaxNodeSourceId()).isEqualTo(7);
    assertThat(decoded.getNodeSourceMeta().getSources())
        .extracting("memberId", "sourceId")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("broker@node01", 1),
            org.assertj.core.groups.Tuple.tuple("broker@node02", 2));
  }

  @Test
  void emptyNodeSourceMetaEncodesAndDecodes() {
    final var source = new AdminDispatchQueryResponseRecord();
    source.getDispatchPlan().setDispatchPlanType(AdminDispatchType.CHANGE_REPLICATION);

    final var bytes = new byte[source.getLength()];
    source.write(new UnsafeBuffer(bytes), 0);

    final var decoded = new AdminDispatchQueryResponseRecord();
    decoded.wrap(new UnsafeBuffer(bytes));
    assertThat(decoded.getNodeSourceMeta()).isNotNull();
    assertThat(decoded.getNodeSourceMeta().getSources()).isEmpty();
  }
}
