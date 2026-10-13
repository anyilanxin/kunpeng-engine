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
package com.anyilanxin.kunpeng.protocol.common;

/**
 * 表示归属于某个租户（Tenant）的实体。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface TenantOwned {

  /** 默认租户标识。未启用多租户时，实体归属该默认租户。这样做是为了兼容将来启用多租户的场景。 */
  String DEFAULT_TENANT_IDENTIFIER = "<default>";

  /** 返回拥有该实体的租户标识。 */
  String getTenantId();
}
