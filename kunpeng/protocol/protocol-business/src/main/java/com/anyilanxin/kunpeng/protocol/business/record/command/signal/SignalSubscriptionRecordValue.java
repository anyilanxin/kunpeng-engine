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
package com.anyilanxin.kunpeng.protocol.business.record.command.signal;

import com.anyilanxin.kunpeng.protocol.common.RecordValueWithVariables;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;

/** Represents a signal subscription event or command. */
public interface SignalSubscriptionRecordValue
    extends TenantOwned, RecordValueWithVariables, JsonSerializable {

  long getSignalSubscriptionId();

  String getSignalName();

  SignalSubscriptionType getSignalType();

  boolean isInterrupting();

  long getProcessDefinitionId();

  String getProcessDefinitionKey();

  long getProcessInstanceId();

  String getActivityDefinitionKey();

  long getActivityInstanceId();
}
