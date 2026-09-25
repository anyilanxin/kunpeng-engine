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

import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.job.worker.ActivatedJobImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass;
import java.util.ArrayList;
import java.util.List;

public final class ActivateJobsResponseImpl implements ActivateJobsResponse {

  private final JsonMapper jsonMapper;
  private final List<ActivatedJob> jobs = new ArrayList<>();

  public ActivateJobsResponseImpl(final JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  public void addResponse(final JobServiceOuterClass.PullResponse pullResponse) {
    pullResponse.getJobsList().stream()
        .map(r -> new ActivatedJobImpl(jsonMapper, r))
        .forEach(jobs::add);
  }

  @Override
  public List<ActivatedJob> getJobs() {
    return jobs;
  }
}
