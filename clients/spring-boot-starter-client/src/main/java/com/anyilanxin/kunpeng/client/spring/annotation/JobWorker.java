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

import static com.anyilanxin.kunpeng.client.command.CommandWithTenantStep.DEFAULT_TENANT_IDENTIFIER;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.spring.annotation.processor.JobWorkerAnnotationProcessor;
import com.anyilanxin.kunpeng.client.spring.exception.BpmnError;
import com.anyilanxin.kunpeng.client.spring.exception.JobError;
import java.lang.annotation.*;

/**
 * job worker 消费方法注解：标记一个 job 处理方法，声明订阅的 job 类型、超时与拉取参数。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobWorker {

  /**
   * Set to empty string which leads to the method name being used (if not
   * ${camunda.client.worker.defaults.type} is configured) Implemented in {@link
   * JobWorkerAnnotationProcessor}
   */
  String type();

  /**
   * set to empty string which leads to default from KunpengClientBuilderImpl being used in {@link
   * JobWorkerAnnotationProcessor}
   */
  String name() default "";

  /**
   * Set the time (in milliseconds) for how long a job is exclusively assigned for this worker.
   * During this time, the job cannot be assigned by other workers to ensure that only one worker
   * works on the job. When the time is over, then the job can be assigned again by this or other
   * worker if it's not completed yet. If no timeout is set, then the default is used from the
   * {@link KunpengClientConfiguration}
   */
  long timeout() default -1L;

  /**
   * Set the maximum number of jobs which will be exclusively activated for this worker at the same
   * time. This is used to control the backpressure of the worker. When the maximum is reached, then
   * the worker will stop activating new jobs to not overwhelm the client and give other workers the
   * chance to work on the jobs. The worker will try to activate new jobs again when jobs are
   * completed (or marked as failed). If no maximum is set, then the default from the {@link
   * KunpengClientConfiguration}, is used. <br>
   * <br>
   * Considerations: A greater value can avoid situations in which the client waits idle for the
   * broker to provide more jobs. This can improve the worker's throughput. The memory used by the
   * worker is linear with respect to this value. The job's timeout starts to run down as soon as
   * the broker pushes the job. Keep in mind that the following must hold to ensure fluent job
   * handling:
   *
   * <pre>time spent in queue + time job handler needs until job completion < job timeout</pre>
   */
  int maxJobsActive() default -1;

  /**
   * Set the request timeout (in seconds) for activate job request used to poll for new jobs. If no
   * request timeout is set then the default is used from the {@link KunpengClientConfiguration}
   */
  long requestTimeout() default -1L;

  /**
   * Set the maximal interval (in milliseconds) between polling for new jobs. A job worker will
   * automatically try to always activate new jobs after completing jobs. If no jobs can be
   * activated after completing, the worker will periodically poll for new jobs. If no poll interval
   * is set then the default is used from the {@link KunpengClientConfiguration}
   */
  long pollInterval() default -1L;

  /**
   * Set a list of variable names which should be fetched on job activation. The jobs which are
   * activated by this worker will only contain variables from this list. This can be used to limit
   * the number of variables of the activated jobs.
   */
  String[] fetchVariables() default {};

  /** If set to true, all variables are fetched */
  boolean fetchAllVariables() default false;

  /**
   * If set to true, the job is automatically completed after the worker code has finished. In this
   * case, your worker code is not allowed to complete the job itself.
   *
   * <p>You can still throw exceptions if you want to raise a problem instead of job completion. To
   * control the retry behavior or submit variables, you can use the {@link JobError}. You could
   * also raise a BPMN error throwing a {@link BpmnError}
   */
  boolean autoComplete() default true;

  /** If set to true, the worker will actually be subscribing. */
  boolean enabled() default true;

  /** A list of tenants for this job will be worked on. */
  String[] tenantIds() default {DEFAULT_TENANT_IDENTIFIER};

  /**
   * Whether job streaming should be enabled for this job type. Useful in high-performance setups
   * but can only be used with a gRPC connection.
   */
  boolean streamEnabled() default false;

  /** Stream timeout in ms */
  long streamTimeout() default 3600000L;

  /** Set the max number of retries for a job */
  int maxRetries() default -1;
}
