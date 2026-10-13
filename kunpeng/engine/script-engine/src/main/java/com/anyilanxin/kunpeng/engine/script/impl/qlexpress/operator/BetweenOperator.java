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

package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.operator;

import com.alibaba.qlexpress4.QLPrecedences;
import com.alibaba.qlexpress4.runtime.Value;
import com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.CustomBaseBinaryOperator;
import com.google.common.collect.Range;

/**
 * between操作符
 *
 * @author zxuanhong
 */
public class BetweenOperator extends CustomBaseBinaryOperator {
  private static final BetweenOperator INSTANCE = new BetweenOperator();

  public BetweenOperator() {}

  public static BetweenOperator getInstance() {
    return INSTANCE;
  }

  @Override
  public Object execute(final Value left, final Value right) throws Throwable {
    final Object leftValue = left.get();
    final Object rightValue = right.get();
    if (!(leftValue instanceof Comparable) && !(leftValue instanceof Range)) {
      throw new CustomBusinessException("数据类错误，between左侧数据不可比较或者不是 range 类型");
    }
    if (!(rightValue instanceof final Range rightRange)) {
      throw new CustomBusinessException("数据类错误，between右侧数据不是 range 类型，请使用相关函数进行转换");
    }

    if (leftValue instanceof final Comparable leftComparable) {
      return rightRange.contains(leftComparable);
    } else {
      final Range leftRange = (Range) leftValue;
      return leftRange.isConnected(rightRange);
    }
  }

  @Override
  public String getOperator() {
    return "between";
  }

  @Override
  public int getPriority() {
    return QLPrecedences.IN_LIKE;
  }
}
