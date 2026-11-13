/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.assignee;

import static org.assertj.core.api.Assertions.assertThat;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 改派请求 Record 序列化往返：新审批人字段（ASSIGNEE key1）经真实 write/wrap 编码解码双向验证。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("UserTaskAssigneeRequestRecord 序列化往返")
class UserTaskAssigneeRequestRecordTest {

  @Test
  void assigneeSurvivesFullFrameRoundTrip() {
    final var source = new UserTaskAssigneeRequestRecord();
    source.setAssignee("new-approver-42");

    final var bytes = new byte[source.getLength()];
    source.write(new UnsafeBuffer(bytes), 0);

    final var decoded = new UserTaskAssigneeRequestRecord();
    decoded.wrap(new UnsafeBuffer(bytes));
    assertThat(decoded.getAssignee()).isEqualTo("new-approver-42");
  }

  @Test
  void bufferOverloadMatchesStringOverload() {
    final var byString = new UserTaskAssigneeRequestRecord();
    byString.setAssignee("zhang-san");

    final var bytes = new byte[byString.getLength()];
    byString.write(new UnsafeBuffer(bytes), 0);

    final var decoded = new UserTaskAssigneeRequestRecord();
    decoded.wrap(new UnsafeBuffer(bytes));
    assertThat(decoded.getAssigneeBuffer()).isNotNull();
    assertThat(decoded.getAssignee()).isEqualTo("zhang-san");
  }
}
