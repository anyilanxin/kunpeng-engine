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

import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;

/**
 * 变量参数解析器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class VariableResolver implements ParameterResolver {
  private final String variableName;
  private final Class<?> variableType;
  private final JsonMapper jsonMapper;
  private final boolean optional;

  public VariableResolver(
      final String variableName,
      final Class<?> variableType,
      final JsonMapper jsonMapper,
      final boolean optional) {
    this.variableName = variableName;
    this.variableType = variableType;
    this.jsonMapper = jsonMapper;
    this.optional = optional;
  }

  @Override
  public Object resolve(final JobClient jobClient, final ActivatedJob job) {
    final Object variableValue = getVariable(job);
    if (variableValue == null) {
      if (optional) {
        return null;
      } else {
        throw new IllegalStateException(
            "Variable " + variableName + " is mandatory, but no value was found");
      }
    }
    try {
      return mapEngineVariable(variableValue);
    } catch (final ClassCastException | IllegalArgumentException ex) {
      throw new RuntimeException(
          "Cannot assign process variable '"
              + variableName
              + "' to parameter when executing job '"
              + job.getType()
              + "', invalid type found: "
              + ex.getMessage());
    }
  }

  protected Object getVariable(final ActivatedJob job) {
    return job.getVariablesAsMap().get(variableName);
  }

  protected Object mapEngineVariable(final Object variableValue) {
    if (variableValue != null && !variableType.isInstance(variableValue)) {
      return jsonMapper.fromJson(jsonMapper.toJson(variableValue), variableType);
    } else {
      return variableType.cast(variableValue);
    }
  }
}
