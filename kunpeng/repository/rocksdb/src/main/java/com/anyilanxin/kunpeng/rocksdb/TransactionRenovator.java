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

/** 事务续借函数接口，用于将旧事务续借为可复用的新事务 */
@FunctionalInterface
public interface TransactionRenovator {

  /**
   * 续借给定的旧事务，使其可被复用
   *
   * @param oldTransaction 需要续借的旧事务
   * @return 续借后的新事务
   */
  Transaction renewTransaction(Transaction oldTransaction);
}
