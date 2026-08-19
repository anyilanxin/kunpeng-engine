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
package com.anyilanxin.kunpeng.structpack.value;

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;
import com.anyilanxin.kunpeng.structpack.StructPackException;
import com.anyilanxin.kunpeng.structpack.property.BaseProperty;
import com.anyilanxin.kunpeng.structpack.property.UndeclaredProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * 对象值（嵌套对象容器），也是所有 Record/Entity 的基类载体。
 *
 * <p>wire 格式（structpack v1，key-id 帧布局，protobuf 式）：
 *
 * <pre>
 * Record := MAGIC(0x4B 0x50) VER(0x01)
 *           FIELD_COUNT varint
 *           [FIELD_ID varint 严格升序 ...]        // id 定义于 Property 构造首参, ~1B/字段
 *           [VALUE_LEN varint VALUE]*             // 值自界定: 已知按类型解析, 未知按长度跳过
 *           UNDECLARED_COUNT varint
 *           { KEY_LEN varint KEY bytes 带 tag VALUE }*
 * </pre>
 *
 * <p>字段身份规则（id 定义在 Property 构造首参，由构建任务自动分配冻结）：
 *
 * <ul>
 *   <li>{@code new LongProperty(1, "ORDER_ID", -1)} —— id/key/类型/默认值同在一行； {@code
 *       declareProperty(prop)} 链保持干净（与 msgpack 模块同形态）
 *   <li>同一实体内 id 与 key 均严禁重复（构造期校验）；id 由构建任务分配，<b>永不复用</b>
 *   <li>任意位置新增字段 = 任务自动分配最小未用 id，与声明顺序无关
 *   <li><b>删除字段 = 直接删那一行，零额外动作</b>——历史 id 由类内标记注释自动退休； 旧数据中的已删字段被读方按值长度直接跳过，新旧版本双向互读互不失败
 *   <li>key 改名 = 新字段（同删除+新增）；字段类型不可变更；id ≤ 127（1 字节保证，构建守卫）
 * </ul>
 *
 * <p>读入按 id 数组直读——零字符串比较；id 严格升序（规范写出序），违例视为数据损坏。 每个值带长度前缀（自界定）：<b>未知 id（新版本字段或已删字段）读长度后直接跳过</b>，
 * 新旧版本双向互读互不失败，无需任何 ghost/载体声明。<b>已知 id 永不错读</b>—— id 永不复用保证同 id 必为同字段同类型。
 */
public class ObjectValue extends BaseValue {

  public static final int MAGIC_1 = 0x4B;
  public static final int MAGIC_2 = 0x50;
  public static final int WIRE_VERSION = 0x01;

  private static final int[] NO_IDS = new int[0];

  private final List<BaseProperty<? extends BaseValue>> declaredProperties;
  private final List<UndeclaredProperty> undeclaredProperties = new ArrayList<>();
  private final List<UndeclaredProperty> recycledProperties = new ArrayList<>();

  private int[] ids = NO_IDS; // 与 declaredProperties 同下标, 声明期由 prop.getId() 填充

  @SuppressWarnings("unchecked")
  private BaseProperty<? extends BaseValue>[] declaredArray =
      new BaseProperty[0]; // 声明槽位数组缓存（布局期构建, 写路径零分配）

  private int[] sortedById = NO_IDS; // 声明槽位索引, 按 id 升序（规范写出序）
  private int[] slotById = NO_IDS; // id → 槽位+1, 0 = 未声明
  private int[] readIds = NO_IDS; // 读入 id 块的复用暂存
  private boolean layoutFrozen;

  public ObjectValue(final int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
    }
    declaredProperties = new ArrayList<>(initialCapacity);
  }

  public ObjectValue() {
    declaredProperties = new ArrayList<>();
  }

  /**
   * 声明一个字段槽位。id 从 {@link BaseProperty#getId()} 读取（构造首参定义）。
   *
   * <p>新增字段无需任何仪式——构建任务自动向初始化器分配 id。
   */
  public ObjectValue declareProperty(final BaseProperty<? extends BaseValue> prop) {
    final int id = prop.getId();
    if (id <= 0) {
      throw new StructPackException(
          prop.getKey(), "字段 id 非法（必须 ≥1）—— id 为强制属性, 请手写 new XxxProperty(id, key, ...)");
    }
    for (int i = 0; i < declaredProperties.size(); i++) {
      final BaseProperty<? extends BaseValue> existing = declaredProperties.get(i);
      if (ids[i] == id) {
        throw new StructPackException(
            prop.getKey(), "字段 id 重复: " + id + " 已被 '" + existing.getKey() + "' 占用（id 严禁重复）");
      }
      if (existing.getKey().equals(prop.getKey())) {
        throw new StructPackException(
            prop.getKey(), "同一实体内 key 严禁重复, 与字段 '" + existing.getKey() + "' 冲突");
      }
    }
    final int count = declaredProperties.size();
    declaredProperties.add(prop);
    ids = grow(ids, count);
    ids[count] = id;
    layoutFrozen = false;
    return this;
  }

  private static int[] grow(final int[] array, final int requiredIndex) {
    if (requiredIndex < array.length) {
      return array;
    }
    final int newLength = Math.max(requiredIndex + 1, Math.max(4, array.length * 2));
    final int[] grown = new int[newLength];
    System.arraycopy(array, 0, grown, 0, array.length);
    return grown;
  }

  public boolean isEmpty() {
    return declaredProperties.isEmpty() && undeclaredProperties.isEmpty();
  }

  public int undeclaredCount() {
    return undeclaredProperties.size();
  }

  public UndeclaredProperty getUndeclaredProperty(final int index) {
    return undeclaredProperties.get(index);
  }

  /** 复用复位：declared 槽位清标志，未声明条目归还池 */
  @Override
  public void reset() {
    for (final BaseProperty<? extends BaseValue> prop : declaredProperties) {
      prop.reset();
    }
    for (int i = undeclaredProperties.size() - 1; i >= 0; --i) {
      final UndeclaredProperty undeclaredProperty = undeclaredProperties.remove(i);
      undeclaredProperty.reset();
      recycledProperties.add(undeclaredProperty);
    }
  }

  private UndeclaredProperty newUndeclaredProperty() {
    final int recycledSize = recycledProperties.size();
    return recycledSize > 0
        ? recycledProperties.remove(recycledSize - 1)
        : new UndeclaredProperty();
  }

  // ===== 布局: id 升序规范写出序 =====

  @SuppressWarnings("unchecked")
  private void ensureLayout() {
    if (layoutFrozen) {
      return;
    }
    final int count = declaredProperties.size();
    if (count == 0) {
      sortedById = NO_IDS;
      slotById = NO_IDS;
      layoutFrozen = true;
      return;
    }
    final BaseProperty<? extends BaseValue>[] array = new BaseProperty[count];
    for (int i = 0; i < count; i++) {
      array[i] = declaredProperties.get(i);
    }
    declaredArray = array;
    sortedById = new int[count];
    for (int i = 0; i < count; i++) {
      sortedById[i] = i;
    }
    // 按 id 升序的槽位序（插入排序, n 小且近乎有序）
    for (int i = 1; i < count; i++) {
      final int slot = sortedById[i];
      final int id = ids[slot];
      int j = i - 1;
      while (j >= 0 && ids[sortedById[j]] > id) {
        sortedById[j + 1] = sortedById[j];
        j--;
      }
      sortedById[j + 1] = slot;
    }

    int maxId = 0;
    for (final int id : ids) {
      maxId = Math.max(maxId, id);
    }
    slotById = new int[maxId + 1];
    for (int i = 0; i < count; i++) {
      final int slot = slotById[ids[i]];
      if (slot != 0) {
        throw new StructPackException("字段 id " + ids[i] + " 被多个槽位占用（id 严禁重复）");
      }
      slotById[ids[i]] = i + 1;
    }
    layoutFrozen = true;
  }

  // ===== 读 =====

  @Override
  public void read(final PackerReader reader) {
    final int magic1 = reader.readByte();
    final int magic2 = reader.readByte();
    if (magic1 != MAGIC_1 || magic2 != MAGIC_2) {
      throw new StructPackException(
          "非法 structpack 数据: magic=0x" + Integer.toHexString(magic1) + Integer.toHexString(magic2));
    }
    final int version = reader.readByte();
    if (version != WIRE_VERSION) {
      throw new StructPackException("不支持的 structpack 版本: " + version);
    }
    ensureLayout();

    final int fieldCount = (int) reader.readVarInt();
    if (readIds.length < fieldCount) {
      readIds = new int[Math.max(fieldCount, readIds.length * 2)];
    }
    // id 块（严格升序 = 规范写出序, 违例视为损坏）
    int previousId = 0;
    for (int i = 0; i < fieldCount; i++) {
      final int id = (int) reader.readVarInt();
      if (id <= previousId) {
        throw new StructPackException(
            getClass().getSimpleName()
                + ": 字段 id 必须严格升序, 实际 "
                + previousId
                + " 后出现 "
                + id
                + " —— 数据损坏或非规范写出");
      }
      readIds[i] = id;
      previousId = id;
    }
    // 值块（与 id 一一对应, 每值带长度前缀）
    // 未知 id（新版本字段/已删字段）: 读长度直接跳过 —— 新旧版本双向互读互不失败
    for (int i = 0; i < fieldCount; i++) {
      final int id = readIds[i];
      final int slot = id < slotById.length ? slotById[id] : 0;
      final int length = (int) reader.readVarInt();
      if (slot > 0) {
        declaredProperties.get(slot - 1).read(reader);
      } else {
        reader.skipBytes(length);
      }
    }

    final int undeclaredCount = (int) reader.readVarInt();
    for (int i = 0; i < undeclaredCount; i++) {
      final UndeclaredProperty prop = newUndeclaredProperty();
      prop.read(reader);
      undeclaredProperties.add(prop);
    }

    // 校验: 无默认值且缺失的字段直接失败
    for (final BaseProperty<? extends BaseValue> prop : declaredProperties) {
      if (!prop.hasValue()) {
        throw new StructPackException(
            String.format("Property '%s' has no valid value", prop.getKey()));
      }
    }
  }

  // ===== 写 =====

  @Override
  public int getEncodedLength() {
    ensureWritable();
    ensureLayout();
    final int count = declaredProperties.size();
    // 单循环 + 布局期数组缓存（与 write 路径同款优化, 消除 List.get 虚调用）
    final int[] sorted = sortedById;
    final int[] localIds = ids;
    final BaseProperty<? extends BaseValue>[] props = declaredArray;
    int length = 3 + PackerWriter.varIntLength(count);
    for (int i = 0; i < count; i++) {
      length += PackerWriter.varIntLength(localIds[sorted[i]]);
      final int valueLength = props[sorted[i]].valueEncodedLength();
      length += PackerWriter.varIntLength(valueLength) + valueLength;
    }
    length += PackerWriter.varIntLength(undeclaredProperties.size());
    for (final UndeclaredProperty prop : undeclaredProperties) {
      length += prop.getEncodedLength();
    }
    return length;
  }

  @Override
  public void write(final PackerWriter writer) {
    ensureWritable();
    ensureLayout();
    final int count = declaredProperties.size();
    // 局部变量提升（方案 C: 布局期缓存的数组消除循环内 List.get 虚调用与装箱检查）
    final BaseProperty<? extends BaseValue>[] props = declaredArray;
    final int[] sorted = sortedById;
    final int[] localIds = ids;

    writer.writeByte(MAGIC_1).writeByte(MAGIC_2).writeByte(WIRE_VERSION);
    writer.writeVarInt(count);
    // 规范写出序: id 严格升序 —— 同一条记录在任何节点、任何声明序的类上字节级一致
    for (int i = 0; i < count; i++) {
      writer.writeVarInt(localIds[sorted[i]]);
    }
    // 值自界定: 预留 1 字节 + 写值 + 回填长度（方案 C: advance 跳过占位写入,
    // 每字段省 1 次内存写——反正必回填, 先写 0 是纯浪费）
    //  - 值 < 128B（绝大多数字段）: 单字节回填
    //  - 值 >= 128B（长串/document/大数组）: 值右移扩展前缀后回填 varint
    for (int i = 0; i < count; i++) {
      final BaseProperty<? extends BaseValue> prop = props[sorted[i]];
      final int prefixOffset = writer.getOffset();
      writer.advance(1); // 预留长度前缀位置（不写占位 0, 回填时直接覆盖）
      prop.writeValue(writer);
      final int valueLength = writer.getOffset() - prefixOffset - 1;
      if (valueLength < 128) {
        writer.patchByte(prefixOffset, valueLength);
      } else {
        final int prefixLength = PackerWriter.varIntLength(valueLength);
        writer.shiftRight(prefixOffset + 1, valueLength, prefixLength - 1);
        writer.backfillVarInt(prefixOffset, valueLength, prefixLength);
        writer.advance(prefixLength - 1);
      }
    }
    writer.writeVarInt(undeclaredProperties.size());
    for (final UndeclaredProperty prop : undeclaredProperties) {
      prop.write(writer);
    }
  }

  private void ensureWritable() {
    if (layoutFrozen) {
      final BaseProperty<? extends BaseValue>[] props = declaredArray;
      for (int i = 0; i < props.length; i++) {
        if (!props[i].hasValue()) {
          throw new StructPackException(
              String.format("Property '%s' has no valid value to write", props[i].getKey()));
        }
      }
    } else {
      for (final BaseProperty<? extends BaseValue> prop : declaredProperties) {
        if (!prop.hasValue()) {
          throw new StructPackException(
              String.format("Property '%s' has no valid value to write", prop.getKey()));
        }
      }
    }
  }

  @Override
  public void writeJSON(final StringBuilder builder) {
    builder.append('{');
    boolean first = true;
    for (final BaseProperty<? extends BaseValue> prop : declaredProperties) {
      if (prop.hasValue()) {
        if (!first) {
          builder.append(',');
        }
        first = false;
        prop.writeJSON(builder);
      }
    }
    for (final UndeclaredProperty prop : undeclaredProperties) {
      if (!first) {
        builder.append(',');
      }
      first = false;
      prop.writeJSON(builder);
    }
    builder.append('}');
  }
}
