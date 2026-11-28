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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.delegateuser;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 代办人 id 的序列化往返与注册：任务记录新字段与设置代办人请求/响应经真实 write/wrap 编码解码双向验证（缺省值与空串都必须可编码），
 * 并在全局反序列化注册表里能按生命周期取到记录实例（broker 入口与响应解码依赖）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("代办人 id 序列化往返")
class UserTaskDelegateRecordTest {

  @Test
  void taskRecordDelegateUserIdSurvivesFullFrameRoundTrip() {
    final var source = new UserTaskRecord().setTaskId(7).setAssignee("alice");
    source.setDelegateUserId("carol");

    final var decoded = roundTrip(source, new UserTaskRecord());
    assertThat(decoded.getDelegateUserId()).isEqualTo("carol");
    assertThat(decoded.getAssignee()).isEqualTo("alice");
  }

  @Test
  void taskRecordDefaultIsEncodable() {
    // 缺省记录必须可编码可解码：属性缺省组合编码失败会拒掉整批
    final var decoded = roundTrip(new UserTaskRecord(), new UserTaskRecord());
    assertThat(decoded.getDelegateUserId()).isEmpty();
  }

  @Test
  void bufferOverloadMatchesStringOverload() {
    final var byString = new UserTaskRecord();
    byString.setDelegateUserId("dave");

    final var decoded = roundTrip(byString, new UserTaskRecord());
    assertThat(decoded.getDelegateUserIdBuffer()).isNotNull();
    assertThat(decoded.getDelegateUserId()).isEqualTo("dave");
  }

  @Test
  void requestDelegateUserIdSurvivesFullFrameRoundTrip() {
    final var source = new UserTaskDelegateUserRequestRecord();
    source.setDelegateUserId("carol");

    final var decoded = roundTrip(source, new UserTaskDelegateUserRequestRecord());
    assertThat(decoded.getDelegateUserId()).isEqualTo("carol");
  }

  @Test
  void emptyRequestDelegateUserIdIsEncodable() {
    // 空串=清空代办人，必须可编码可解码
    final var source = new UserTaskDelegateUserRequestRecord();
    source.setDelegateUserId("");

    final var decoded = roundTrip(source, new UserTaskDelegateUserRequestRecord());
    assertThat(decoded.getDelegateUserId()).isEmpty();
  }

  @Test
  void delegateUserLifecyclesResolveToRegisteredRecords() {
    // 漏注册 = 请求在 broker 入口直接异常；全局注册表重复注册会在此抛出
    final RecordValueMapper mapper = DefaultRecordValueMapper.getInstance();
    assertThat(mapper.getValue(CommandApiUserTaskValueLifeCycle.SET_DELEGATE_USER_REQUEST))
        .isInstanceOf(UserTaskDelegateUserRequestRecord.class);
    assertThat(mapper.getValue(CommandApiUserTaskValueLifeCycle.SET_DELEGATE_USER_RESPONSE))
        .isInstanceOf(UserTaskDelegateUserResponseRecord.class);
    assertThat(mapper.getValue(UserTaskLifeCycle.DELEGATE_USER)).isInstanceOf(UserTaskRecord.class);
  }

  private static <T extends UnifiedRecordValue<T>> T roundTrip(final T source, final T target) {
    final var bytes = new byte[source.getLength()];
    final var buffer = new UnsafeBuffer(bytes);
    source.write(buffer, 0);
    target.wrap(buffer, 0, buffer.capacity());
    return target;
  }
}
