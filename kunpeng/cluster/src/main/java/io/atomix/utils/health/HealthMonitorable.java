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

/** A component whose health can be monitored. */
public interface HealthMonitorable {

  /** @return the name under which this component is reported in health checks */
  default String componentName() {
    return getClass().getSimpleName();
  }

  /** @return the current health report of this component */
  HealthReport getHealthReport();

  /**
   * Registers a listener notified when this component fails.
   *
   * @param listener the listener to register
   */
  void addFailureListener(FailureListener listener);

  /**
   * Removes a previously registered failure listener.
   *
   * @param listener the listener to remove
   */
  void removeFailureListener(FailureListener listener);
}
