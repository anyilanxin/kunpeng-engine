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
package com.anyilanxin.kunpeng.sink.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Map sink 配置测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class MapSinkConfigTest {

  public static final class SampleSettings {
    private String endpoint;
    private Map<String, Object> extra = Map.of();
    private List<String> indexPrefixes = List.of();
    private Flush flush = new Flush();

    public String getEndpoint() {
      return endpoint;
    }

    public Map<String, Object> getExtra() {
      return extra;
    }

    public List<String> getIndexPrefixes() {
      return indexPrefixes;
    }

    public Flush getFlush() {
      return flush;
    }

    void setEndpoint(final String endpoint) {
      this.endpoint = endpoint;
    }

    void setExtra(final Map<String, Object> extra) {
      this.extra = extra;
    }

    void setIndexPrefixes(final List<String> indexPrefixes) {
      this.indexPrefixes = indexPrefixes;
    }

    void setFlush(final Flush flush) {
      this.flush = flush;
    }
  }

  public static final class Flush {
    private Interval interval = Interval.FAST;

    public Interval getInterval() {
      return interval;
    }

    void setInterval(final Interval interval) {
      this.interval = interval;
    }
  }

  enum Interval {
    FAST,
    SLOW
  }

  @Test
  void bindsArgumentsLeniently() {
    // 假设
    final var config =
        new MapSinkConfig(
            "test",
            Map.of(
                "ENDPOINT", "http://localhost:9200",
                "flush", Map.of("interval", "slow"), // 枚举值大小写不敏感
                "extra", Map.of("batch", 5)));

    // 当
    final var settings = config.createSettings(SampleSettings.class);

    // 则
    assertThat(settings.getEndpoint()).isEqualTo("http://localhost:9200");
    assertThat(settings.getFlush().getInterval()).isEqualTo(Interval.SLOW);
    assertThat(settings.getExtra()).containsEntry("batch", 5);
  }

  @Test
  void convertsNumericKeyMapsIntoLists() {
    // 假设
    final var config =
        new MapSinkConfig("test", Map.of("indexPrefixes", Map.of("1", "second", "0", "first")));

    // 当
    final var settings = config.createSettings(SampleSettings.class);

    // 则
    assertThat(settings.getIndexPrefixes()).containsExactly("first", "second");
  }

  @Test
  void ignoresUnknownProperties() {
    // 假设
    final var config =
        new MapSinkConfig("test", Map.of("endpoint", "x", "totallyUnknown", 1));

    // 当
    final var settings = config.createSettings(SampleSettings.class);

    // 则
    assertThat(settings.getEndpoint()).isEqualTo("x");
  }

  @Test
  void emptyArgumentsYieldDefaults() {
    // 假设
    final var config = new MapSinkConfig("test", null);

    // 当
    final var settings = config.createSettings(SampleSettings.class);

    // 则
    assertThat(settings.getEndpoint()).isNull();
    assertThat(settings.getFlush().getInterval()).isEqualTo(Interval.FAST);
    assertThat(config.getArguments()).isEmpty();
  }
}
