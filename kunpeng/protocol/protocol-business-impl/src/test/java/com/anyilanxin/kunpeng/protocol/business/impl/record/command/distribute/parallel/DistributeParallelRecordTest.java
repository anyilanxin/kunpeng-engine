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
package com.anyilanxin.kunpeng.protocol.business.impl.record.command.distribute.parallel;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.copyInto;
import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.historycleanup.HistoryCleanupRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 嵌套 record 属性的透传语义（裸 UnifiedRecordValue 载体）：set 即保留、整帧序列化往返后仍可按 valueType 还原为具体记录。
 *
 * <p>钉住的线上故障形态：setDistributeRecord 后载体按声明路径解析静默丢弃全部字段，嵌套属性始终为空。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("DistributeParallelRecord 嵌套 record 透传")
class DistributeParallelRecordTest {

  private static DistributeParallelRecord record() {
    final var record = new DistributeParallelRecord();
    record.setDistributeId(1);
    record.setDistributeRecordId(99);
    record.setDistributeRecordValueType(ValueType.HISTORY_CLEANUP);
    record.setDistributeRecordLifeCycle(HistoryCleanupLifeCycle.TRIGGERED);
    record.setDistributeRecord(nested());
    return record;
  }

  private static HistoryCleanupRecord nested() {
    final var nested = new HistoryCleanupRecord();
    nested.setProcessDefinitionId(7);
    nested.setProcessDefinitionKey("order-process");
    return nested;
  }

  @Test
  void nestedRecordSurvivesSetAndMapperResolve() {
    final var resolved = record().getDistributeRecord();

    assertThat(resolved).isInstanceOf(HistoryCleanupRecord.class);
    final var cleanup = (HistoryCleanupRecord) resolved;
    assertThat(cleanup.getProcessDefinitionId()).isEqualTo(7);
    assertThat(cleanup.getProcessDefinitionKey()).isEqualTo("order-process");
  }

  @Test
  void nestedRecordSurvivesFullFrameRoundTrip() {
    final var source = record();
    final var bytes = new byte[source.getLength()];
    source.write(new UnsafeBuffer(bytes), 0);

    final var decoded = new DistributeParallelRecord();
    decoded.wrap(new UnsafeBuffer(bytes));

    final var resolved = decoded.getDistributeRecord();
    assertThat(resolved).isInstanceOf(HistoryCleanupRecord.class);
    assertThat(((HistoryCleanupRecord) resolved).getProcessDefinitionKey()).isEqualTo("order-process");
  }

  @Test
  void copyIntoKeepsSameTypeSemantics() {
    final var source = record();
    final var target = new DistributeParallelRecord();

    copyInto(source, target);

    final var resolved = target.getDistributeRecord();
    assertThat(resolved).isInstanceOf(HistoryCleanupRecord.class);
    assertThat(((HistoryCleanupRecord) resolved).getProcessDefinitionId()).isEqualTo(7);
  }
}
