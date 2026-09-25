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
package com.anyilanxin.kunpeng.client.command.job;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceGrpc;
import java.time.Duration;
import java.util.function.Predicate;

public class JobUpdateCommandImpl
    implements UpdateJobCommandStep1, UpdateJobCommandStep1.UpdateJobCommandStep2 {

  private final JsonMapper jsonMapper;

  public JobUpdateCommandImpl(
      final JobServiceGrpc.JobServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate,
      final JsonMapper jsonMapper,
      final long jobKey) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public FinalCommandStep<UpdateJobResponse> requestTimeout(final Duration requestTimeout) {
    return this;
  }

  @Override
  public KunpengFuture<UpdateJobResponse> send() {
    return null;
  }

  @Override
  public UpdateJobCommandStep2 update(final JobChangeset jobChangeset) {
    return this;
  }

  @Override
  public UpdateJobCommandStep2 update(final Integer retries, final Long timeout) {
    return this;
  }

  @Override
  public UpdateJobCommandStep2 updateRetries(final int retries) {
    return this;
  }

  @Override
  public UpdateJobCommandStep2 updateTimeout(final long timeout) {
    return this;
  }

  @Override
  public UpdateJobCommandStep2 updateTimeout(final Duration timeout) {
    return updateTimeout(timeout.toMillis());
  }
}
