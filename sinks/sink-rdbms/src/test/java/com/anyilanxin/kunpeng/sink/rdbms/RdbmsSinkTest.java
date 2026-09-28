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
package com.anyilanxin.kunpeng.sink.rdbms;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ProcessDefinitionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.historycleanup.HistoryCleanupLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.incident.IncidentLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.message.MessageSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalSubscriptionLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.variable.VariableLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RdbmsSinkTest {

  private SqlSessionFactory verifyFactory;
  private RdbmsSink sink;
  private Fakes.RecordingController controller;
  private String jdbcUrl;

  @BeforeEach
  void setUp() throws Exception {
    // DB_CLOSE_DELAY=-1：重启用例里首个 sink 关闭会使连接池归零关池，内存库需跨连接存活到用例结束
    jdbcUrl =
        "jdbc:h2:mem:sinkrdbms_"
            + UUID.randomUUID().toString().replace("-", "")
            + ";DB_CLOSE_DELAY=-1";
    final var args =
        Map.<String, Object>of(
            "url", jdbcUrl,
            "userName", "sa",
            "password", "",
            "flushInterval", 0L);
    sink = new RdbmsSink();
    sink.initialize(Fakes.context(args));
    controller = new Fakes.RecordingController();
    sink.start(controller);
    verifyFactory = verificationFactory(jdbcUrl);
  }

  @AfterEach
  void tearDown() {
    sink.close();
  }

  @Test
  void shouldPersistOneRowPerEntityType() {
    var position = 0L;
    position = emit(position, ValueType.PROCESS_DEFINITION, ProcessDefinitionLifeCycle.CREATED, new Fakes.ProcessDefinitionValue());
    position = emit(position, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    final var activity = new Fakes.ActivityInstanceValue();
    activity.startActivityDefinitionKey = "start-gateway";
    activity.startActivityInstanceId = 777;
    position = emit(position, ValueType.ACTIVITY, ActivityInstanceLifeCycle.ACTIVATED, activity);
    final var variables = new Fakes.VariableValue();
    variables.variables.put("amount", 10);
    variables.variables.put("currency", "EUR");
    position = emit(position, ValueType.VARIABLE, VariableLifeCycle.CREATED, variables);
    position = emit(position, ValueType.USER_TASK, UserTaskLifeCycle.CREATED, new Fakes.UserTaskValue());
    position = emit(position, ValueType.JOB, JobLifeCycle.CREATED, new Fakes.JobValue());
    position = emit(position, ValueType.INCIDENT, IncidentLifeCycle.CREATED, new Fakes.IncidentValue());
    position = emit(position, ValueType.TIMER, TimerLifeCycle.CREATED, new Fakes.TimerValue());
    position = emit(position, ValueType.MESSAGE_SUBSCRIPTION, MessageSubscriptionLifeCycle.CREATED, new Fakes.MessageSubscriptionValue());
    position = emit(position, ValueType.SIGNAL_SUBSCRIPTION, SignalSubscriptionLifeCycle.CREATED, new Fakes.SignalSubscriptionValue());

    sink.flush();

    // 10 类实体各 1 行，变量展开 2 行
    try (var session = verifyFactory.openSession()) {
      final var mapper = session.getMapper(VerificationMapper.class);
      assertThat(mapper.totalRows()).isEqualTo(11L);
      assertThat(mapper.instanceRow()).containsEntry("STATE", "ACTIVATED");
      assertThat(mapper.userTaskRow()).containsEntry("ASSIGNEE", "bob");
      // 活动实例起点信息（迁移/改单场景的起始来源）随创建事件落库
      assertThat(mapper.activityInstanceRow())
          .containsEntry("START_ACTIVITY_DEFINITION_KEY", "start-gateway")
          .containsEntry("START_ACTIVITY_INSTANCE_ID", 777L);
      assertThat(mapper.variableRows())
          .extracting(row -> row.get("VALUE_JSON"))
          .containsExactly("10", "\"EUR\"");
    }
    assertThat(controller.getPosition()).isEqualTo(position);
  }

  @Test
  void shouldCoalesceSameEntityWithinWindow() {
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    final var finished = new Fakes.ProcessInstanceValue();
    finished.state = ProcessInstanceState.COMPLETED;
    emit(2L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.COMPLETED, finished);

    sink.flush();

    // 同一窗口内两次事件收敛成一行，start_time 继承首事件的值
    try (var session = verifyFactory.openSession()) {
      final var mapper = session.getMapper(VerificationMapper.class);
      assertThat(mapper.totalRows()).isEqualTo(1L);
      assertThat(mapper.instanceRow())
          .containsEntry("STATE", "COMPLETED")
          .containsKey("START_TIME");
    }
    assertThat(controller.getPosition()).isEqualTo(2L);
  }

  @Test
  void shouldApplyStateChangeAcrossWindows() {
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    sink.flush();

    final var suspended = new Fakes.ProcessInstanceValue();
    suspended.state = ProcessInstanceState.SUSPENDED;
    emit(2L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.SUSPENDED, suspended);
    sink.flush();

    try (var session = verifyFactory.openSession()) {
      final var mapper = session.getMapper(VerificationMapper.class);
      assertThat(mapper.totalRows()).isOne();
      assertThat(mapper.instanceRow()).containsEntry("STATE", "SUSPENDED");
    }
  }

  @Test
  void shouldNotOverwriteCreateTimeColumnsOnUpdate() {
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    sink.flush();

    final var finished = new Fakes.ProcessInstanceValue();
    finished.state = ProcessInstanceState.COMPLETED;
    emit(2L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.COMPLETED, finished);
    sink.flush();

    // update 语句不触碰 start_time / start_user：终态更新后创建时间仍在、结束时间已写（H2 下同样成立）
    try (var session = verifyFactory.openSession()) {
      final var mapper = session.getMapper(VerificationMapper.class);
      assertThat(mapper.totalRows()).isOne();
      assertThat(mapper.instanceRow())
          .containsEntry("STATE", "COMPLETED")
          .containsKey("START_TIME")
          .containsKey("END_TIME");
    }
  }

  @Test
  void shouldRecoverFromRedeliveredCreateEvent() {
    final var value = new Fakes.ProcessInstanceValue();
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, value);
    sink.flush();

    // 模拟「已提交、未确认」后的重投递：同一创建事件原样再送（位置推进），批量 insert 撞主键后逐条降级恢复
    emit(2L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, value);
    sink.flush();

    try (var session = verifyFactory.openSession()) {
      final var mapper = session.getMapper(VerificationMapper.class);
      assertThat(mapper.totalRows()).isOne();
      assertThat(mapper.instanceRow()).containsKey("START_TIME");
    }
    assertThat(controller.getPosition()).isEqualTo(2L);
  }

  @Test
  void shouldDeleteInstanceDataOnHistoryCleanup() {
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    emit(2L, ValueType.ACTIVITY, ActivityInstanceLifeCycle.ACTIVATED, new Fakes.ActivityInstanceValue());
    final var variables = new Fakes.VariableValue();
    variables.variables.put("amount", 10);
    emit(3L, ValueType.VARIABLE, VariableLifeCycle.CREATED, variables);
    emit(4L, ValueType.USER_TASK, UserTaskLifeCycle.CREATED, new Fakes.UserTaskValue());
    emit(5L, ValueType.INCIDENT, IncidentLifeCycle.CREATED, new Fakes.IncidentValue());
    emit(6L, ValueType.TIMER, TimerLifeCycle.CREATED, new Fakes.TimerValue());
    emit(7L, ValueType.MESSAGE_SUBSCRIPTION, MessageSubscriptionLifeCycle.CREATED, new Fakes.MessageSubscriptionValue());
    emit(8L, ValueType.SIGNAL_SUBSCRIPTION, SignalSubscriptionLifeCycle.CREATED, new Fakes.SignalSubscriptionValue());
    sink.flush();

    emit(9L, ValueType.HISTORY_CLEANUP, HistoryCleanupLifeCycle.TRIGGERED, new Fakes.HistoryCleanupValue());
    sink.flush();

    try (var session = verifyFactory.openSession()) {
      assertThat(session.getMapper(VerificationMapper.class).totalRows()).isZero();
    }
    assertThat(controller.getPosition()).isEqualTo(9L);
  }

  @Test
  void shouldPurgeAllTables() {
    emit(1L, ValueType.PROCESS_DEFINITION, ProcessDefinitionLifeCycle.CREATED, new Fakes.ProcessDefinitionValue());
    emit(2L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    sink.flush();

    sink.purge();

    try (var session = verifyFactory.openSession()) {
      assertThat(session.getMapper(VerificationMapper.class).totalRows()).isZero();
    }
  }

  @Test
  void shouldAcknowledgeOnlyAfterFlush() {
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());

    // 未刷盘前不确认位置
    assertThat(controller.getPosition()).isEqualTo(-1L);

    sink.flush();
    assertThat(controller.getPosition()).isEqualTo(1L);
  }

  @Test
  void shouldInsertRowForSequenceFlowTaking() {
    // 连线实例的首事件是 TAKING（非 ACTIVATING），必须建行而不是走 update 静默丢行
    final var flow = new Fakes.ActivityInstanceValue();
    flow.activityInstanceId = 3002;
    flow.activityElementType = BpmnElementType.SEQUENCE_FLOW;
    flow.startActivityInstanceId = 3001;
    emit(1L, ValueType.ACTIVITY, ActivityInstanceLifeCycle.TAKING, flow);

    sink.flush();

    try (var session = verifyFactory.openSession()) {
      final var mapper = session.getMapper(VerificationMapper.class);
      assertThat(mapper.totalRows()).isOne();
      assertThat(mapper.activityInstanceRow())
          .containsEntry("ACTIVITY_TYPE", "SEQUENCE_FLOW")
          .containsEntry("START_ACTIVITY_INSTANCE_ID", 3001L);
    }
    assertThat(controller.getPosition()).isEqualTo(1L);
  }

  @Test
  void shouldWriteOwnPositionRowOnFlush() {
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    emit(2L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.COMPLETED, new Fakes.ProcessInstanceValue());

    sink.flush();

    // 位置行与数据同事务落库，行值即本窗确认点
    try (var session = verifyFactory.openSession()) {
      assertThat(session.getMapper(VerificationMapper.class).sinkPositionRow()).isEqualTo(2L);
    }
  }

  @Test
  void shouldResumeFromOwnPositionRowAfterRestart() {
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    sink.flush();

    // 重启：新实例、新引擎位置（-1），start() 从位置行对账并反推
    sink.close();
    final var args =
        Map.<String, Object>of(
            "url", jdbcUrl, "userName", "sa", "password", "", "flushInterval", 0L);
    sink = new RdbmsSink();
    sink.initialize(Fakes.context(args));
    controller = new Fakes.RecordingController();
    sink.start(controller);
    assertThat(controller.getPosition()).isEqualTo(1L);

    // 引擎侧位置丢失后的重放：同位置记录再送一遍，低于权威位置直接跳过，不产生重复行
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    sink.flush();
    try (var session = verifyFactory.openSession()) {
      assertThat(session.getMapper(VerificationMapper.class).totalRows()).isOne();
    }

    // 权威位置之后的记录正常落库
    final var finished = new Fakes.ProcessInstanceValue();
    finished.state = ProcessInstanceState.COMPLETED;
    emit(2L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.COMPLETED, finished);
    sink.flush();
    try (var session = verifyFactory.openSession()) {
      assertThat(session.getMapper(VerificationMapper.class).instanceRow())
          .containsEntry("STATE", "COMPLETED");
      assertThat(session.getMapper(VerificationMapper.class).sinkPositionRow()).isEqualTo(2L);
    }
    assertThat(controller.getPosition()).isEqualTo(2L);
  }

  @Test
  void shouldResetOwnPositionRowOnPurge() {
    emit(1L, ValueType.PROCESS_INSTANCE, ProcessInstanceLifeCycle.ACTIVATED, new Fakes.ProcessInstanceValue());
    sink.flush();

    sink.purge();

    // 集群级清理：数据与位置行一并清空并重建 -1 行，重导出从 0 开始
    try (var session = verifyFactory.openSession()) {
      final var mapper = session.getMapper(VerificationMapper.class);
      assertThat(mapper.totalRows()).isZero();
      assertThat(mapper.sinkPositionRow()).isEqualTo(-1L);
    }
  }

  private long emit(
      final long position,
      final ValueType valueType,
      final ValueLifeCycle lifecycle,
      final RecordValue value) {
    final var record =
        new Fakes.FakeRecord<>(position, 1L, 1_000L + position, 1, valueType, lifecycle, value);
    sink.sink(record);
    return position;
  }

  private static SqlSessionFactory verificationFactory(final String jdbcUrl) throws Exception {
    final var configuration = new Configuration();
    final var properties = new java.util.Properties();
    properties.setProperty("user", "sa");
    properties.setProperty("password", "");
    configuration.setEnvironment(
        new Environment(
            "verify",
            new JdbcTransactionFactory(),
            new UnpooledDataSource("org.h2.Driver", jdbcUrl, properties)));
    configuration.addMapper(VerificationMapper.class);
    try (var input = Resources.getResourceAsStream("mapper/test/Verify.xml")) {
      new XMLMapperBuilder(input, configuration, "mapper/test/Verify.xml", configuration.getSqlFragments())
          .parse();
    }
    return new SqlSessionFactoryBuilder().build(configuration);
  }
}
