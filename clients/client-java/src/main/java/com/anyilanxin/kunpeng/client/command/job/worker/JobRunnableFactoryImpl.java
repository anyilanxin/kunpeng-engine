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

import com.anyilanxin.kunpeng.client.ClientLoggers;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;

public final class JobRunnableFactoryImpl implements JobRunnableFactory {

  private static final Logger LOG = ClientLoggers.JOB_WORKER_LOGGER;

  private final JobClient jobClient;
  private final JobHandler handler;

  public JobRunnableFactoryImpl(final JobClient jobClient, final JobHandler handler) {
    this.jobClient = jobClient;
    this.handler = handler;
  }

  @Override
  public Runnable create(final ActivatedJob job, final Runnable doneCallback) {
    return () -> executeJob(job, doneCallback);
  }

  private void executeJob(final ActivatedJob job, final Runnable doneCallback) {
    try {
      handler.handle(jobClient, job);
    } catch (final Exception e) {
      LOG.warn(
          "Worker {} failed to handle job with key {} of type {}, sending fail command to broker",
          job.getWorker(),
          job.getKey(),
          job.getType(),
          e);
      final StringWriter stringWriter = new StringWriter();
      final PrintWriter printWriter = new PrintWriter(stringWriter);
      e.printStackTrace(printWriter);
      final String message = stringWriter.toString();
      jobClient
          .newFailCommand(job.getKey())
          .retries(job.getRetries() - 1)
          .errorMessage(message)
          .send();
    } finally {
      doneCallback.run();
    }
  }
}
