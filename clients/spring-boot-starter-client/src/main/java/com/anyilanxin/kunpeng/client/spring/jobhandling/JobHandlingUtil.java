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
package com.anyilanxin.kunpeng.client.spring.jobhandling;

import com.anyilanxin.kunpeng.client.command.CommandWithVariables;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.exception.JobError;
import com.anyilanxin.kunpeng.client.spring.jobhandling.parameter.ParameterResolver;
import com.anyilanxin.kunpeng.client.spring.jobhandling.parameter.ParameterResolverStrategy;
import com.anyilanxin.kunpeng.client.spring.jobhandling.result.ResultProcessor;
import com.anyilanxin.kunpeng.client.spring.jobhandling.result.ResultProcessorStrategy;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * job 处理工具类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JobHandlingUtil {

  private JobHandlingUtil() {}

  public static List<ParameterResolver> createParameterResolvers(
      final ParameterResolverStrategy parameterResolverStrategy, final JobWorkerValue workerValue) {
    return workerValue.getMethodInfo().getParameters().stream()
        .map(parameterResolverStrategy::createResolver)
        .toList();
  }

  public static ResultProcessor createResultProcessor(
      final ResultProcessorStrategy resultProcessorStrategy, final JobWorkerValue workerValue) {
    return resultProcessorStrategy.createProcessor(workerValue.getMethodInfo());
  }

  public static <T extends CommandWithVariables<T>> T applyVariables(
      final Object variables, final T command) {
    if (variables == null) {
      return command;
    } else if (variables.getClass().isAssignableFrom(Map.class)) {
      return command.variables((Map<String, Object>) variables);
    } else if (variables.getClass().isAssignableFrom(String.class)) {
      return command.variables((String) variables);
    } else if (variables.getClass().isAssignableFrom(InputStream.class)) {
      return command.variables((InputStream) variables);
    } else {
      return command.variables(variables);
    }
  }

  public static String createErrorMessage(final JobError jobError) {
    return ExceptionUtils.getStackTrace(jobError);
  }
}
