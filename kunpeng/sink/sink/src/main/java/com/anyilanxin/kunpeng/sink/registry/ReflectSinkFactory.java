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

import com.anyilanxin.kunpeng.sink.SinkLoggers;
import com.anyilanxin.kunpeng.sink.api.RecordSink;
import com.anyilanxin.kunpeng.utils.ReflectUtil;
import org.slf4j.Logger;

/**
 * 默认的 {@link SinkFactory}：一个 Sink 类，通过反射实例化。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class ReflectSinkFactory implements SinkFactory {

  private static final Logger LOG = SinkLoggers.SINK;

  private final String id;
  private final Class<? extends RecordSink> sinkClass;

  ReflectSinkFactory(final String id, final Class<? extends RecordSink> sinkClass) {
    this.id = id;
    this.sinkClass = sinkClass;
  }

  @Override
  public String sinkId() {
    return id;
  }

  @Override
  public RecordSink newInstance() throws SinkInstantiationException {
    LOG.debug("Creating sink '{}' instance of {}", id, sinkClass.getName());
    try {
      return ReflectUtil.newInstance(sinkClass);
    } catch (final Exception e) {
      throw new SinkInstantiationException(id, e);
    }
  }

  @Override
  public boolean producesSameType(final SinkFactory other) {
    return other instanceof final ReflectSinkFactory that && that.sinkClass.equals(sinkClass);
  }
}
