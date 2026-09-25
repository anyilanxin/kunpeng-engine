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
package com.anyilanxin.kunpeng.repository.business.modules.distribute.parallel.record;

/**
 * 并行分发状态枚举。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum DistributeParallelState {
  NULL_VAL((short) -1),
  WAIT((short) 0),
  ACK((short) 1);

  private final short value;

  DistributeParallelState(final short value) {
    this.value = value;
  }

  public short getValue() {
    return value;
  }

  public static DistributeParallelState fromValue(final short value) {
    return switch (value) {
      case 0 -> WAIT;
      case 1 -> ACK;
      default -> NULL_VAL;
    };
  }
}
