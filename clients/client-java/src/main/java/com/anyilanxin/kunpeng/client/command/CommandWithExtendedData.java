/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.client.command;

import java.util.Map;

/**
 * 部署资源业务扩展数据注入接口：添加逻辑与变量一致（可整体覆盖、可逐键追加、可多次调用累加）， 但数据语义是资源级业务扩展数据（分类/归属标注等），
 * 落在具体资源上并随部署落到流程定义、决策定义等记录。
 *
 * @param <T> builder 类型
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface CommandWithExtendedData<T> {

  /**
   * 设置业务扩展数据（覆盖最近添加资源的既有扩展数据）。
   *
   * @param extendedData 业务扩展数据
   * @return the builder for this command. Call {@code send()} to complete the command and send it
   *     to the broker.
   */
  T extendedData(Map<String, String> extendedData);

  /**
   * 追加单键业务扩展数据到最近添加的资源。
   *
   * @param key the key of the extended data as string
   * @param value the value of the extended data as string
   * @return the builder for this command. Call {@code send()} to complete the command and send it
   *     to the broker.
   */
  T extendedData(String key, String value);
}
