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
package com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;

/**
 * 加签许可扩展（仅用户任务）：在 userTask 的 extensionElements 中声明 kunpeng:addSign 即允许该节点运行时加签；未声明 =
 * 禁止。cascade=是否允许级联加签（缺省 false，加签任务不可再加签）；maxDepth=级联深度上限（缺省 1，cascade 开启时生效）。
 */
public interface KunpengAddSign extends BpmnModelElementInstance {

  /** 是否允许级联加签：false=加签任务不可再加签（缺省），true=允许，受 maxDepth 限深 */
  Boolean getCascade();

  void setCascade(Boolean cascade);

  /** 级联层数上限：仅 cascade 开启时有意义，必须 >=1（解析期校验）；1=一级级联（缺省） */
  Integer getMaxDepth();

  void setMaxDepth(Integer maxDepth);
}
