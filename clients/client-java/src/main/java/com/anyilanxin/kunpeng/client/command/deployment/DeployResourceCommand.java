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
package com.anyilanxin.kunpeng.client.command.deployment;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import java.io.InputStream;
import java.nio.charset.Charset;

public interface DeployResourceCommand {

  /**
   * Add the given resource to the deployment.
   *
   * @param resourceBytes the resource content as byte array
   * @param resourceName the name of the resource (e.g. "process.bpmn" or "decision.dmn")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  DeployResourceCommandStep2 addResourceBytes(byte[] resourceBytes, String resourceName);

  /**
   * Add the given resource to the deployment.
   *
   * @param resourceString the resource content as String
   * @param charset the charset of the String
   * @param resourceName the name of the resource (e.g. "process.bpmn" or "decision.dmn")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  DeployResourceCommandStep2 addResourceString(
      String resourceString, Charset charset, String resourceName);

  /**
   * Add the given resource to the deployment.
   *
   * @param resourceString the resource content as UTF-8-encoded String
   * @param resourceName the name of the resource (e.g. "process.bpmn" or "decision.dmn")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  DeployResourceCommandStep2 addResourceStringUtf8(String resourceString, String resourceName);

  /**
   * Add the given resource to the deployment.
   *
   * @param resourceStream the resource content as stream
   * @param resourceName the name of the resource (e.g. "process.bpmn" or "decision.dmn")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  DeployResourceCommandStep2 addResourceStream(InputStream resourceStream, String resourceName);

  /**
   * Add the given resource to the deployment.
   *
   * @param classpathResource the path of the resource file in the classpath (e.g. "wf/process.bpmn"
   *     or "dmn/decision.dmn")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  DeployResourceCommandStep2 addResourceFromClasspath(String classpathResource);

  /**
   * Add the given resource to the deployment.
   *
   * @param filename the absolute path of the resource file (e.g. "~/wf/process.bpmn" or
   *     "~/dmn/decision.dmn")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  DeployResourceCommandStep2 addResourceFile(String filename);

  /**
   * Add the given process as resource to the deployment.
   *
   * @param processDefinition the process as model
   * @param resourceName the name of the resource (e.g. "process.bpmn")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  DeployResourceCommandStep2 addProcessModel(
      BpmnModelInstance processDefinition, String resourceName);

  interface DeployResourceCommandStep2
      extends DeployResourceCommand,
          CommandWithTenantStep<DeployResourceCommandStep2>,
          FinalCommandStep<DeployResourceCommandResponse> {
    // the place for new optional parameters
  }
}
