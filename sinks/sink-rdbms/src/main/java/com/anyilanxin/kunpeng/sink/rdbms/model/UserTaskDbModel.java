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
