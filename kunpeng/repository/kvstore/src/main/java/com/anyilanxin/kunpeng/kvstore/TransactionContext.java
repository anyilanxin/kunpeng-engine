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
package com.anyilanxin.kunpeng.kvstore;

/** 表示在事务中与数据库交互的事务上下文 */
public interface TransactionContext {

  /**
   * 在事务中执行删除、写入等命令，事务内可以访问不同的 column family。
   *
   * <p>也可以通过 get 或迭代器读取 key-value，读取结果会反映事务期间发生的变更。
   *
   * <p><b>注意</b>：允许嵌套调用，嵌套时事务会被复用（运行在同一事务中）。最外层调用结束时提交 事务，出错时回滚事务。
   *
   * @param operations 要执行的操作
   * @throws RuntimeException 执行操作发生意外错误时抛出
   */
  void runInTransaction(TransactionOperation operations);

  /**
   * 返回一个调用方可直接操作的事务对象，由调用方自行决定何时提交或回滚
   *
   * @return 事务对象
   */
  RepositoryTransaction getCurrentTransaction();
}
