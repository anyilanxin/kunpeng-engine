/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client;

import com.anyilanxin.kunpeng.client.command.deployment.DeleteResourceCommand;
import com.anyilanxin.kunpeng.client.command.deployment.DeployResourceCommand;
import com.anyilanxin.kunpeng.client.command.incident.resolve.IncidentResolveCommand;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.UpdateRetriesJobCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.UpdateTimeoutJobCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.worker.JobClient;
import com.anyilanxin.kunpeng.client.command.job.worker.JobWorkerBuilderStep1;
import com.anyilanxin.kunpeng.client.command.message.correlation.MessageCorrelationCommand;
import com.anyilanxin.kunpeng.client.command.processinstance.CancelProcessInstanceCommand;
import com.anyilanxin.kunpeng.client.command.processinstance.CreateProcessInstanceCommand;
import com.anyilanxin.kunpeng.client.command.signal.correlation.SignalCorrelationCommand;
import com.anyilanxin.kunpeng.client.command.topology.TopologyCommand;
import com.anyilanxin.kunpeng.client.command.usertask.cancel.CancelUserTaskCommand;
import com.anyilanxin.kunpeng.client.command.usertask.complete.CompleteUserTaskCommand;
import com.anyilanxin.kunpeng.client.command.variable.remove.RemoveVariableCommand;
import com.anyilanxin.kunpeng.client.command.variable.update.UpdateVariableCommand;
import com.anyilanxin.kunpeng.client.loadbalancer.GatewayServiceDiscoverClient;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface KunpengClient extends AutoCloseable, JobClient, GatewayServiceDiscoverClient {
  @Override
  void close();

  /**
   * @return a kunpeng client with default configuration values. In order to customize
   *     configuration, use the methods {@link #newClientBuilder()} or {@link
   *     #newClient(KunpengClientConfiguration)}. See {@link KunpengClientBuilder} for the
   *     configuration options and default values.
   */
  static KunpengClient newClient() {
    return newClientBuilder().build();
  }

  /**
   * @return a new {@link KunpengClient} using the provided configuration.
   */
  static KunpengClient newClient(final KunpengClientConfiguration configuration) {
    return new KunpengClientImpl(configuration);
  }

  /**
   * @return a builder to configure and create a new {@link KunpengClient}.
   */
  static KunpengClientBuilder newClientBuilder() {
    return new KunpengClientBuilderImpl();
  }

  /**
   * @return the client's configuration
   */
  KunpengClientConfiguration getConfiguration();

  /**
   * Command to deploy new resources, i.e. BPMN process models and DMN decision models.
   *
   * <pre>
   * kunpengClient
   *  .newDeployCommand()
   *  .addResourceFile("~/wf/process1.bpmn")
   *  .addResourceFile("~/wf/process2.bpmn")
   *  .addResourceFile("~/dmn/decision.dmn")
   *  .send();
   * </pre>
   *
   * @return a builder for the command
   */
  DeployResourceCommand newDeployResourceCommand();

  /**
   * Command to delete a resource.
   *
   * <pre>
   * kunpengClient
   *  .newDeleteResourceCommand(resourceKey)
   *  .send();
   * </pre>
   *
   * @param resourceKey the key of the resource
   * @return the builder for the command
   */
  DeleteResourceCommand newDeleteResourceCommand(long resourceKey);

  /**
   * Command to update the retries of a job.
   *
   * <pre>
   * long jobKey = ..;
   *
   * kunpengClient
   *  .newUpdateRetriesCommand(jobKey)
   *  .retries(3)
   *  .send();
   * </pre>
   *
   * <p>If the given retries are greater than zero then this job will be picked up again by a job
   * worker. This will not close a related incident, which still has to be marked as resolved with
   *
   * @param jobKey the key of the job to update
   * @return a builder for the command
   */
  UpdateRetriesJobCommandStep1 newUpdateRetriesCommand(long jobKey);

  /**
   * Command to update the retries of a job.
   *
   * <pre>
   * IncidentResolveCommand job= ..;
   * kunpengClient
   * .newUpdateRetriesCommand(job)
   * .retries(3)
   * .send();
   * </pre>
   *
   * <p>If the given retries are greater than zero then this job will be picked up again by a job
   * worker. This will not close a related incident, which still has to be marked as resolved with .
   *
   * @param job the activated job
   * @return a builder for the command
   */
  UpdateRetriesJobCommandStep1 newUpdateRetriesCommand(ActivatedJob job);

  /**
   * Command to update the timeout of a job.
   *
   * <pre>
   * long jobKey = ..;
   *
   * kunpengClient
   *  .newUpdateTimeoutCommand(jobKey)
   *  .timeout(100)
   *  .send();
   * </pre>
   *
   * <p>Timeout value in millis is used to calculate a new job deadline. This will happen when the
   * command to update the timeline is processed. The timeout value will be added to the current
   * time then.
   *
   * @param jobKey the key of the job to update
   * @return a builder for the command
   */
  UpdateTimeoutJobCommandStep1 newUpdateTimeoutCommand(long jobKey);

  /**
   * Command to update the timeout of a job.
   *
   * <pre>
   * IncidentResolveCommand job= ..;
   *
   * kunpengClient
   *  .newUpdateTimeoutCommand(job)
   *  .timeout(100)
   *  .send();
   * </pre>
   *
   * <p>Timeout value in millis is used to calculate a new job deadline. This will happen when the
   * command to update the timeline is processed. The timeout value will be added to the current
   * time then.
   *
   * @param job the activated job
   * @return a builder for the command
   */
  UpdateTimeoutJobCommandStep1 newUpdateTimeoutCommand(ActivatedJob job);

  /**
   * Registers a new job worker for jobs of a given type.
   *
   * <p>After registration, the broker activates available jobs and assigns them to this worker. It
   * then publishes them to the client. The given worker is called for every received job, works on
   * them and eventually completes them.
   *
   * <pre>
   * JobWorker worker = kunpengClient
   *  .newWorker()
   *  .jobType("payment")
   *  .handler(paymentHandler)
   *  .open();
   *
   * ...
   * worker.close();
   * </pre>
   *
   * <p>Example JobHandler implementation:
   *
   * <pre>
   * public final class PaymentHandler implements JobHandler
   * {
   *   &#64;Override
   *   public void handle(JobClient client, JobEvent jobEvent)
   *   {
   *     String json = jobEvent.getVariables();
   *     // modify variables
   *
   *     client
   *      .newCompleteCommand()
   *      .event(jobEvent)
   *      .variables(json)
   *      .send();
   *   }
   * };
   * </pre>
   *
   * @return a builder for the worker registration
   */
  JobWorkerBuilderStep1 newWorker();

  /**
   * Request the current cluster topology. Can be used to inspect which brokers are available at
   * which endpoint and which broker is the leader of which partition.
   *
   * <pre>
   * List&#60;BrokerInfo&#62; brokers = kunpengClient
   *  .newTopologyRequest()
   *  .send()
   *  .join()
   *  .getBrokers();
   *
   *  InetSocketAddress address = broker.getSocketAddress();
   *
   *  List&#60;PartitionInfo&#62; partitions = broker.getPartitions();
   * </pre>
   *
   * @return the request where you must call {@code send()}
   */
  TopologyCommand newTopologyCommand();

  /**
   * 创建流程实例
   *
   * @return {@link CreateProcessInstanceCommand }
   */
  CreateProcessInstanceCommand newCreateProcessInstanceCommand();

  /**
   * 取消流程实例
   *
   * @return {@link CancelProcessInstanceCommand }
   */
  CancelProcessInstanceCommand newCancelProcessInstanceCommand();

  /**
   * 完成用户任务
   *
   * @return {@link CompleteUserTaskCommand }
   */
  CompleteUserTaskCommand newCompleteUserTaskCommand();

  /**
   * 取消用户任务
   *
   * @return {@link CancelUserTaskCommand }
   */
  CancelUserTaskCommand newCancelUserTaskCommand();

  /**
   * 关联消息
   *
   * @return {@link MessageCorrelationCommand }
   */
  MessageCorrelationCommand newMessageCorrelationCommand();

  /**
   * 关联信号
   *
   * @return {@link SignalCorrelationCommand }
   */
  SignalCorrelationCommand newSignalCorrelationCommand();

  /**
   * 解决事故
   *
   * @param incidentId
   * @return {@link IncidentResolveCommand }
   */
  IncidentResolveCommand newIncidentResolveCommand(final long incidentId);

  /**
   * 变更变量
   *
   * @return {@link UpdateVariableCommand }
   */
  UpdateVariableCommand newVariableUpdateCommand();

  /**
   * 移除变量
   *
   * @return {@link RemoveVariableCommand }
   */
  RemoveVariableCommand newVariableRemoveCommand();
}
