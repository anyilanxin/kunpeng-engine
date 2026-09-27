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
package com.anyilanxin.kunpeng.sink.rdbms.handler;

import com.anyilanxin.kunpeng.protocol.business.BusinessEventRecord;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.sink.rdbms.write.ChangeBuffer;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * 一种记录负载到表实体的翻译规则（数据转换层）。实现只做纯转换：读 {@link BusinessEventRecord#getValue() 记录值}，组装对应的 DbModel 实体并以
 * save（insert）/ update 两种形态写入 {@link ChangeBuffer}，不做任何 IO。
 *
 * <p>哪些列随 update 更新由各实体 mapper XML 的 update 语句表达；这里只负责在创建类生命周期上产全量实体（insert），
 * 其余生命周期上产只含可变列的实体（update）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface RecordModelHandler {

  /**
   * @return 本规则响应的负载类型
   */
  ValueType valueType();

  /**
   * @param lifecycle 记录的生命周期状态 @return 是否响应
   */
  boolean accepts(ValueLifeCycle lifecycle);

  /** 把一条记录翻译成零或多条行变更并放入缓冲。 */
  void transition(BusinessEventRecord<?> record, ChangeBuffer buffer);

  /** 引擎毫秒时间戳 -> 数据库时间戳；非正数视为未携带。 */
  static Timestamp at(final long epochMilli) {
    return epochMilli <= 0 ? null : Timestamp.from(Instant.ofEpochMilli(epochMilli));
  }
}
