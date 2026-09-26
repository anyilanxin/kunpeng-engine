package com.anyilanxin.modules.resource.service.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 部署信息分页查询Response
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 16:00:12
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode
@Schema
public class ProcessDefinitionPageVo implements Serializable {
  @Serial private static final long serialVersionUID = 785720097483414917L;

  /** 流程定义id */
  private String processDefinitionId;

  /** 流程定义key */
  private String processDefinitionKey;

  /** 模型名称 */
  private String name;

  /** 资源名称 */
  private String resourceName;

  /** 模型元数据 */
  private String bpmnXml;

  /** 租户 id */
  private String tenantId;

  /** 模型版本 */
  private Integer version;

  /** 模型版本标签 */
  private String versionTag;

  /** 表单id */
  private String formId;
}
