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
package com.anyilanxin.kunpeng.client.spring.bean;

import java.lang.reflect.Parameter;

/**
 * 参数信息。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ParameterInfo implements BeanInfo {
  private final MethodInfo methodInfo;
  private final String parameterName;
  private final Parameter parameterInfo;

  public ParameterInfo(final MethodInfo methodInfo, final Parameter param, final String paramName) {
    this.methodInfo = methodInfo;
    if (paramName == null) {
      parameterName = param.getName();
    } else {
      parameterName = paramName;
    }
    parameterInfo = param;
  }

  public Parameter getParameterInfo() {
    return parameterInfo;
  }

  public String getParameterName() {
    return parameterName;
  }

  public MethodInfo getMethodInfo() {
    return methodInfo;
  }

  @Override
  public String toString() {
    return "ParameterInfo{"
        + "parameterName="
        + parameterName
        + ", parameterInfo="
        + parameterInfo
        + '}';
  }

  @Override
  public Object getBean() {
    return methodInfo.getBean();
  }

  @Override
  public String getBeanName() {
    return methodInfo.getBeanName();
  }
}
