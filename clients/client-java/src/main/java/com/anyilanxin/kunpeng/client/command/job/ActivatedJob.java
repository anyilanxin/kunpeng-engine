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
package com.anyilanxin.kunpeng.client.command.job;

import com.anyilanxin.kunpeng.client.command.ClientException;
import com.anyilanxin.kunpeng.client.command.ListenerEventType;
import com.anyilanxin.kunpeng.client.command.UserTaskProperties;
import java.util.Map;

public interface ActivatedJob {

  /**
   * @return the unique key of the job
   */
  long getKey();

  /**
   * @return the type of the job
   */
  String getType();

  /**
   * @return key of the process instance
   */
  long getProcessInstanceId();

  /**
   * @return BPMN process id of the process
   */
  String getProcessDefinitionKey();

  /**
   * @return version of the process
   */
  int getProcessDefinitionVersion();

  /**
   * @return key of the process
   */
  long getProcessDefinitionId();

  /**
   * @return id of the process element
   */
  String getActivityDefinitionKey();

  /**
   * @return key of the element instance
   */
  long getActivityInstanceId();

  /**
   * @return user-defined headers associated with this job
   */
  Map<String, String> getCustomHeaders();

  /**
   * @return the assigned worker to complete the job
   */
  String getWorker();

  /**
   * @return remaining retries
   */
  int getRetries();

  /**
   * @return the unix timestamp until when the job is exclusively assigned to this worker (time unit
   *     * is milliseconds since unix epoch). If the deadline is exceeded, it can happen that the
   *     job is handed to another worker and the work is performed twice.
   */
  long getDeadline();

  /**
   * @return JSON-formatted variables
   */
  String getVariables();

  /**
   * @return de-serialized variables as map
   */
  Map<String, Object> getVariablesAsMap();

  /**
   * @return de-serialized variables as the given type
   */
  <T> T getVariablesAsType(Class<T> variableType);

  <T> T getVariablesAsType(String name, Class<T> variableType);

  /**
   * @return de-serialized variable value or null if the provided variable name is present among the
   *     available variables, otherwise throw a {@link ClientException}
   */
  Object getVariable(String name);

  /**
   * @return user task properties associated with this job. Present only if the job is of kind
   *     {@code TASK_LISTENER}; returns {@code null} for other job kinds such as {@code
   *     BPMN_ELEMENT} or {@code EXECUTION_LISTENER}.
   */
  UserTaskProperties getUserTask();

  /**
   * @return the kind of the job.
   */
  JobKind getKind();

  /**
   * @return the listener event type of the job.
   */
  ListenerEventType getListenerEventType();

  /**
   * @return the record encoded as JSON
   */
  String toJson();

  /**
   * @return the identifier of the tenant that owns the job
   */
  String getTenantId();
}
