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

/** 故障行模型。createdTime 属创建期字段，仅随 insert 写入。 */
@Getter
@Setter
public class IncidentDbModel {

  private long incidentId;
  private String incidentType;
  private String incidentMessage;
  private Long processInstanceId;
  private Long activityInstanceId;
  private Long processDefinitionId;
  private String definitionKey;
  private String activityKey;
  private Long taskId;
  private Long jobId;
  private String relatedValueType;
  private String relatedLifecycle;
  private String state;
  private Timestamp createdTime;
  private Timestamp resolvedTime;
  private Integer resourceId;
}
