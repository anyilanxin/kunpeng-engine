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

/** 变量行模型，主键为 (scopeId, name)。 */
@Getter
@Setter
public class VariableDbModel {

  private long scopeId;
  private String name;
  private Long parentScopeId;
  private long processInstanceId;
  private long processDefinitionId;
  private String valueJson;
  private Integer revision;
  private Integer resourceId;
}
