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
package com.anyilanxin.kunpeng.sink.registry;

import com.anyilanxin.kunpeng.sink.api.RecordSink;

/**
 * {@link RecordSink} 实例的来源。
 *
 * <p>默认来源 {@link ReflectSinkFactory} 只是调用无参构造器。自定义工厂适合需要依赖注入 或按分区维护状态的 Sink 。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface SinkFactory {

  /**
   * @return 本工厂产出的 Sink id
   */
  String sinkId();

  /**
   * @return 一个全新的、尚未配置的 Sink 实例
   */
  RecordSink newInstance() throws SinkInstantiationException;

  /**
   * @param other 待比较的工厂
   * @return 两个工厂是否产出同一类型的 Sink ；用于判断一个 Sink 能否从另一个的元数据初始化状态
   */
  boolean producesSameType(SinkFactory other);
}
