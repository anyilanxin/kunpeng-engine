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

import java.util.List;
import java.util.function.UnaryOperator;

public class CompleteUserTaskJobResultImpl implements CompleteUserTaskJobResultStep1 {

  private boolean isDenied;
  private String deniedReason;
  private JobResultCorrections corrections;

  public CompleteUserTaskJobResultImpl() {
    corrections = new JobResultCorrections();
  }

  @Override
  public CompleteUserTaskJobResultImpl deny(final boolean isDenied) {
    this.isDenied = isDenied;
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl deny(final boolean isDenied, final String deniedReason) {
    deny(isDenied);
    deniedReason(deniedReason);
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl deniedReason(final String deniedReason) {
    this.deniedReason = deniedReason;
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl correct(final JobResultCorrections corrections) {
    if (corrections == null) {
      this.corrections = new JobResultCorrections();
    } else {
      this.corrections = corrections;
    }
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl correct(
      final UnaryOperator<JobResultCorrections> corrections) {
    return correct(corrections.apply(this.corrections));
  }

  @Override
  public CompleteUserTaskJobResultImpl correctAssignee(final String assignee) {
    corrections.assignee(assignee);
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl correctDueDate(final String dueDate) {
    corrections.dueDate(dueDate);
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl correctFollowUpDate(final String followUpDate) {
    corrections.followUpDate(followUpDate);
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl correctCandidateGroups(final List<String> candidateGroups) {
    corrections.candidateGroups(candidateGroups);
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl correctCandidateUsers(final List<String> candidateUsers) {
    corrections.candidateUsers(candidateUsers);
    return this;
  }

  @Override
  public CompleteUserTaskJobResultImpl correctPriority(final Integer priority) {
    corrections.priority(priority);
    return this;
  }

  public boolean isDenied() {
    return isDenied;
  }

  public String getDeniedReason() {
    return deniedReason;
  }

  public JobResultCorrections getCorrections() {
    return corrections;
  }

  @Override
  public JobResultType getType() {
    return JobResultType.USER_TASK;
  }
}
