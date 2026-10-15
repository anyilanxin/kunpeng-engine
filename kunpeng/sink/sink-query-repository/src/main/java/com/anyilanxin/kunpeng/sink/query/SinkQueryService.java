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
package com.anyilanxin.kunpeng.sink.query;

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ProcessDefinitionRecordValue;
import java.util.Map;

/**
 * Sink 侧分区本地只读查询入口：直查本分区存储（RocksDB），不产生日志记录、不走命令链路。
 *
 * <p>线程安全：任意线程可调用（含 Sink 自建的工作线程），命中缓存的查询无锁。 返回实例视为只读视图：数据经缓存复用，跨调用持有或需要修改时请自行拷贝（与 {@code Record} 的
 * copyOf 约定同风格）；无匹配数据返回 {@code null}。
 *
 * <p>变量查询不经缓存（变量随流程执行变更），每次读取最新值，返回映射为当次调用独享。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface SinkQueryService {

  /**
   * 按流程定义 id 查询 BPMN 资源域的流程定义（含 BPMN 资源内容）。
   *
   * @param processDefinitionId 流程定义 id
   * @return 流程定义记录（只读视图，需持有/修改请自行拷贝）；本分区不存在该定义时返回 {@code null}
   */
  ProcessDefinitionRecordValue getProcessDefinition(final long processDefinitionId);

  /**
   * 查询流程实例（流程作用域）的全部变量。
   *
   * @param processInstanceId 流程实例 id
   * @return 变量名到值的映射（当次调用独享）；该实例尚无变量时返回空映射
   */
  Map<String, Object> getVariable(final long processInstanceId);

  /**
   * 查询某活动可见的变量：从活动自身作用域起沿父实例链向上合并，子作用域优先（父作用域只补齐未定义的键）， 最终并入流程作用域变量。
   *
   * @param processInstanceId 流程实例 id
   * @param activityInstanceId 活动实例 id
   * @return 变量名到值的映射（当次调用独享）；无可见变量时返回空映射
   */
  Map<String, Object> getVariable(final long processInstanceId, final long activityInstanceId);
}
