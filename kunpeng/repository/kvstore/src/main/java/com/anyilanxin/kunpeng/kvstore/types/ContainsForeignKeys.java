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
package com.anyilanxin.kunpeng.kvstore.types;

import java.util.Collection;

/** 表示对象中包含外键（{@link ForeignKeyType}），供一致性校验时收集引用关系 */
public interface ContainsForeignKeys {

  /**
   * 返回当前对象包含的所有外键集合
   *
   * @return 外键集合，不含外键时返回空集合
   */
  Collection<ForeignKeyType<KeyType>> containedForeignKeys();
}
