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

import com.anyilanxin.kunpeng.client.command.job.worker.JobWorkerMetrics;

/**
 * Bridge between spring-sdk metrics and camunda-client metrics. One way flow where camunda-client
 * metrics get propagated to MetricRecorder format in Spring.
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class KunpengClientMetricsBridge implements JobWorkerMetrics {

  private final MetricsRecorder metricsRecorder;
  private final String jobType;

  public KunpengClientMetricsBridge(final MetricsRecorder metricsRecorder, final String jobType) {
    this.metricsRecorder = metricsRecorder;
    this.jobType = jobType;
  }

  @Override
  public void jobActivated(final int count) {
    metricsRecorder.increase("camunda.client.worker.job", "activated", jobType, count);
  }

  @Override
  public void jobHandled(final int count) {
    metricsRecorder.increase("camunda.client.worker.job", "handled", jobType, count);
  }
}
