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
package com.anyilanxin.kunpeng.structpack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.structpack.property.LongProperty;
import com.anyilanxin.kunpeng.structpack.property.StringProperty;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 演进测试：值自界定 —— 未知 id（新版本字段/已删字段）读长度直接跳过，
 * 新旧版本双向互读互不失败；删除字段零动作（无 ghost）。
 */
@DisplayName("schema 演进（自界定值, 双向跳过）")
class SchemaEvolutionTest {

  /** v1: ID(1) + NAME(2) */
  static class RecordV1 extends UnpackedObject {
    final LongProperty id = new LongProperty(1, "ID");
    final StringProperty name = new StringProperty(2, "NAME");

    RecordV1() {
      super(2);
      declareProperty(id);
      declareProperty(name);
    }
  }

  /** v2: 新增 SCORE(3, 带默认值) */
  static class RecordV2 extends UnpackedObject {
    final LongProperty id = new LongProperty(1, "ID");
    final StringProperty name = new StringProperty(2, "NAME");
    final LongProperty score = new LongProperty(3, "SCORE", 60);

    RecordV2() {
      super(3);
      declareProperty(id);
      declareProperty(name);
      declareProperty(score);
    }
  }

  /** v2 无默认值版 */
  static class RecordV2NoDefault extends UnpackedObject {
    final LongProperty id = new LongProperty(1, "ID");
    final StringProperty name = new StringProperty(2, "NAME");
    final LongProperty score = new LongProperty(3, "SCORE");

    RecordV2NoDefault() {
      super(3);
      declareProperty(id);
      declareProperty(name);
      declareProperty(score);
    }
  }

  /** 删除版: 只剩 ID(1) —— NAME(2)/SCORE(3) 直接删行, 无任何 ghost */
  static class RecordDeleted extends UnpackedObject {
    final LongProperty id = new LongProperty(1, "ID");

    RecordDeleted() {
      super(1);
      declareProperty(id);
    }
  }

  /** 改名版: NAME→TITLE(4) */
  static class RecordRenamed extends UnpackedObject {
    final LongProperty id = new LongProperty(1, "ID");
    final StringProperty title = new StringProperty(4, "TITLE", "");

    RecordRenamed() {
      super(2);
      declareProperty(id);
      declareProperty(title);
    }
  }

  /** 新增字段不占用退休 id: ID(1) 后直接跳到 5 */
  static class RecordGapIds extends UnpackedObject {
    final LongProperty id = new LongProperty(1, "ID");
    final StringProperty name = new StringProperty(2, "NAME");
    final LongProperty a = new LongProperty(5, "A", 0);
    final LongProperty b = new LongProperty(9, "B", 0);

    RecordGapIds() {
      super(4);
      declareProperty(id);
      declareProperty(name);
      declareProperty(a);
      declareProperty(b);
    }
  }

  private byte[] serialize(final UnpackedObject record) {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[256]);
    record.write(buffer, 0);
    final byte[] bytes = new byte[record.getLength()];
    buffer.getBytes(0, bytes);
    return bytes;
  }

  @Test
  @DisplayName("新 schema 读旧数据: 缺席字段走默认值")
  void newSchemaReadsOldDataWithDefault() {
    final RecordV1 v1 = new RecordV1();
    v1.id.setValue(100);
    v1.name.setValue("鲲鹏");

    final RecordV2 v2 = new RecordV2();
    v2.wrap(new UnsafeBuffer(serialize(v1)));
    assertThat(v2.id.getValue()).isEqualTo(100);
    assertThat(v2.name.getValueAsString()).isEqualTo("鲲鹏");
    assertThat(v2.score.getValue()).isEqualTo(60);
    assertThat(v2.score.isSet()).isFalse();
  }

  @Test
  @DisplayName("新 schema 读旧数据: 缺席字段无默认值则失败")
  void newSchemaReadsOldDataWithoutDefaultFails() {
    final RecordV1 v1 = new RecordV1();
    v1.id.setValue(1);
    v1.name.setValue("x");

    final RecordV2NoDefault v2 = new RecordV2NoDefault();
    assertThatThrownBy(() -> v2.wrap(new UnsafeBuffer(serialize(v1))))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("SCORE")
        .hasMessageContaining("no valid value");
  }

  @Test
  @DisplayName("旧 schema 读新数据: 未知字段按长度跳过, 已知字段正常解析（双向互读）")
  void oldSchemaReadsNewDataSkipsUnknown() {
    final RecordV2 v2 = new RecordV2();
    v2.id.setValue(1);
    v2.name.setValue("x");
    v2.score.setValue(99);

    // 旧读方: SCORE(id 3) 未知 → 跳过; ID/NAME 正常
    final RecordV1 v1 = new RecordV1();
    v1.wrap(new UnsafeBuffer(serialize(v2)));
    assertThat(v1.id.getValue()).isEqualTo(1);
    assertThat(v1.name.getValueAsString()).isEqualTo("x");
  }

  @Test
  @DisplayName("删除字段: 旧数据被新代码直接读（跳过已删字段）, 零 ghost")
  void deletedFieldsSkippedWithoutGhost() {
    final RecordV2 v2 = new RecordV2();
    v2.id.setValue(9);
    v2.name.setValue("待删除");
    v2.score.setValue(77);
    final byte[] v2Bytes = serialize(v2);

    final RecordDeleted deleted = new RecordDeleted();
    deleted.wrap(new UnsafeBuffer(v2Bytes));
    assertThat(deleted.id.getValue()).isEqualTo(9);
    // NAME/SCORE 已删: 按值长度直接跳过, 不报错
  }

  @Test
  @DisplayName("改名即新字段: 新代码读旧数据跳过旧 key 字段")
  void renameIsNewField() {
    final RecordV1 v1 = new RecordV1();
    v1.id.setValue(5);
    v1.name.setValue("旧名字");

    final RecordRenamed renamed = new RecordRenamed();
    renamed.wrap(new UnsafeBuffer(serialize(v1)));
    assertThat(renamed.id.getValue()).isEqualTo(5);
    assertThat(renamed.title.getValueAsString()).isEmpty(); // 新字段走默认

    // 旧代码读改名版数据: TITLE(id 4) 跳过, NAME 缺失但有默认? 无默认 → 失败
    renamed.title.setValue("新标题");
    final byte[] renamedBytes = serialize(renamed);
    final RecordV1 oldReader = new RecordV1();
    assertThatThrownBy(() -> oldReader.wrap(new UnsafeBuffer(renamedBytes)))
        .isInstanceOf(StructPackException.class)
        .hasMessageContaining("NAME");
  }

  @Test
  @DisplayName("稀疏 id（含退休空洞）正常读写")
  void gapIdsWork() {
    final RecordGapIds record = new RecordGapIds();
    record.id.setValue(1);
    record.name.setValue("n");
    record.a.setValue(10);
    record.b.setValue(20);

    final byte[] bytes = serialize(record);
    final RecordGapIds back = new RecordGapIds();
    back.wrap(new UnsafeBuffer(bytes));
    assertThat(back.a.getValue()).isEqualTo(10);
    assertThat(back.b.getValue()).isEqualTo(20);
  }
}
