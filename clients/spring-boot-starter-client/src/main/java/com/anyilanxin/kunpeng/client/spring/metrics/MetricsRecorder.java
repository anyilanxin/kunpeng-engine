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
package com.anyilanxin.kunpeng.client.spring.metrics;

/**
 * 指标记录器接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface MetricsRecorder {

  String METRIC_NAME_JOB = "camunda.job.invocations";
  String ACTION_ACTIVATED = "activated";
  String ACTION_COMPLETED = "completed";
  String ACTION_FAILED = "failed";
  String ACTION_BPMN_ERROR = "bpmn-error";

  /**
   * Increase the counter for the given metric name, action and type
   *
   * @param metricName - the name of the metric
   * @param action - event type within the metric, e.g. activated, completed, failed, bpmn-error
   * @param type - type of the job the metric is for
   */
  default void increase(final String metricName, final String action, final String type) {
    increase(metricName, action, type, 1);
  }

  /**
   * Increase the counter for the given metric name, action and type
   *
   * @param metricName - the name of the metric
   * @param action - event type within the metric, e.g. activated, completed, failed, bpmn-error
   * @param type - type of the job the metric is for
   * @param count - the amount to increase the metric by
   */
  void increase(String metricName, String action, String type, int count);

  /**
   * Execute the given runnable and measure the execution time
   *
   * <p>Note: the provided runnable is executed synchronously
   *
   * @param metricName - the name of the metric
   * @param jobType - type of the job the metric is for
   * @param methodToExecute - the method to execute
   */
  void executeWithTimer(String metricName, String jobType, Runnable methodToExecute);
}
