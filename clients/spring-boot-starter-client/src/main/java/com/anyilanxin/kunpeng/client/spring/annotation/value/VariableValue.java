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
package com.anyilanxin.kunpeng.client.spring.annotation.value;

import com.anyilanxin.kunpeng.client.spring.bean.ParameterInfo;

/**
 * 变量注解值信息。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableValue implements KunpengAnnotationValue<ParameterInfo> {
  private final String name;
  private final ParameterInfo parameterInfo;
  private final boolean optional;

  public VariableValue(
      final String name, final ParameterInfo parameterInfo, final boolean optional) {
    this.name = name;
    this.parameterInfo = parameterInfo;
    this.optional = optional;
  }

  public String getName() {
    return name;
  }

  @Override
  public ParameterInfo getBeanInfo() {
    return parameterInfo;
  }

  public boolean isOptional() {
    return optional;
  }
}
