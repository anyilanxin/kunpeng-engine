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
package com.anyilanxin.kunpeng.client.spring.jobhandling.parameter;

import static com.anyilanxin.kunpeng.client.spring.annotation.AnnotationUtil.*;

import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import com.anyilanxin.kunpeng.client.spring.annotation.value.VariableValue;
import com.anyilanxin.kunpeng.client.spring.bean.ParameterInfo;

/**
 * 默认参数解析策略。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultParameterResolverStrategy implements ParameterResolverStrategy {
  protected final JsonMapper jsonMapper;
  private final JobClient jobClient;

  public DefaultParameterResolverStrategy(final JsonMapper jsonMapper, final JobClient jobClient) {
    this.jsonMapper = jsonMapper;
    this.jobClient = jobClient;
  }

  public DefaultParameterResolverStrategy(final JsonMapper jsonMapper) {
    this(jsonMapper, null);
  }

  @Override
  public ParameterResolver createResolver(final ParameterInfo parameterInfo) {
    final Class<?> parameterType = parameterInfo.getParameterInfo().getType();
    if (JobClient.class.isAssignableFrom(parameterType)) {
      return new JobClientParameterResolver();
    } else if (ActivatedJob.class.isAssignableFrom(parameterType)) {
      return new ActivatedJobParameterResolver();
    } else if (isVariable(parameterInfo)) {
      // get() can be used savely here as isVariable() verifies that an annotation is present
      final VariableValue variableValue = getVariableValue(parameterInfo).get();
      final String variableName = variableValue.getName();
      final boolean optional = variableValue.isOptional();
      return new VariableResolver(variableName, parameterType, jsonMapper, optional);
    } else if (isVariablesAsType(parameterInfo)) {
      return new VariablesAsTypeResolver(parameterType);
    } else if (isCustomHeaders(parameterInfo)) {
      return new CustomHeadersResolver();
    }
    throw new IllegalStateException(
        "Could not create parameter resolver for parameter " + parameterInfo);
  }
}
