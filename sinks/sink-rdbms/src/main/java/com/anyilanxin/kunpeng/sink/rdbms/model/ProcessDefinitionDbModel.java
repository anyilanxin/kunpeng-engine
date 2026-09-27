/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
