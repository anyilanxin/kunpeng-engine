/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Free Software Foundation as either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import com.anyilanxin.kunpeng.sink.rdbms.RdbmsSinkSettings.TableNameCase;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * 数据库标识符登记枚举：每个常量声明 {@code 变量 key + 表名}（含 changelog 索引名与 Liquibase 簿记表）， 由 {@link
 * #registerAll(BiConsumer, String, TableNameCase)} 统一算出完整物理名（前缀 + 大小写渲染）后注入 MyBatis / Liquibase
 * 变量—— XML 里的标识符一律只写 {@code ${<变量key>}}，一个变量即完整表名，不存在前缀拼装。
 *
 * <p>新增表或索引时在此追加常量即可，变量注册零改动。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum TableName {
  // 业务表
  PROCESS_DEFINITION("table.process_definition", "process_definition"),
  PROCESS_INSTANCE("table.process_instance", "process_instance"),
  ACTIVITY_INSTANCE("table.activity_instance", "activity_instance"),
  VARIABLE("table.variable", "variable"),
  USER_TASK("table.user_task", "user_task"),
  JOB("table.job", "job"),
  INCIDENT("table.incident", "incident"),
  TIMER("table.timer", "timer"),
  MESSAGE_SUBSCRIPTION("table.message_subscription", "message_subscription"),
  SIGNAL_SUBSCRIPTION("table.signal_subscription", "signal_subscription"),
  // sink 自身的导出位置表（不走 RowChange 路由，由 RdbmsSink 直接读写）
  SINK_POSITION("table.sink_position", "sink_position"),
  // changelog 索引（逻辑名须与 changesets 保持同步）
  IDX_PD_KEY("table.idx_pd_key", "idx_pd_key"),
  IDX_PI_ROOT("table.idx_pi_root", "idx_pi_root"),
  IDX_PI_PARENT("table.idx_pi_parent", "idx_pi_parent"),
  IDX_PI_DEFINITION("table.idx_pi_definition", "idx_pi_definition"),
  IDX_AI_INSTANCE("table.idx_ai_instance", "idx_ai_instance"),
  IDX_AI_TASK("table.idx_ai_task", "idx_ai_task"),
  IDX_VAR_INSTANCE("table.idx_var_instance", "idx_var_instance"),
  IDX_UT_INSTANCE("table.idx_ut_instance", "idx_ut_instance"),
  IDX_UT_ASSIGNEE("table.idx_ut_assignee", "idx_ut_assignee"),
  IDX_JOB_INSTANCE("table.idx_job_instance", "idx_job_instance"),
  IDX_JOB_STATE_DUE("table.idx_job_state_due", "idx_job_state_due"),
  IDX_INC_INSTANCE("table.idx_inc_instance", "idx_inc_instance"),
  IDX_TIMER_INSTANCE("table.idx_timer_instance", "idx_timer_instance"),
  IDX_TIMER_DUE("table.idx_timer_due", "idx_timer_due"),
  IDX_MS_INSTANCE("table.idx_ms_instance", "idx_ms_instance"),
  IDX_MS_NAME("table.idx_ms_name", "idx_ms_name"),
  IDX_SS_INSTANCE("table.idx_ss_instance", "idx_ss_instance"),
  IDX_SS_NAME("table.idx_ss_name", "idx_ss_name"),
  // Liquibase 簿记表（不进 changelog，名称经 SchemaMigration 设置到 liquibase Database）
  DATABASECHANGELOG("table.databasechangelog", "databasechangelog"),
  DATABASECHANGELOGLOCK("table.databasechangeloglock", "databasechangeloglock");

  private final String variableKey;
  private final String logicalName;

  TableName(final String variableKey, final String logicalName) {
    this.variableKey = variableKey;
    this.logicalName = logicalName;
  }

  /** XML 里 {@code ${...}} 引用使用的完整变量 key */
  public String variableKey() {
    return variableKey;
  }

  /** 不带前缀的逻辑表名/索引名 */
  public String logicalName() {
    return logicalName;
  }

  /** 物理名 = 前缀 + 按大小写策略渲染的逻辑名 */
  public String physicalName(final String prefix, final TableNameCase nameCase) {
    return prefix + rendered(nameCase);
  }

  /** 按大小写策略渲染的逻辑名 */
  public String rendered(final TableNameCase nameCase) {
    return switch (nameCase) {
      case UPPER -> logicalName.toUpperCase(Locale.ROOT);
      case LOWER -> logicalName.toLowerCase(Locale.ROOT);
    };
  }

  /** 把全部标识符的完整物理名注册为变量：setter 接收 (变量key, 物理名)，适配 MyBatis 与 Liquibase 的参数容器。 */
  public static void registerAll(
      final BiConsumer<String, String> setter, final String prefix, final TableNameCase nameCase) {
    for (final TableName name : values()) {
      setter.accept(name.variableKey, name.physicalName(prefix, nameCase));
    }
  }
}
