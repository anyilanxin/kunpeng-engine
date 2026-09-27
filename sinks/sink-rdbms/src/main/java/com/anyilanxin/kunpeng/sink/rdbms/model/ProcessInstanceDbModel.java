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
