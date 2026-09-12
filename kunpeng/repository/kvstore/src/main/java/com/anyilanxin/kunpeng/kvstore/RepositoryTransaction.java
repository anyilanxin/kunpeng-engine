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

/** 表示一个可提交、出错时可回滚的数据库事务。 */
public interface RepositoryTransaction {

  /**
   * 在当前事务中执行 delete、put 等命令，事务内可以访问不同的列族。
   *
   * <p>也可以通过 get 或 iterator 读取 key-value 对，读取结果会反映事务过程中所做的变更。
   *
   * @param operations 要执行的操作
   * @throws RuntimeException 执行操作过程中发生意外错误时抛出
   */
  void run(TransactionOperation operations) throws Exception;

  /**
   * 提交事务并将数据写入数据库。
   *
   * @throws Exception 底层数据库抛出不可恢复异常时抛出
   */
  void commit() throws Exception;

  /**
   * 将事务回滚到最近一次提交，丢弃其后的所有变更。
   *
   * @throws Exception 底层数据库抛出不可恢复异常时抛出
   */
  void rollback() throws Exception;
}
