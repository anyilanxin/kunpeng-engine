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
package com.anyilanxin.core.config;

import io.netty.util.HashedWheelTimer;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zxuanhong
 * @date 2026-04-23 12:07
 * @since
 */
@Configuration
public class NettyWheelTimerConfig {

  @Bean(destroyMethod = "stop")
  public HashedWheelTimer hashedWheelTimer() {
    final HashedWheelTimer hashedWheelTimer =
        new HashedWheelTimer(Executors.defaultThreadFactory(), 100, TimeUnit.MILLISECONDS, 1024);
    hashedWheelTimer.start();
    return hashedWheelTimer;
  }
}
