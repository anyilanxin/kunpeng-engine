package com.anyilanxin.modules.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 流程定义信息(ProcessDefinition)Entity
 *
 * <p>字段与 sink 建表（changelog 2026.9.0-create-process-definition）严格对齐，勿自行增删列名。
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
  @TableField(value = "DEFINITION_KEY")
  private String processDefinitionKey;

  /** 流程定义名称 */
  @TableField(value = "DEFINITION_NAME")
  private String name;

  /** 版本 */
  @TableField(value = "VERSION")
  private Integer version;

  /** 版本标签 */
  @TableField(value = "VERSION_TAG")
  private String versionTag;

  /** 历史留存时间 */
  @TableField(value = "HISTORY_TIME_TO_LIVE")
  private Integer historyTimeToLive;

  /** 部署 id */
  @TableField(value = "DEPLOYMENT_ID")
  private String deploymentId;

  /** 资源定义 id */
  @TableField(value = "RESOURCE_DEFINITION_ID")
  private String resourceDefinitionId;

  /** 资源名称 */
  @TableField(value = "RESOURCE_NAME")
  private String resourceName;

  /** 校验和 */
  @TableField(value = "CHECKSUM")
  private String checksum;

  /** 起始事件 */
  @TableField(value = "STARTER_EVENTS")
  private String starterEvents;

  /** 候选启动组 */
  @TableField(value = "CANDIDATE_STARTER_GROUPS")
  private String candidateStarterGroups;

  /** 候选启动用户 */
  @TableField(value = "CANDIDATE_STARTER_USERS")
  private String candidateStarterUsers;

  /** 模型元数据（bpmn xml 内容） */
  @TableField(value = "RESOURCE_CONTENT")
  private String bpmnXml;

  /** 资源 id */
  @TableField(value = "RESOURCE_ID")
  private Integer resourceId;
}
