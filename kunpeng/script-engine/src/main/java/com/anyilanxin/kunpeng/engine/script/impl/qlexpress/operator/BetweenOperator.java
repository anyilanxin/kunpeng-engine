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
 * @date 2026/07/13
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
