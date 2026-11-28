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
package com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.delegateuser.ProcessInstanceDelegateUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.delegateuser.ProcessInstanceDelegateUserResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.startuser.ProcessInstanceStartUserRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.startuser.ProcessInstanceStartUserResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 流程实例发起人与代办人的序列化往返与注册：实例记录新字段与两条命令的请求/响应经真实 write/wrap 编码解码双向验证（缺省值与空串都必须可编码），
 * 并在全局反序列化注册表里能按生命周期取到记录实例（broker 入口与响应解码依赖）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("流程实例发起人与代办人序列化往返")
class ProcessInstanceDelegateRecordTest {

  @Test
  void instanceRecordDelegateUserIdSurvivesFullFrameRoundTrip() {
    final var source = instance();
    source.setDelegateUserId("carol");

    final var decoded = roundTrip(source, new ProcessInstanceRecord());
    assertThat(decoded.getDelegateUserId()).isEqualTo("carol");
    assertThat(decoded.getStartUserId()).isEqualTo("alice");
  }

  @Test
  void instanceRecordWithoutDelegateIsEncodable() {
    // 未设置代办人（缺省空串）的实例记录必须可编码可解码：属性缺省组合编码失败会拒掉整批
    final var decoded = roundTrip(instance(), new ProcessInstanceRecord());
    assertThat(decoded.getDelegateUserId()).isEmpty();
  }

  @Test
  void bufferOverloadMatchesStringOverload() {
    final var byString = instance();
    byString.setDelegateUserId("dave");

    final var decoded = roundTrip(byString, new ProcessInstanceRecord());
    assertThat(decoded.getDelegateUserIdBuffer()).isNotNull();
    assertThat(decoded.getDelegateUserId()).isEqualTo("dave");
  }

  @Test
  void requestStartUserIdSurvivesFullFrameRoundTrip() {
    final var source = new ProcessInstanceStartUserRequestRecord();
    source.setStartUserId("bob");

    final var decoded = roundTrip(source, new ProcessInstanceStartUserRequestRecord());
    assertThat(decoded.getStartUserId()).isEqualTo("bob");
  }

  @Test
  void requestDelegateUserIdSurvivesFullFrameRoundTrip() {
    final var source = new ProcessInstanceDelegateUserRequestRecord();
    source.setDelegateUserId("carol");

    final var decoded = roundTrip(source, new ProcessInstanceDelegateUserRequestRecord());
    assertThat(decoded.getDelegateUserId()).isEqualTo("carol");
  }

  @Test
  void emptyRequestsAreEncodable() {
    // 空串=清空代办人，必须可编码可解码
    final var delegate = roundTrip(
        new ProcessInstanceDelegateUserRequestRecord(),
        new ProcessInstanceDelegateUserRequestRecord());
    assertThat(delegate.getDelegateUserId()).isEmpty();
    final var startUser =
        roundTrip(
            new ProcessInstanceStartUserRequestRecord(),
            new ProcessInstanceStartUserRequestRecord());
    assertThat(startUser.getStartUserId()).isEmpty();
  }

  @Test
  void lifecyclesResolveToRegisteredRecords() {
    // 漏注册 = 请求在 broker 入口直接异常；全局注册表重复注册会在此抛出
    final RecordValueMapper mapper = DefaultRecordValueMapper.getInstance();
    assertThat(mapper.getValue(CommandApiProcessInstanceValueLifeCycle.SET_START_USER_REQUEST))
        .isInstanceOf(ProcessInstanceStartUserRequestRecord.class);
    assertThat(mapper.getValue(CommandApiProcessInstanceValueLifeCycle.SET_START_USER_RESPONSE))
        .isInstanceOf(ProcessInstanceStartUserResponseRecord.class);
    assertThat(mapper.getValue(CommandApiProcessInstanceValueLifeCycle.SET_DELEGATE_USER_REQUEST))
        .isInstanceOf(ProcessInstanceDelegateUserRequestRecord.class);
    assertThat(mapper.getValue(CommandApiProcessInstanceValueLifeCycle.SET_DELEGATE_USER_RESPONSE))
        .isInstanceOf(ProcessInstanceDelegateUserResponseRecord.class);
    assertThat(mapper.getValue(ProcessInstanceLifeCycle.START_USER))
        .isInstanceOf(ProcessInstanceRecord.class);
    assertThat(mapper.getValue(ProcessInstanceLifeCycle.DELEGATE_USER))
        .isInstanceOf(ProcessInstanceRecord.class);
  }

  /** 与引擎创建流程实例时的记录形态一致（未设置的属性保持缺省） */
  private static ProcessInstanceRecord instance() {
    return new ProcessInstanceRecord()
        .setProcessInstanceId(7)
        .setRootProcessInstanceId(7)
        .setLifeCycle(ProcessInstanceLifeCycle.ACTIVATED)
        .setBusinessKey("bk-1")
        .setStartUserId("alice")
        .setProcessDefinitionId(5)
        .setProcessDefinitionKey("p1");
  }

  private static <T extends UnifiedRecordValue<T>> T roundTrip(final T source, final T target) {
    final var bytes = new byte[source.getLength()];
    final var buffer = new UnsafeBuffer(bytes);
    source.write(buffer, 0);
    target.wrap(buffer, 0, buffer.capacity());
    return target;
  }
}
