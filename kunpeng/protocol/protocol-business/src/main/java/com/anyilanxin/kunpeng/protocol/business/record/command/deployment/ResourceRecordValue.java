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
package com.anyilanxin.kunpeng.protocol.business.record.command.deployment;

import com.anyilanxin.kunpeng.structpack.JsonSerializable;
import java.util.Map;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ResourceRecordValue extends JsonSerializable {
  String getResourceName();

  ResourceType getResourceType();

  byte[] getBytes();

  /** 业务扩展数据（资源级，随部署携带） */
  Map<String, Object> getExtendedData();
}
