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
package com.anyilanxin.kunpeng.structpack.sample;

import com.anyilanxin.kunpeng.structpack.AutoDeclareProperties;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.property.DoubleProperty;
import com.anyilanxin.kunpeng.structpack.property.LongProperty;

/**
 * structpack Record 使用样例（活文档）。
 *
 * <p>开发流程：声明字段（可省 id）→ 打 {@code @AutoDeclareProperties} → 构建期
 * {@code autoDeclareProperties} 任务自动向初始化器分配冻结 id，并生成 declareProperty 链。
 * id 与 key 同行定义，新增/删除/改名字段与声明顺序无关。
 *
 * <p>删除零动作演示：历史字段 STATE(id=3) 已被直接删行 —— id 由 structpack-ids 标记注释
 * 自动退休（永不复用），存量 Raft 日志/RocksDB 中的旧数据被读方按值长度直接跳过。
 */
@AutoDeclareProperties
public class SampleRecord extends UnpackedObject {
  // structpack-ids[SampleRecord]: 1,2,3

  final LongProperty orderId = new LongProperty(1, "ORDER_ID", -1);
  final DoubleProperty amount = new DoubleProperty(2, "AMOUNT", 0);

  public SampleRecord() {
    super(2);
    // formatting:off
    declareProperty(orderId)
      .declareProperty(amount);
    // formatting:on
  }
}
