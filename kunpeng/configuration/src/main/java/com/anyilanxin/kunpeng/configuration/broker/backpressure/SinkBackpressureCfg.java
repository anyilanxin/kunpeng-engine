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
package com.anyilanxin.kunpeng.configuration.broker.backpressure;

import com.anyilanxin.kunpeng.configuration.broker.ConfigurationEntry;
import java.util.Objects;

/**
 * sink 滞后背压闸门配置：当分区事件日志写入位与全部 sink 最低确认位的差值（lag）越过 {@code rejectBacklog} 时拒绝业务面 USER_COMMAND 准入，回落到
 * {@code resumeBacklog} 才恢复放行（双阈值迟滞防边界抖动）。
 *
 * <p>{@code enabled} 与 request/write 控制点统一默认 {@code false}（未配置即全关，显式开启）； {@code false}
 * 等价于闸门不存在（不是放宽阈值）；未接入任何 sink 时闸门运行时恒放行，与本配置无关。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class SinkBackpressureCfg implements ConfigurationEntry {

  private static final long DEFAULT_REJECT_BACKLOG = 50_000L;
  private static final long DEFAULT_RESUME_BACKLOG = 40_000L;

  private boolean enabled = false;
  private long rejectBacklog = DEFAULT_REJECT_BACKLOG;
  private long resumeBacklog = DEFAULT_RESUME_BACKLOG;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  /** 拒绝阈值：lag ≥ rejectBacklog 转拒绝 */
  public long getRejectBacklog() {
    return rejectBacklog;
  }

  public void setRejectBacklog(final long rejectBacklog) {
    this.rejectBacklog = rejectBacklog;
  }

  /** 恢复阈值：拒绝状态下 lag ≤ resumeBacklog 才重开，必须小于 rejectBacklog */
  public long getResumeBacklog() {
    return resumeBacklog;
  }

  public void setResumeBacklog(final long resumeBacklog) {
    this.resumeBacklog = resumeBacklog;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final SinkBackpressureCfg that)) {
      return false;
    }
    return enabled == that.enabled
        && rejectBacklog == that.rejectBacklog
        && resumeBacklog == that.resumeBacklog;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, rejectBacklog, resumeBacklog);
  }

  @Override
  public String toString() {
    return "SinkBackpressureCfg{"
        + "enabled="
        + enabled
        + ", rejectBacklog="
        + rejectBacklog
        + ", resumeBacklog="
        + resumeBacklog
        + '}';
  }
}
