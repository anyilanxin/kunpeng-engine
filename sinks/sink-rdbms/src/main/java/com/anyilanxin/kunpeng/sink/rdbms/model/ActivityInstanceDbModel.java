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
