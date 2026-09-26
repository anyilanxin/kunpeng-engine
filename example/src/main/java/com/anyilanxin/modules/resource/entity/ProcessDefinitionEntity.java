package com.anyilanxin.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 部署信息(ProcessDefinition)Entity
 *
 * @author zxh
 * @copyright zhouxuanhong（https://anyilanxin.com）
 * @date 2026-04-23 16:00:12
 * @since v1.0.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@TableName("ANYI_PROCESS_DEFINITION")
public class ProcessDefinitionEntity implements Serializable {
  @Serial private static final long serialVersionUID = -97216999094221894L;

  /** 流程定义id */
  @TableId(value = "PROCESS_DEFINITION_ID")
  private String processDefinitionId;

  /** 流程定义key */
  @TableField(value = "PROCESS_DEFINITION_KEY")
  private String processDefinitionKey;

  /** 模型名称 */
  @TableField(value = "NAME")
  private String name;

  /** 资源名称 */
  @TableField(value = "RESOURCE_NAME")
  private String resourceName;

  /** 模型元数据 */
  @TableField(value = "BPMN_XML")
  private String bpmnXml;

  /** 租户 id */
  @TableField(value = "TENANT_ID")
  private String tenantId;

  /** 模型版本 */
  @TableField(value = "VERSION")
  private Integer version;

  /** 模型版本标签 */
  @TableField(value = "VERSION_TAG")
  private String versionTag;

  /** 表单id */
  @TableField(value = "FORM_ID")
  private String formId;
}
