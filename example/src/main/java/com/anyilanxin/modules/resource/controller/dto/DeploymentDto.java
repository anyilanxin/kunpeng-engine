/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.modules.resource.controller.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author zxuanhong
 * @date 2026-03-05 12:22
 * @since
 */
@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@NoArgsConstructor
public class DeploymentDto implements Serializable {
  @Serial private static final long serialVersionUID = 1772684724997L;

  /** 部署激活时间 */
  private String activateProcessDate;

  /** 类别 */
  private String category;

  /** 部署名称 */
  private String deploymentName;

  /** 模型 base64数据 */
  private String diagramBase64Data;

  /** 模型名称 */
  private String diagramNames;

  /** 是否此话：0-不是，1-是 */
  private Integer havePool;

  /** 流程定义 key */
  private String processDefinitionKeys;
}
