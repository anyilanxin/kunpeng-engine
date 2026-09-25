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
package com.anyilanxin.kunpeng.client.spring.properties;

import static com.anyilanxin.kunpeng.client.KunpengClientBuilderImpl.DEFAULT_JOB_WORKER_NAME_VAR;
import static com.anyilanxin.kunpeng.client.KunpengClientBuilderImpl.DEFAULT_JOB_WORKER_TENANT_IDS;
import static com.anyilanxin.kunpeng.client.spring.annotation.AnnotationUtil.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.spring.annotation.customizer.JobWorkerValueCustomizer;
import com.anyilanxin.kunpeng.client.spring.annotation.value.JobWorkerValue;
import com.anyilanxin.kunpeng.client.spring.bean.MethodInfo;
import com.anyilanxin.kunpeng.client.spring.bean.ParameterInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * 基于配置的 job worker 值定制器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class PropertyBasedJobWorkerValueCustomizer implements JobWorkerValueCustomizer {
  private static final Logger LOG =
      LoggerFactory.getLogger(PropertyBasedJobWorkerValueCustomizer.class);

  private final KunpengClientProperties kunpengClientProperties;

  public PropertyBasedJobWorkerValueCustomizer(
      final KunpengClientProperties kunpengClientProperties) {
    this.kunpengClientProperties = kunpengClientProperties;
  }

  @Override
  public void customize(final JobWorkerValue jobWorkerValue) {
    applyDefaultWorkerName(jobWorkerValue);
    applyDefaultJobWorkerType(jobWorkerValue);
    applyDefaultJobWorkerTenantIds(jobWorkerValue);
    applyFetchVariables(jobWorkerValue);
    applyOverrides(jobWorkerValue);
  }

  private void applyFetchVariables(final JobWorkerValue jobWorkerValue) {
    if (hasActivatedJobInjected(jobWorkerValue)) {
      LOG.debug(
          "Worker '{}': IncidentResolveCommand is injected, no variable filtering possible",
          jobWorkerValue.getName());
    } else if (jobWorkerValue.getForceFetchAllVariables() != null
        && jobWorkerValue.getForceFetchAllVariables()) {
      LOG.debug("Worker '{}': Force fetch all variables is enabled", jobWorkerValue.getName());
      jobWorkerValue.setFetchVariables(List.of());
    } else {
      final List<String> variables = new ArrayList<>();
      if (jobWorkerValue.getFetchVariables() != null) {
        variables.addAll(jobWorkerValue.getFetchVariables());
      }
      if (kunpengClientProperties.getWorker().getDefaults().getFetchVariables() != null) {
        variables.addAll(kunpengClientProperties.getWorker().getDefaults().getFetchVariables());
      }
      variables.addAll(
          readVariableParameters(jobWorkerValue.getMethodInfo()).stream()
              .map(this::extractVariableName)
              .toList());
      variables.addAll(readVariablesAsTypeParameters(jobWorkerValue.getMethodInfo()));
      jobWorkerValue.setFetchVariables(variables.stream().distinct().toList());
      LOG.debug(
          "Worker '{}': Fetching only required variables {}", jobWorkerValue.getName(), variables);
    }
  }

  private boolean hasActivatedJobInjected(final JobWorkerValue jobWorkerValue) {
    return jobWorkerValue.getMethodInfo().getParameters().stream()
        .anyMatch(p -> p.getParameterInfo().getType().isAssignableFrom(ActivatedJob.class));
  }

  private List<ParameterInfo> readVariableParameters(final MethodInfo methodInfo) {
    return getVariableParameters(methodInfo);
  }

  private String extractVariableName(final ParameterInfo parameterInfo) {
    // get can be used here as the list is already filtered by readVariableParameters
    return getVariableValue(parameterInfo).get().getName();
  }

  private List<String> readVariablesAsTypeParameters(final MethodInfo methodInfo) {
    final List<String> result = new ArrayList<>();
    final List<ParameterInfo> parameters = getVariablesAsTypeParameters(methodInfo);
    parameters.forEach(
        pi ->
            ReflectionUtils.doWithFields(
                pi.getParameterInfo().getType(), f -> result.add(extractFieldName(f))));
    return result;
  }

  private String extractFieldName(final Field field) {
    if (field.isAnnotationPresent(JsonProperty.class)) {
      final String value = field.getAnnotation(JsonProperty.class).value();
      if (StringUtils.isNotBlank(value)) {
        return value;
      }
    }
    return field.getName();
  }

  private void applyOverrides(final JobWorkerValue editedJobWorkerValue) {
    final KunpengClientJobWorkerProperties defaults =
        kunpengClientProperties.getWorker().getDefaults();
    if (defaults != null) {
      copyProperties(defaults, editedJobWorkerValue, OverrideSource.defaults);
    }
    final String workerType = editedJobWorkerValue.getType();
    findWorkerOverride(workerType)
        .ifPresent(
            jobWorkerValue -> {
              LOG.debug("Worker '{}': Applying overrides {}", workerType, jobWorkerValue);
              copyProperties(jobWorkerValue, editedJobWorkerValue, OverrideSource.worker);
            });
  }

  private Optional<KunpengClientJobWorkerProperties> findWorkerOverride(final String type) {
    return Optional.ofNullable(kunpengClientProperties.getWorker().getOverride().get(type));
  }

  private void copyProperties(
      final KunpengClientJobWorkerProperties source,
      final JobWorkerValue target,
      final OverrideSource overrideSource) {
    if (overrideSource == OverrideSource.worker) {
      copyProperty(
          "fetchVariables", overrideSource, source::getFetchVariables, target::setFetchVariables);
      copyProperty("type", overrideSource, source::getType, target::setType);
      copyProperty("name", overrideSource, source::getName, target::setName);
      copyProperty("tenantIds", overrideSource, source::getTenantIds, target::setTenantIds);
    }
    copyProperty("timeout", overrideSource, source::getTimeout, target::setTimeout);
    copyProperty(
        "maxJobsActive", overrideSource, source::getMaxJobsActive, target::setMaxJobsActive);
    copyProperty(
        "requestTimeout", overrideSource, source::getRequestTimeout, target::setRequestTimeout);
    copyProperty("pollInterval", overrideSource, source::getPollInterval, target::setPollInterval);
    copyProperty("autoComplete", overrideSource, source::getAutoComplete, target::setAutoComplete);
    copyProperty("enabled", overrideSource, source::getEnabled, target::setEnabled);
    //    copyProperty(
    //        "streamEnabled", overrideSource, source::getStreamEnabled, target::setStreamEnabled);
    copyProperty(
        "streamTimeout", overrideSource, source::getStreamTimeout, target::setStreamTimeout);
    copyProperty(
        "forceFetchAllVariables",
        overrideSource,
        source::getForceFetchAllVariables,
        target::setForceFetchAllVariables);
    copyProperty("maxRetries", overrideSource, source::getMaxRetries, target::setMaxRetries);
  }

  private <T> void copyProperty(
      final String propertyName,
      final OverrideSource overrideSource,
      final Supplier<T> getter,
      final Consumer<T> setter) {
    final T value = getter.get();
    if (value != null) {
      LOG.debug("Overriding property '{}' from source {}", propertyName, overrideSource);
      setter.accept(value);
    }
  }

  private void applyDefaultWorkerName(final JobWorkerValue jobWorkerValue) {
    final String defaultJobWorkerName = kunpengClientProperties.getWorker().getDefaults().getName();
    if (isBlank(jobWorkerValue.getName())) {
      if (isNotBlank(defaultJobWorkerName)
          && !DEFAULT_JOB_WORKER_NAME_VAR.equals(defaultJobWorkerName)) {
        LOG.debug(
            "Worker '{}': Setting name to default {}",
            jobWorkerValue.getName(),
            defaultJobWorkerName);
        jobWorkerValue.setName(defaultJobWorkerName);
      } else {
        final String generatedJobWorkerName =
            jobWorkerValue.getMethodInfo().getBeanName()
                + "#"
                + jobWorkerValue.getMethodInfo().getMethodName();
        LOG.debug(
            "Worker '{}': Setting name to generated {}",
            jobWorkerValue.getName(),
            generatedJobWorkerName);
        jobWorkerValue.setName(generatedJobWorkerName);
      }
    }
  }

  private void applyDefaultJobWorkerType(final JobWorkerValue jobWorkerValue) {
    final String defaultJobWorkerType = kunpengClientProperties.getWorker().getDefaults().getType();
    if (isBlank(jobWorkerValue.getType())) {
      if (isNotBlank(defaultJobWorkerType)) {
        LOG.debug(
            "Worker '{}': Setting type to default {}",
            jobWorkerValue.getName(),
            defaultJobWorkerType);
        jobWorkerValue.setType(defaultJobWorkerType);
      } else {
        final String generatedJobWorkerType = jobWorkerValue.getMethodInfo().getMethodName();
        LOG.debug(
            "Worker '{}': Setting type to generated {}",
            jobWorkerValue.getName(),
            generatedJobWorkerType);
        jobWorkerValue.setType(generatedJobWorkerType);
      }
    }
  }

  private void applyDefaultJobWorkerTenantIds(final JobWorkerValue jobWorkerValue) {
    final Set<String> tenantIds = new HashSet<>();

    // we consider default worker tenant ids configurations first
    if (!DEFAULT_JOB_WORKER_TENANT_IDS.equals(
        kunpengClientProperties.getWorker().getDefaults().getTenantIds())) {
      tenantIds.addAll(kunpengClientProperties.getWorker().getDefaults().getTenantIds());
    } else {
      // the default tenant set on the client is included in the default if no other default is set
      tenantIds.add(kunpengClientProperties.getTenantId());
    }

    // if set, worker annotation defaults get included as well
    if (jobWorkerValue.getTenantIds() != null) {
      tenantIds.addAll(jobWorkerValue.getTenantIds());
    }

    if (!tenantIds.isEmpty()) {
      LOG.debug("Worker '{}': Setting tenantIds to {}", jobWorkerValue.getName(), tenantIds);
      jobWorkerValue.setTenantIds(new ArrayList<>(tenantIds));
    }
  }

  private enum OverrideSource {
    defaults,
    worker
  }
}
