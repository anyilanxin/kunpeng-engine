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
package com.anyilanxin.kunpeng.client.command.job.worker;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.job.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceGrpc;
import java.util.function.Predicate;

/** job 命令入口：业务命令（complete/fail/throwError）与消费命令（activate/stream）统一走 JobService。 */
public final class JobClientImpl implements JobClient {

  private final JobServiceGrpc.JobServiceStub jobServiceStub;
  private final KunpengClientConfiguration config;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;

  public JobClientImpl(
      final JobServiceGrpc.JobServiceStub jobServiceStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.jobServiceStub = jobServiceStub;
    this.config = config;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;
  }

  @Override
  public CompleteJobCommandStep1 newCompleteCommand(final long jobKey) {
    return new CompleteJobCommandImpl(
        jobServiceStub, jsonMapper, config.getDefaultRequestTimeout(), retryPredicate, jobKey);
  }

  @Override
  public CompleteJobCommandStep1 newCompleteCommand(final ActivatedJob job) {
    return newCompleteCommand(job.getKey());
  }

  @Override
  public FailJobCommandStep1 newFailCommand(final long jobKey) {
    return new FailJobCommandImpl(
        jobServiceStub, jsonMapper, jobKey, config.getDefaultRequestTimeout(), retryPredicate);
  }

  @Override
  public FailJobCommandStep1 newFailCommand(final ActivatedJob job) {
    return newFailCommand(job.getKey());
  }

  @Override
  public ThrowErrorCommandStep1 newThrowErrorCommand(final long jobKey) {
    return new ThrowErrorCommandImpl(
        jobServiceStub, jsonMapper, jobKey, config.getDefaultRequestTimeout(), retryPredicate);
  }

  @Override
  public ThrowErrorCommandStep1 newThrowErrorCommand(final ActivatedJob job) {
    return newThrowErrorCommand(job.getKey());
  }

  @Override
  public ActivateJobsCommandStep1 newActivateJobsCommand() {
    return new ActivateJobsCommandImpl(jobServiceStub, config, jsonMapper, retryPredicate);
  }

  @Override
  public StreamJobsCommandStep1 newStreamJobsCommand() {
    return new StreamJobsCommandImpl(jobServiceStub, jsonMapper, retryPredicate, config);
  }
}
