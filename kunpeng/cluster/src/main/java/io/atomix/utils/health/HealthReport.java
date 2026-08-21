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
package io.atomix.utils.health;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Immutable report describing the health of a component at a point in time. */
public final class HealthReport {

  private final Status status;
  private final String monitored;
  private final List<Issue> issues;

  private HealthReport(final Status status, final String monitored, final List<Issue> issues) {
    this.status = status;
    this.monitored = monitored;
    this.issues = List.copyOf(issues);
  }

  /** @return a healthy report for the given component */
  public static HealthReport healthy(final Object monitored) {
    return new HealthReport(Status.HEALTHY, nameOf(monitored), List.of());
  }

  /** @return an unhealthy report for the given component */
  public static HealthReport unhealthy(final Object monitored) {
    return new HealthReport(Status.UNHEALTHY, nameOf(monitored), List.of());
  }

  /** @return a dead report for the given component; the component is considered unrecoverable */
  public static HealthReport dead(final Object monitored) {
    return new HealthReport(Status.DEAD, nameOf(monitored), List.of());
  }

  private static String nameOf(final Object monitored) {
    return monitored instanceof final Class<?> clazz ? clazz.getName() : String.valueOf(monitored);
  }

  /** @return a copy of this report with an additional issue */
  public HealthReport withIssue(final Throwable throwable, final Instant occurredAt) {
    final var issues = new ArrayList<>(this.issues);
    issues.add(new Issue(throwable, occurredAt));
    return new HealthReport(status, monitored, issues);
  }

  /** @return a copy of this report with a different component name */
  public HealthReport withName(final String name) {
    return new HealthReport(status, name, issues);
  }

  public boolean isHealthy() {
    return status == Status.HEALTHY;
  }

  public Status getStatus() {
    return status;
  }

  public String getMonitored() {
    return monitored;
  }

  public List<Issue> getIssues() {
    return issues;
  }

  public Optional<Issue> firstIssue() {
    return issues.stream().findFirst();
  }

  @Override
  public String toString() {
    return "HealthReport[status=%s, monitored=%s, issues=%s]"
        .formatted(status, monitored, issues);
  }

  /** Severity of a report. */
  public enum Status {
    HEALTHY,
    UNHEALTHY,
    DEAD
  }

  /** A single failure captured in a report. */
  public record Issue(Throwable throwable, Instant occurredAt) {

    @Override
    public String toString() {
      return "%s (at %s): %s"
          .formatted(throwable.getClass().getName(), occurredAt, throwable.getMessage());
    }

    public static String joinIssues(final List<Issue> issues) {
      return issues.stream().map(Issue::toString).collect(Collectors.joining("; "));
    }
  }
}
