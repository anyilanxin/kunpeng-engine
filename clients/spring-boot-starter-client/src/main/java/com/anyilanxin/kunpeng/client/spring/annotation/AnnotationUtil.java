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
package com.anyilanxin.kunpeng.client.spring.annotation;

import com.anyilanxin.kunpeng.client.spring.annotation.value.DeploymentValue;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.annotation.value.VariableValue;
import com.anyilanxin.kunpeng.client.spring.bean.BeanInfo;
import com.anyilanxin.kunpeng.client.spring.bean.ClassInfo;
import com.anyilanxin.kunpeng.client.spring.bean.MethodInfo;
import com.anyilanxin.kunpeng.client.spring.bean.ParameterInfo;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 注解工具类。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class AnnotationUtil {
  private static final Logger LOG = LoggerFactory.getLogger(AnnotationUtil.class);

  public static boolean isVariable(final ParameterInfo parameterInfo) {
    return parameterInfo.getParameterInfo().isAnnotationPresent(Variable.class);
  }

  public static List<ParameterInfo> getVariableParameters(final MethodInfo methodInfo) {
    return new ArrayList<>(methodInfo.getParametersFilteredByAnnotation(Variable.class));
  }

  public static List<ParameterInfo> getVariablesAsTypeParameters(final MethodInfo methodInfo) {
    return new ArrayList<>(methodInfo.getParametersFilteredByAnnotation(VariablesAsType.class));
  }

  public static boolean isVariablesAsType(final ParameterInfo parameterInfo) {
    return parameterInfo.getParameterInfo().isAnnotationPresent(VariablesAsType.class);
  }

  public static boolean isCustomHeaders(final ParameterInfo parameterInfo) {
    return parameterInfo.getParameterInfo().isAnnotationPresent(CustomHeaders.class);
  }

  public static boolean isDeployment(final ClassInfo classInfo) {
    return classInfo.hasClassAnnotation(Deployment.class);
  }

  public static boolean isJobWorker(final BeanInfo beanInfo) {
    return beanInfo.hasMethodAnnotation(JobWorker.class);
  }

  public static Optional<JobWorkerValue> getJobWorkerValue(final MethodInfo methodInfo) {
    return getJobWorkerValueInternal(methodInfo);
  }

  private static Optional<JobWorkerValue> getJobWorkerValueInternal(final MethodInfo methodInfo) {
    final Optional<JobWorker> methodAnnotation = methodInfo.getAnnotation(JobWorker.class);
    if (methodAnnotation.isPresent()) {
      final JobWorker annotation = methodAnnotation.get();
      return Optional.of(
          new JobWorkerValue(
              annotation.type(),
              workerName(annotation, methodInfo),
              Duration.of(annotation.timeout(), ChronoUnit.MILLIS),
              annotation.maxJobsActive(),
              Duration.of(annotation.requestTimeout(), ChronoUnit.SECONDS),
              Duration.of(annotation.pollInterval(), ChronoUnit.MILLIS),
              annotation.autoComplete(),
              Arrays.asList(annotation.fetchVariables()),
              annotation.enabled(),
              methodInfo,
              Arrays.asList(annotation.tenantIds()),
              annotation.fetchAllVariables(),
              annotation.streamEnabled(),
              Duration.of(annotation.streamTimeout(), ChronoUnit.MILLIS),
              annotation.maxRetries()));
    }
    return Optional.empty();
  }

  /**
   * worker 名决策：注解显式指定优先；为空则生成方法指纹 {@code beanName#methodName}（可读且跨重启稳定， lockOwner
   * 归属记录可对应；同一方法多实例天然同名，配合聚合订阅按 worker 合并水位线）。
   */
  private static String workerName(final JobWorker annotation, final MethodInfo methodInfo) {
    final String declared = annotation.name();
    return declared == null || declared.isEmpty()
        ? methodInfo.getBeanName() + "#" + methodInfo.getMethodName()
        : declared;
  }

  public static Optional<VariableValue> getVariableValue(final ParameterInfo parameterInfo) {
    if (isVariable(parameterInfo)) {
      return Optional.of(
          new VariableValue(
              getVariableName(parameterInfo), parameterInfo, getVariableOptional(parameterInfo)));
    }
    return Optional.empty();
  }

  public static Optional<DeploymentValue> getDeploymentValue(final ClassInfo beanInfo) {
    if (isDeployment(beanInfo)) {
      final List<String> resources = new ArrayList<>(getDeploymentResources(beanInfo));
      return Optional.of(DeploymentValue.builder().beanInfo(beanInfo).resources(resources).build());
    }
    return Optional.empty();
  }

  private static List<String> getDeploymentResources(final ClassInfo beanInfo) {
    return beanInfo
        .getAnnotation(Deployment.class)
        .map(Deployment::resources)
        .map(Arrays::asList)
        .orElseGet(List::of);
  }

  private static String getVariableName(final ParameterInfo param) {
    if (param.getParameterInfo().isAnnotationPresent(Variable.class)) {
      final String nameFromAnnotation =
          param.getParameterInfo().getAnnotation(Variable.class).name();
      if (StringUtils.isNotBlank(nameFromAnnotation)) {
        LOG.trace("Extracting name {} from Variable.name", nameFromAnnotation);
        return nameFromAnnotation;
      }
      final String valueFromAnnotation =
          param.getParameterInfo().getAnnotation(Variable.class).value();
      if (StringUtils.isNotBlank(valueFromAnnotation)) {
        LOG.trace("Extracting name {} from Variable.value", valueFromAnnotation);
        return valueFromAnnotation;
      }
    }
    LOG.trace("Extracting variable name from parameter name");
    return param.getParameterName();
  }

  private static boolean getVariableOptional(final ParameterInfo param) {
    if (param.getParameterInfo().isAnnotationPresent(Variable.class)) {
      final boolean optional = param.getParameterInfo().getAnnotation(Variable.class).optional();
      LOG.trace("Extracting optional flag from Variable");
      return optional;
    }
    LOG.trace("No variable annotation found, defaulting to true");
    return true;
  }
}
