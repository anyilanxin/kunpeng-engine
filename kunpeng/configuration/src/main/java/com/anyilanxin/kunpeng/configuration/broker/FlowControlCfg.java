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

import com.anyilanxin.kunpeng.configuration.broker.backpressure.LimitCfg;
import com.anyilanxin.kunpeng.configuration.broker.backpressure.SinkBackpressureCfg;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 流控配置。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class FlowControlCfg implements ConfigurationEntry {

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();
  // 两个控制点统一语义：均有 enabled 开关且默认 false（未配置即全关，逐项显式开启）。
  // 上游 FlowControlCfg.write（写侧速率限流）已删除：自研 eventlog 无此机制，导出滞后限写的职能由 sink 闸门承接。
  private LimitCfg request = new LimitCfg();
  private SinkBackpressureCfg sink = new SinkBackpressureCfg();

  public FlowControlCfg() {}

  public LimitCfg getRequest() {
    return request;
  }

  public void setRequest(final LimitCfg request) {
    this.request = request;
  }

  public SinkBackpressureCfg getSink() {
    return sink;
  }

  public void setSink(final SinkBackpressureCfg sink) {
    this.sink = sink;
  }

  @Override
  public int hashCode() {
    return Objects.hash(request, sink);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final FlowControlCfg that)) {
      return false;
    }
    return Objects.equals(request, that.request) && Objects.equals(sink, that.sink);
  }

  public static FlowControlCfg deserialize(final String serialized) throws JacksonException {
    return MAPPER.readValue(serialized, FlowControlCfg.class);
  }

  public byte[] serialize() throws JacksonException {
    return MAPPER.writeValueAsBytes(this);
  }
}
