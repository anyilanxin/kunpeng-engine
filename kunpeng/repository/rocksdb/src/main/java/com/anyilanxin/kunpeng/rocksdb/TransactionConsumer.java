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
package com.anyilanxin.kunpeng.rocksdb;

import org.rocksdb.Transaction;

/** 事务消费者函数接口，用于消费事务以确保其处于打开状态 */
@FunctionalInterface
interface TransactionConsumer {

  /**
   * 消费一个事务，以确保事务处于打开状态
   *
   * @param transaction 被消费的事务
   * @throws Exception 发生意外异常时抛出，例如打开新事务失败时
   */
  void run(Transaction transaction) throws Exception;
}
