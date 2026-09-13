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
import com.anyilanxin.kunpeng.configuration.broker.backpressure.RateLimitCfg;
import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class FlowControlCfg implements ConfigurationEntry {

  private static final ObjectMapper MAPPER = JsonMapper.builder().build();
  private LimitCfg request = null;
  private RateLimitCfg write = null;

  public FlowControlCfg() {}

  public LimitCfg getRequest() {
    return request;
  }

  public void setRequest(final LimitCfg request) {
    this.request = request;
  }

  public RateLimitCfg getWrite() {
    return write;
  }

  public void setWrite(final RateLimitCfg write) {
    this.write = write;
  }

  @Override
  public int hashCode() {
    return Objects.hash(request, write);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final FlowControlCfg that)) {
      return false;
    }
    return Objects.equals(request, that.request) && Objects.equals(write, that.write);
  }

  public static FlowControlCfg deserialize(final String serialized) throws JacksonException {
    return MAPPER.readValue(serialized, FlowControlCfg.class);
  }

  public byte[] serialize() throws JacksonException {
    return MAPPER.writeValueAsBytes(this);
  }
}
