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

/** 活动实例行模型。startTime 属创建期字段，仅随 insert 写入。 */
@Getter
@Setter
public class ActivityInstanceDbModel {

  private long activityInstanceId;
  private long processInstanceId;
  private Long rootProcessInstanceId;
  private Long parentActivityInstanceId;
  private Long callProcessInstanceId;
  private long processDefinitionId;
  private String definitionKey;
  private String activityKey;
  private String activityName;
  private String activityType;
  private Long taskId;
  private String assignee;
  private String startActivityDefinitionKey;
  private Long startActivityInstanceId;
  private String state;
  private Long incidentId;
  private Long sequenceCounter;
  private Timestamp startTime;
  private Timestamp endTime;
  private Long duration;
  private Integer revision;
  private Integer resourceId;
}
