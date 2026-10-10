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

import com.anyilanxin.kunpeng.sink.rdbms.RdbmsSinkSettings;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * 一个 sink 实例持有的数据库句柄：共享连接池 + 按方言装配的 MyBatis 会话工厂 + 迁移入口。
 *
 * <p>会话工厂使用批处理执行器（{@link ExecutorType#BATCH}），映射层的每次调用只入队语句， 真正的批量网络往返发生在 {@link
 * SqlSession#commit()} 时。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class Database implements AutoCloseable {

  private final ConnectionPool.PooledDataSource pool;
  private final Dialect dialect;
  private final SqlSessionFactory sessionFactory;

  private Database(
      final ConnectionPool.PooledDataSource pool,
      final Dialect dialect,
      final SqlSessionFactory sessionFactory) {
    this.pool = pool;
    this.dialect = dialect;
    this.sessionFactory = sessionFactory;
  }

  /**
   * 打开（或复用）指向目标库的句柄。
   *
   * @param settings sink 配置
   * @return 数据库句柄
   */
  public static Database open(final RdbmsSinkSettings settings) {
    final var dialect = settings.resolvedDialect();
    final var pool =
        ConnectionPool.acquire(
            settings.getUrl(),
            settings.getUserName(),
            settings.getPassword(),
            settings.getMaxPoolSize());
    final var sessionFactory = buildSessionFactory(pool, settings);
    return new Database(pool, dialect, sessionFactory);
  }

  private static SqlSessionFactory buildSessionFactory(
      final ConnectionPool.PooledDataSource pool, final RdbmsSinkSettings settings) {
    final var configuration = new Configuration();
    // 标识符变量值=完整物理名（前缀 + 大小写渲染）：mapper XML 里的 ${table.x} 即整个表名
    TableName.registerAll(
        configuration.getVariables()::setProperty,
        settings.getTablePrefix(),
        settings.resolvedTableNameCase());
    configuration.setEnvironment(
        new Environment("kunpeng-sink-rdbms", new JdbcTransactionFactory(), pool.dataSource()));
    // 方言识别：MyBatis 的 databaseId 机制，产品名 -> id 的映射来自 db/vendor-properties/*.properties，
    // XML 里同一语句 id 按 databaseId 取匹配版本
    final var databaseIdProvider = new org.apache.ibatis.mapping.VendorDatabaseIdProvider();
    databaseIdProvider.setProperties(VendorDatabaseIds.load());
    try {
      configuration.setDatabaseId(databaseIdProvider.getDatabaseId(pool.dataSource()));
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to detect database id for rdbms sink", e);
    }
    // 批处理执行器：映射调用累积为 statement batch，commit 时统一发送
    configuration.setDefaultExecutorType(ExecutorType.BATCH);
    // 各库对 setNull(OTHER) 兼容性差，统一按 NULL 类型绑定空值
    configuration.setJdbcTypeForNull(JdbcType.NULL);

    // 每个实体一份接口 + XML（约定路径 mapper/<简单名>.xml）
    for (final var mapperInterface : SinkMappers.mapperInterfaces()) {
      configuration.addMapper(mapperInterface);
      parseMapper(configuration, SinkMappers.mapperResource(mapperInterface));
    }

    return new SqlSessionFactoryBuilder().build(configuration);
  }

  private static void parseMapper(final Configuration configuration, final String resource) {
    try (final var input = Resources.getResourceAsStream(resource)) {
      new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to load mapper resource " + resource, e);
    }
  }

  /**
   * @return 目标库方言
   */
  public Dialect dialect() {
    return dialect;
  }

  /** 打开一个手动提交的批处理会话；调用方负责提交/回滚与关闭。 */
  public SqlSession openSession() {
    return sessionFactory.openSession(false);
  }

  /** 执行建表迁移（幂等；Liquibase 负责版本登记与并发锁）。 */
  public void migrate(final RdbmsSinkSettings settings) {
    try (final var connection = pool.dataSource().getConnection()) {
      SchemaMigration.run(connection, settings);
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to acquire connection for schema migration", e);
    }
  }

  /** 清空本 sink 管理的全部表（集群级清理；位置行一并清空——集群清理时引擎侧位置同样重置，重导出从 0 开始）。 */
  public void purge() {
    try (final var session = openSession()) {
      final var mappers = SinkMappers.from(session);
      mappers.processDefinition().clearAll();
      mappers.processInstance().clearAll();
      mappers.activityInstance().clearAll();
      mappers.variable().clearAll();
      mappers.userTask().clearAll();
      mappers.job().clearAll();
      mappers.incident().clearAll();
      mappers.timer().clearAll();
      mappers.messageSubscription().clearAll();
      mappers.signalSubscription().clearAll();
      mappers.sinkPosition().clearAll();
      session.commit();
    }
  }

  /** 删除仅属于某个分区（resource）的业务数据（分区从集群移除时）。 */
  public void removePartitionData(final int resourceId) {
    try (final var session = openSession()) {
      final var mappers = SinkMappers.from(session);
      mappers.processDefinition().removePartition(resourceId);
      mappers.processInstance().removePartition(resourceId);
      mappers.activityInstance().removePartition(resourceId);
      mappers.variable().removePartition(resourceId);
      mappers.userTask().removePartition(resourceId);
      mappers.job().removePartition(resourceId);
      mappers.incident().removePartition(resourceId);
      mappers.timer().removePartition(resourceId);
      mappers.messageSubscription().removePartition(resourceId);
      mappers.signalSubscription().removePartition(resourceId);
      mappers.sinkPosition().removePartition(resourceId);
      session.commit();
    }
  }

  /** 释放本句柄对共享连接池的引用。 */
  @Override
  public void close() {
    pool.release();
  }
}
