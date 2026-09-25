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
package com.anyilanxin.kunpeng.client.command.job.worker;

import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import java.util.function.Consumer;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
interface JobStreamer extends AutoCloseable {

  @Override
  void close();

  boolean isOpen();

  void openStreamer(final Consumer<ActivatedJob> jobConsumer);

  static JobStreamer noop() {
    return NoopJobStream.Singleton.INSTANCE.stream;
  }

  @ThreadSafe
  final class NoopJobStream implements JobStreamer {

    @Override
    public void close() {}

    @Override
    public boolean isOpen() {
      return false;
    }

    @Override
    public void openStreamer(final Consumer<ActivatedJob> jobConsumer) {}

    private enum Singleton {
      @SuppressWarnings("resource")
      INSTANCE(new NoopJobStream());

      private final NoopJobStream stream;

      Singleton(final NoopJobStream stream) {
        this.stream = stream;
      }
    }
  }
}
