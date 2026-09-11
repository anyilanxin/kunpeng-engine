/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.bpm.model.bpmn.builder;

/**
 * A fluent builder for job worker related properties of elements that are based on jobs and are
 * processed by job workers (e.g. service tasks).
 */
public interface KunpengJobWorkerPropertiesBuilder<T> {

  /**
   * Sets a static type for the job.
   *
   * @param type the type of the job
   * @return the builder instance
   */
  T kunpengJobType(final String type);

  /**
   * Sets a dynamic type for the job that is retrieved from the given expression.
   *
   * @param expression the expression for the type of the job
   * @return the builder instance
   */
  T kunpengJobTypeExpression(final String expression);

  /**
   * Sets a static number of retries for the job.
   *
   * @param retries the number of job retries
   * @return the builder instance
   */
  T kunpengJobRetries(final String retries);

  /**
   * Sets a dynamic number of retries for the job that is retrieved from the given expression
   *
   * @param expression the expression for the number of job retries
   * @return the builder instance
   */
  T kunpengJobRetriesExpression(final String expression);
}
