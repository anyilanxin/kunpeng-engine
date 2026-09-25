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
package com.anyilanxin.kunpeng.client.command.processinstance;

import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceOuterClass;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CreateProcessInstanceCommandResponseImpl
    implements CreateProcessInstanceCommandResponse {
  private final long processInstanceId;

  public CreateProcessInstanceCommandResponseImpl(
      final ProcessInstanceServiceOuterClass.CreateProcessInstanceResponse response) {
    processInstanceId = response.getProcessInstanceId();
  }

  @Override
  public long getProcessInstanceId() {
    return processInstanceId;
  }

  @Override
  public String getTenantId() {
    return "";
  }
}
