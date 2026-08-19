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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress.function.range.utils;

import static com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException.checkParam;

import com.alibaba.qlexpress4.runtime.Parameters;
import com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException;
import java.util.List;

/**
 * @author zxuanhong
 * @date 2026-07-14 12:05
 * @since
 */
public class RangeUtil {
  private RangeUtil() {}

  public static <C extends Comparable> List<C> rangeCheckParam(
      final String signature, final Parameters parameters) {
    if (parameters.size() != 2) {
      throw new CustomBusinessException(signature + " 函数需要两个参数，请确定传参信息");
    }
    final Object start = checkParam(signature, parameters.get(0));
    final Object end = checkParam(signature, parameters.get(1));
    if (!(start instanceof Comparable)) {
      throw new CustomBusinessException(
          signature + " 函数第一个参数不可比较" + start.getClass().getSimpleName());
    }
    if (!(end instanceof Comparable)) {
      throw new CustomBusinessException(
          signature + " 函数第二个参数不可比较" + start.getClass().getSimpleName());
    }
    if (start.getClass() != end.getClass()) {
      throw new CustomBusinessException(
          signature
              + " 函数两个参数类型不一致("
              + start.getClass().getSimpleName()
              + ","
              + end.getClass().getSimpleName()
              + ")");
    }
    return List.of((C) start, (C) end);
  }
}
