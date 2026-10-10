/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.core;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zxuanhong
 * @date 2026-04-23 12:23
 * @since
 */
public abstract class TimerTaskInfo implements TimerTask, AutoCloseable {
  private final HashedWheelTimer wheelTimer;
  private final long delay;
  private final TimeUnit unit;
  private final AtomicBoolean state = new AtomicBoolean(false);
  private final AtomicBoolean cycleStart = new AtomicBoolean(false);

  public TimerTaskInfo(final HashedWheelTimer wheelTimer, final long delay, final TimeUnit unit) {
    this.wheelTimer = wheelTimer;
    this.delay = delay;
    this.unit = unit;
  }

  public void start() {
    wheelTimer.newTimeout(this, delay, unit);
    state.set(true);
  }

  public void cycleStart() {
    cycleStart.set(true);
    start();
  }

  @Override
  public void run(final Timeout timeout) {
    if (state.get()) {
      process();
      if (cycleStart.get()) {
        wheelTimer.newTimeout(this, delay, unit);
      }
    }
  }

  @Override
  public void close() {
    state.set(false);
  }

  public abstract void process();
}
