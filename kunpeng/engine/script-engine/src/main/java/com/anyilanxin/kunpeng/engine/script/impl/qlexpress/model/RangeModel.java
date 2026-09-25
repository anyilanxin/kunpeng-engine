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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.ToString;

/**
 * 区间模型：range 表达式的区间数据载体。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Getter
@ToString
public class RangeModel implements Serializable {
  private final Object start;
  private final Object end;

  public RangeModel(final Object start, final Object end) {
    this.start = start;
    this.end = end;
  }
}
