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
package com.anyilanxin.kunpeng.protocol.business.impl;

/**
 * 公共常量
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface BusinessRecordConstant {
  // =================公共部分=================
  /** 数据版本 */
  String VERSION = "REV";

  /** 生命周期 */
  String LIFE_CYCLE = "LIFE_CYCLE";

  /** 开始时间 */
  String START_TIME = "START_TIME";

  /** 开始时间 */
  String END_TIME = "END_TIME";

  /** 耗时 */
  String DURATION = "DURATION";

  /** 租户 id */
  String TENANT_ID = "TENANT_ID";

  /** 优先级 */
  String PRIORITY = "PRIORITY";

  /** 状态 */
  String STATE = "STATE";

  /** 变量 */
  String VARIABLES = "VARIABLES";

  String LOCAL_VARIABLES = "LOCAL_VARIABLES";

  // =================流程定义部分=================

  /** 流程定义 key(模型 id) */
  String PROCESS_DEFINITION_KEY = "PROCESS_DEFINITION_KEY";

  /** 流程定义版本 */
  String PROCESS_DEFINITION_VERSION = "PROCESS_DEFINITION_VERSION";

  /** 流程定义 名称 */
  String PROCESS_DEFINITION_NAME = "PROCESS_DEFINITION_NAME";

  /** 流程活动元素 key */
  String ACTIVITY_DEFINITION_KEY = "ACTIVITY_DEFINITION_KEY";

  /** 流程活动元素名称 */
  String ACTIVITY_DEFINITION_NAME = "ACTIVITY_DEFINITION_NAME";

  /** 流程活动元素类型 */
  String ACTIVITY_DEFINITION_TYPE = "ACTIVITY_DEFINITION_TYPE";

  /** 流程定义 id */
  String PROCESS_DEFINITION_ID = "PROCESS_DEFINITION_ID";

  // =================流程实例部分=================
  /** 附加信息 */
  String ADDITIONS = "ADDITIONS";

  /** 监听器类型 */
  String LISTENER_TYPE = "LISTENER_TYPE";

  /** 监听器下标 */
  String LISTENER_INDEX = "LISTENER_INDEX";

  /** 业务 key */
  String BUSINESS_KEY = "BUSINESS_KEY";

  /** 流程实例 id */
  String PROCESS_INSTANCE_ID = "PROCESS_INSTANCE_ID";

  /** 父级流程实例 id */
  String PARENT_PROCESS_INSTANCE_ID = "PARENT_PROCESS_INSTANCE_ID";

  String ROOT_PROCESS_INSTANCE_ID = "ROOT_PROCESS_INSTANCE_ID";

  /** 活动实例 id */
  String ACTIVITY_INSTANCE_ID = "ACTIVITY_INSTANCE_ID";

  /** 引用活动实例 id */
  String REFERENCE_ACTIVITY_INSTANCE_ID = "REFERENCE_ACTIVITY_INSTANCE_ID";

  /** 父级活动实例 id */
  String PARENT_ACTIVITY_INST_ID = "PARENT_ACTIVITY_INST_ID";

  // 用户任务部分
  /** 任务 id */
  String TASK_ID = "TASK_ID";

  /** 父级任务 id */
  String PARENT_TASK_ID = "PARENT_TASK_ID";

  /** 审批人 */
  String ASSIGNEE = "ASSIGNEE";

  // =================部署/资源通用=================
  /** 部署 id */
  String DEPLOYMENT_ID = "DEPLOYMENT_ID";

  /** 资源 id */
  String RESOURCE_ID = "RESOURCE_ID";

  /** 版本标签 */
  String VERSION_TAG = "VERSION_TAG";
}
