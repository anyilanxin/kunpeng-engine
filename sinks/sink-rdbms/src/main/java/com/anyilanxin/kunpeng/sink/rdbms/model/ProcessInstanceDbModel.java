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

/** 流程实例行模型。startTime 属创建期字段，仅随 insert 写入。 */
@Getter
@Setter
public class ProcessInstanceDbModel {

  private long processInstanceId;
  private Long parentProcessInstanceId;
  private Long rootProcessInstanceId;
  private String businessKey;
  private long processDefinitionId;
  private String definitionKey;
  private String definitionName;
  private String startUser;
  private String state;
  private Timestamp startTime;
  private Timestamp endTime;
  private Integer revision;
  private Integer resourceId;
}
