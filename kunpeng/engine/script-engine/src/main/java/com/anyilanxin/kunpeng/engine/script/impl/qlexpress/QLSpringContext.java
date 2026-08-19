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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress;

import com.alibaba.qlexpress4.runtime.Value;
import com.alibaba.qlexpress4.runtime.context.ExpressContext;
import com.alibaba.qlexpress4.runtime.data.DataValue;
import com.alibaba.qlexpress4.runtime.data.MapItemValue;
import com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.BeanFactory;

/**
 * ql spring context
 *
 * @author zxuanhong
 * @date 2025-11-11 10:06
 * @since
 */
public class QLSpringContext implements ExpressContext {

  private final Map<String, Object> context;

  private final BeanFactory beanFactory;
  private final Set<String> outVarNames;

  public QLSpringContext(
      final Set<String> outVarNames,
      final Map<String, Object> context,
      final BeanFactory beanFactory) {
    this.context = Objects.requireNonNullElseGet(context, HashMap::new);
    this.beanFactory = beanFactory;
    this.outVarNames = outVarNames;
  }

  @Override
  public Value get(final Map<String, Object> attachments, final String variableName) {
    if (context.containsKey(variableName)) {
      return new MapItemValue(context, variableName);
    }
    if (beanFactory != null && beanFactory.containsBean(variableName)) {
      return new DataValue(beanFactory.getBean(variableName));
    }
    if ("context".equals(variableName)) {
      return new DataValue(context);
    }
    if (outVarNames.contains(variableName)) {
      throw new CustomBusinessException("'" + variableName + "' not found");
    }
    return new MapItemValue(context, variableName);
  }
}
