/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.runtime.metrics;

import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import java.util.Map;

public class ConnectorMetrics {

  public static class Tag {

    public static final String ELEMENT_TEMPLATE_ID = "elementTemplateId";
    public static final String TYPE = "type";
    public static final String ACTION = "action";
    public static final String ELEMENT_TEMPLATE_VERSION = "elementTemplateVersion";
    public static final String RESULT = "result";
    public static final String PHYSICAL_TENANT_ID = "physicalTenantId";
  }

  public static class Outbound {

    public static final String METRIC_NAME_INVOCATIONS = "camunda.connector.outbound.invocations";
    public static final String METRIC_NAME_TIME = "camunda.connector.outbound.execution-time";

    /** Jobs pulled from the broker queue, tagged by connector {@code type}. */
    public static final String METRIC_NAME_WORKER_JOB_ACTIVATED =
        "camunda.client.worker.job.activated";

    /** Jobs acknowledged back to the broker, tagged by connector {@code type}. */
    public static final String METRIC_NAME_WORKER_JOB_HANDLED = "camunda.client.worker.job.handled";

    /**
     * Epoch-millisecond timestamp of the last successfully completed job, per connector type. Value
     * is {@code 0} if no job has completed yet.
     */
    public static final String METRIC_NAME_LAST_COMPLETED =
        "camunda.connector.outbound.last-completed";

    /**
     * Epoch-millisecond timestamp of the last failed job, per connector type. Value is {@code 0} if
     * no job has failed yet.
     */
    public static final String METRIC_NAME_LAST_FAILED = "camunda.connector.outbound.last-failed";

    /**
     * All-time maximum execution duration in milliseconds, per connector type. Unlike the Timer's
     * built-in max (which decays after ~2 minutes of inactivity), this gauge is never reset.
     */
    public static final String METRIC_NAME_MAX_EXECUTION_TIME =
        "camunda.connector.outbound.max-execution-time";

    /**
     * Number of times a job-stream was recreated due to inactivity, tagged by connector {@code
     * type}. Spikes indicate broker connectivity instability.
     */
    public static final String METRIC_NAME_WORKER_STREAM_INACTIVITY_RECREATED =
        "camunda.client.worker.stream.inactivity.recreated";

    /** Value of the {@code action} tag for successfully completed jobs. */
    public static final String ACTION_COMPLETED = "completed";

    /** Value of the {@code action} tag for jobs that ended with a connector error. */
    public static final String ACTION_FAILED = "failed";

    /** Value of the {@code action} tag for jobs that threw a BPMN error. */
    public static final String ACTION_BPMN_ERROR = "bpmn-error";
  }

  /**
   * Physical tenant (engine) a meter is attributed to when none could be resolved — the runtime
   * wires a single engine, which reports under this fixed tenant id.
   */
  public static final String DEFAULT_PHYSICAL_TENANT_ID = "default";

  public record CounterMetricsContext(String metricName, Map<String, String> tags, long count) {}

  public record TimerMetricsContext(String metricName, Map<String, String> tags) {}

  public static CounterMetricsContext counter(ActivatedJob job) {
    return counter(job, null);
  }

  /**
   * Attributes the counter to the physical tenant the job worker was opened for, so that the meter
   * and the {@code /outbound} entry it belongs to always carry the same value.
   */
  public static CounterMetricsContext counter(ActivatedJob job, String physicalTenantId) {
    Result result = Result.getResult(job);
    return new CounterMetricsContext(
        Outbound.METRIC_NAME_INVOCATIONS,
        Map.ofEntries(
            Map.entry(ConnectorMetrics.Tag.TYPE, result.type()),
            Map.entry(ConnectorMetrics.Tag.ELEMENT_TEMPLATE_ID, result.id()),
            Map.entry(ConnectorMetrics.Tag.ELEMENT_TEMPLATE_VERSION, result.version()),
            Map.entry(
                ConnectorMetrics.Tag.PHYSICAL_TENANT_ID,
                resolvePhysicalTenantId(physicalTenantId))),
        1);
  }

  /** See {@link #counter(ActivatedJob)}; the equivalent for the execution-time timer. */
  public static TimerMetricsContext timer(ActivatedJob job) {
    return timer(job, null);
  }

  public static TimerMetricsContext timer(ActivatedJob job, String physicalTenantId) {
    Result result = Result.getResult(job);
    return new TimerMetricsContext(
        ConnectorMetrics.Outbound.METRIC_NAME_TIME,
        Map.ofEntries(
            Map.entry(ConnectorMetrics.Tag.TYPE, result.type()),
            Map.entry(ConnectorMetrics.Tag.ELEMENT_TEMPLATE_ID, result.id()),
            Map.entry(ConnectorMetrics.Tag.ELEMENT_TEMPLATE_VERSION, result.version()),
            Map.entry(
                ConnectorMetrics.Tag.PHYSICAL_TENANT_ID,
                resolvePhysicalTenantId(physicalTenantId))));
  }

  /** A tag value is never allowed to be null. */
  private static String resolvePhysicalTenantId(String physicalTenantId) {
    return physicalTenantId == null || physicalTenantId.isBlank()
        ? DEFAULT_PHYSICAL_TENANT_ID
        : physicalTenantId;
  }
}
