/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import java.util.List;

/**
 * sink 使用的全部表定义。这里登记的是不带前缀的逻辑表名（变量注册与大小写渲染见 {@link TableName} 枚举）， 实际表名 = 配置的 {@code
 * tablePrefix}（默认 {@code KP_}）+ 按 {@code tableNameCase}（默认大写）渲染的逻辑名。
 *
 * <p>设计取向：只存引擎记录里真实存在的字段；展示用的冗余（例如实例行上的流程定义名称）交给查询侧 JOIN，换取更小的写放大。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class Tables {

  public static final TableSpec PROCESS_DEFINITION =
      new TableSpec(
          "process_definition",
          List.of(
              "process_definition_id",
              "process_definition_key",
              "process_definition_name",
              "version",
              "version_tag",
              "history_time_to_live",
              "deployment_id",
              "resource_definition_id",
              "resource_name",
              "checksum",
              "starter_events",
              "candidate_starter_groups",
              "candidate_starter_users",
              "resource_content",
              "resource_id"),
          List.of("process_definition_id"));

  public static final TableSpec PROCESS_INSTANCE =
      new TableSpec(
          "process_instance",
          List.of(
              "process_instance_id",
              "parent_process_instance_id",
              "root_process_instance_id",
              "business_key",
              "process_definition_id",
              "process_definition_key",
              "process_definition_name",
              "start_user_id",
              "state",
              "start_time",
              "end_time",
              "revision",
              "resource_id"),
          List.of("process_instance_id"));

  public static final TableSpec ACTIVITY_INSTANCE =
      new TableSpec(
          "activity_instance",
          List.of(
              "activity_instance_id",
              "process_instance_id",
              "root_process_instance_id",
              "parent_activity_instance_id",
              "call_process_instance_id",
              "process_definition_id",
              "process_definition_key",
              "activity_definition_key",
              "activity_definition_name",
              "activity_definition_type",
              "task_id",
              "assignee",
              "start_activity_definition_key",
              "start_activity_instance_id",
              "state",
              "incident_id",
              "sequence_counter",
              "start_time",
              "end_time",
              "duration",
              "revision",
              "resource_id"),
          List.of("activity_instance_id"));

  public static final TableSpec VARIABLE =
      new TableSpec(
          "variable",
          List.of(
              "scope_id",
              "name",
              "parent_scope_id",
              "process_instance_id",
              "process_definition_id",
              "value_json",
              "revision",
              "resource_id"),
          List.of("scope_id", "name"));

  public static final TableSpec USER_TASK =
      new TableSpec(
          "user_task",
          List.of(
              "task_id",
              "activity_instance_id",
              "process_instance_id",
              "business_key",
              "process_definition_id",
              "process_definition_key",
              "task_definition_key",
              "task_definition_name",
              "assignee",
              "owner",
              "priority",
              "due_date",
              "follow_up_date",
              "state",
              "candidate_groups",
              "candidate_users",
              "start_time",
              "end_time",
              "duration",
              "revision",
              "resource_id"),
          List.of("task_id"));

  public static final TableSpec JOB =
      new TableSpec(
          "job",
          List.of(
              "job_id",
              "job_type",
              "job_kind",
              "state",
              "retries",
              "retry_backoff",
              "priority",
              "due_date",
              "lock_owner",
              "lock_expire_time",
              "process_instance_id",
              "activity_instance_id",
              "process_definition_id",
              "process_definition_key",
              "activity_definition_key",
              "task_id",
              "incident_id",
              "denied_reason",
              "start_time",
              "end_time",
              "duration",
              "variables_json",
              "local_variables_json",
              "revision",
              "resource_id"),
          List.of("job_id"));

  public static final TableSpec INCIDENT =
      new TableSpec(
          "incident",
          List.of(
              "incident_id",
              "incident_type",
              "incident_message",
              "process_instance_id",
              "activity_instance_id",
              "process_definition_id",
              "process_definition_key",
              "activity_definition_key",
              "task_id",
              "job_id",
              "related_value_type",
              "related_lifecycle",
              "state",
              "created_time",
              "resolved_time",
              "resource_id"),
          List.of("incident_id"));

  public static final TableSpec TIMER =
      new TableSpec(
          "timer",
          List.of(
              "timer_id",
              "due_date",
              "repetitions",
              "process_instance_id",
              "activity_instance_id",
              "process_definition_id",
              "process_definition_key",
              "activity_definition_key",
              "timer_element_type",
              "timer_type",
              "timer_content",
              "state",
              "resource_id"),
          List.of("timer_id"));

  public static final TableSpec MESSAGE_SUBSCRIPTION =
      new TableSpec(
          "message_subscription",
          List.of(
              "message_subscription_id",
              "message_name",
              "message_type",
              "correlation_key",
              "process_instance_id",
              "activity_instance_id",
              "correlation_activity_instance_id",
              "correlation_process_instance_id",
              "process_definition_id",
              "process_definition_key",
              "activity_definition_key",
              "state",
              "resource_id"),
          List.of("message_subscription_id"));

  public static final TableSpec SIGNAL_SUBSCRIPTION =
      new TableSpec(
          "signal_subscription",
          List.of(
              "signal_subscription_id",
              "signal_name",
              "signal_type",
              "process_instance_id",
              "activity_instance_id",
              "process_definition_id",
              "process_definition_key",
              "activity_definition_key",
              "state",
              "resource_id"),
          List.of("signal_subscription_id"));

  private Tables() {}

  /**
   * @return 本 sink 写入的全部业务表（顺序即清除顺序，定义表在前）
   */
  public static List<TableSpec> all() {
    return List.of(
        PROCESS_DEFINITION,
        PROCESS_INSTANCE,
        ACTIVITY_INSTANCE,
        VARIABLE,
        USER_TASK,
        JOB,
        INCIDENT,
        TIMER,
        MESSAGE_SUBSCRIPTION,
        SIGNAL_SUBSCRIPTION);
  }
}
