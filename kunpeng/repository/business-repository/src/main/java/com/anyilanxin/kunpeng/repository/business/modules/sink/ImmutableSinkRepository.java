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
package com.anyilanxin.kunpeng.repository.business.modules.sink;

import com.anyilanxin.kunpeng.repository.business.ResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.sink.record.SinkStateEntry;
import java.util.function.BiConsumer;
import org.agrona.DirectBuffer;

/**
 * sink 域只读仓储接口：sink 位置与状态查询。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ImmutableSinkRepository extends ResourceDataSplit {
  long VALUE_NOT_FOUND = -1;

  long getSinkPosition(String sinkId);

  DirectBuffer getSinkMetadata(final String sinkId);

  long getLowestPosition();

  long getMetadataVersion(final String sinkId);

  void visitSinkState(final BiConsumer<String, SinkStateEntry> consumer);

  boolean hasSinks();
}
