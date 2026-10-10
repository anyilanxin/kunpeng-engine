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

/** 流程定义行模型（表列一一对应，映射层负责从记录值填充）。 */
@Getter
@Setter
public class ProcessDefinitionDbModel {

  private long processDefinitionId;
  private String definitionKey;
  private String definitionName;
  private int version;
  private String versionTag;
  private Integer historyTimeToLive;
  private Long deploymentId;
  private Long resourceDefinitionId;
  private String resourceName;
  private String checksum;
  private String starterEvents;
  private String candidateStarterGroups;
  private String candidateStarterUsers;
  private String resourceContent;
  private Integer resourceId;
}
