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
package com.anyilanxin.kunpeng.client;

import com.anyilanxin.kunpeng.client.command.deployment.DeployResourceCommand;
import com.anyilanxin.kunpeng.client.command.deployment.DeployResourceCommandResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ClientTest {

  static void main() {
    final KunpengClient kunpengClient = KunpengClient
      .newClientBuilder()
      .grpcAddress(URI.create("http://localhost:2027"))
      .usePlaintext()
      .build();
    try (final InputStream resourceAsStream =
           ClientTest.class.getResourceAsStream("/model/diagram_12.bpmn")) {
      final byte[] bytes = resourceAsStream.readAllBytes();
      final DeployResourceCommand.DeployResourceCommandStep2 deployResourceCommandStep2 = kunpengClient.newDeployResourceCommand().addResourceBytes(bytes, "sdfsdf.bpmn");
      final DeployResourceCommandResponse join = deployResourceCommandStep2.send().join();
      System.out.println(join);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

  }
}
