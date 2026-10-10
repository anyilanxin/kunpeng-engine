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
package com.anyilanxin.kunpeng.sink.rdbms.model;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

/** 用户任务行模型。startTime 与定义类字段属创建期，仅随 insert 写入。 */
@Getter
@Setter
public class UserTaskDbModel {

  private long taskId;
  private Long activityInstanceId;
  private long processInstanceId;
  private String businessKey;
  private long processDefinitionId;
  private String definitionKey;
  private String taskDefinitionKey;
  private String taskName;
  private String assignee;
  private String owner;
  private Integer priority;
  private Timestamp dueTime;
  private Timestamp followUpTime;
  private String state;
  private String candidateGroups;
  private String candidateUsers;
  private Timestamp startTime;
  private Timestamp endTime;
  private Long duration;
  private Integer revision;
  private Integer resourceId;
}
