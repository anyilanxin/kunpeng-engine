package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.operator;

import com.alibaba.qlexpress4.QLPrecedences;
import com.alibaba.qlexpress4.runtime.Value;
import com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.CustomBaseBinaryOperator;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.model.RangeModel;

/**
 * .. 才分操作符
 *
 * @author zxuanhong
 * @date 2026/07/13
 */
public class RangeSplitOperator extends CustomBaseBinaryOperator {
  private static final RangeSplitOperator INSTANCE = new RangeSplitOperator();

  public RangeSplitOperator() {}

  public static RangeSplitOperator getInstance() {
    return INSTANCE;
  }

  @Override
  public Object execute(final Value left, final Value right) throws Throwable {
    final Object start = left.get();
    if (start == null) {
      throw new CustomBusinessException("操作符'..'左侧数据不能为 null");
    }
    final Object end = right.get();
    if (end == null) {
      throw new CustomBusinessException("操作符'..'右侧数据不能为 null");
    }
    if (start.getClass() != end.getClass()) {
      throw new CustomBusinessException(
          "操作符'..'左右两侧数据类型不一致'["
              + start.getClass().getSimpleName()
              + ".."
              + end.getClass().getSimpleName()
              + "]'");
    }
    return new RangeModel(left.get(), right.get());
  }

  @Override
  public String getOperator() {
    return "->";
  }

  @Override
  public int getPriority() {
    return QLPrecedences.ASSIGN;
  }
}
