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

/** 定时器行模型。 */
@Getter
@Setter
public class TimerDbModel {

  private long timerId;
  private Timestamp dueTime;
  private Integer repetitions;
  private Long processInstanceId;
  private Long activityInstanceId;
  private Long processDefinitionId;
  private String definitionKey;
  private String activityKey;
  private String timerElementType;
  private String timerType;
  private String timerContent;
  private String state;
  private Integer resourceId;
}
