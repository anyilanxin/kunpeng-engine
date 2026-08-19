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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.journal.util.health;

import java.util.Objects;

/** 组件健康报告（状态 + 问题） */
public final class HealthReport {

  public enum Status {
    HEALTHY,
    UNHEALTHY,
    DEAD
  }

  private final Object monitored;
  private final Status status;
  private final Throwable issue;

  private HealthReport(final Object monitored, final Status status, final Throwable issue) {
    this.monitored = monitored;
    this.status = status;
    this.issue = issue;
  }

  public static HealthReport healthy(final Object monitored) {
    return new HealthReport(monitored, Status.HEALTHY, null);
  }

  public static HealthReport unhealthy(final Object monitored) {
    return new HealthReport(monitored, Status.UNHEALTHY, null);
  }

  public static HealthReport dead(final Object monitored) {
    return new HealthReport(monitored, Status.DEAD, null);
  }

  /** 附加问题（返回新实例） */
  public HealthReport withIssue(final Throwable issue) {
    return new HealthReport(monitored, status, issue);
  }

  public Status getStatus() {
    return status;
  }

  public Throwable getIssue() {
    return issue;
  }

  public Object getMonitored() {
    return monitored;
  }

  public boolean isHealthy() {
    return status == Status.HEALTHY;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HealthReport that)) {
      return false;
    }
    return status == that.status && Objects.equals(issue, that.issue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, issue);
  }

  @Override
  public String toString() {
    return "HealthReport{status=" + status + (issue != null ? ", issue=" + issue + "}" : "}");
  }
}
