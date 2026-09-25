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
package com.anyilanxin.kunpeng.configuration.broker;

/**
 * BPMN 引擎相关配置。后续引擎可调参数统一放到这里。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class EngineCfg implements ConfigurationEntry {

  /**
   * 单次事件处理批次中 follow-up command/state 的最大数量。达到上限后当前批次会被强制提交到 raft，
   * 剩余命令留到下个批次。值越大吞吐越高但单批次内存占用和延迟也越高。
   */
  public static final int DEFAULT_MAX_BATCH = 200;

  private int maxBatch = DEFAULT_MAX_BATCH;

  public int getMaxBatch() {
    return maxBatch;
  }

  public void setMaxBatch(final int maxBatch) {
    this.maxBatch = maxBatch;
  }

  @Override
  public String toString() {
    return "EngineCfg{maxBatch=" + maxBatch + '}';
  }
}
