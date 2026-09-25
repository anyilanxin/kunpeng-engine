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
package com.anyilanxin.kunpeng.engine.bpmn;

import java.util.function.Supplier;

/**
 * 批处理收集器供给器：为每次处理批次提供收集器实例。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ProcessingCollectSupplier implements Supplier<BatchProcessingCollect> {

  private BatchProcessingCollect collect;

  public void setCollect(final BatchProcessingCollect collect) {
    this.collect = collect;
  }

  @Override
  public BatchProcessingCollect get() {
    return collect;
  }
}
