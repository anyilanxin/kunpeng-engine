/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.numberandmath;

import static com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException.checkParam;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.alibaba.qlexpress4.runtime.QContext;
import com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException;
import com.anyilanxin.kunpeng.engine.script.impl.qlexpress.QLFunction;
import java.math.BigDecimal;
import java.time.Duration;

/** abs(n) */
public class AbsFunction implements QLFunction {
  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    if (parameters.size() != 1) {
      throw new CustomBusinessException("abs 函数需要一个参数，请确定传参信息");
    }
    checkParam(getSignature(), parameters.get(0));

    final Object value = parameters.getValue(0);
    switch (value) {
      case final Integer integerValue -> {
        return Math.abs(integerValue);
      }
      case final Long longValue -> {
        return Math.abs(longValue);
      }
      case final Float floatValue -> {
        return Math.abs(floatValue);
      }
      case final Double doubleValue -> {
        return Math.abs(doubleValue);
      }
      case final Byte byteValue -> {
        return Math.abs(byteValue);
      }
      case final Short shortValue -> {
        return Math.abs(shortValue);
      }
      case final Duration durationValue -> {
        return durationValue.abs();
      }
      case final BigDecimal bigDecimalValue -> {
        return bigDecimalValue.abs();
      }
      default -> throw new RuntimeException("不支持的类型:" + value.getClass().getSimpleName());
    }
  }

  @Override
  public String getSignature() {
    return "abs";
  }
}
