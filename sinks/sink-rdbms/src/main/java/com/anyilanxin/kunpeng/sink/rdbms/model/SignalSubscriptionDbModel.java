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

import lombok.Getter;
import lombok.Setter;

/** 信号订阅行模型。除 state 外均属创建期字段。 */
@Getter
@Setter
public class SignalSubscriptionDbModel {

  private long signalSubscriptionId;
  private String signalName;
  private String signalType;
  private Long processInstanceId;
  private Long activityInstanceId;
  private Long processDefinitionId;
  private String definitionKey;
  private String activityKey;
  private String state;
  private Integer resourceId;
}
