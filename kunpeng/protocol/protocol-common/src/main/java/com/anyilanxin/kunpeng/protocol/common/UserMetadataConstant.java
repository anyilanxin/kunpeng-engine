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
 * 授权元数据 map 的用户元数据标准键（对齐 Flowable IDM 用户模型：user + groups + tenant）。授权元数据整体 以 JSON
 * 编码随信封透传（网关构造、引擎消费、SDK 消费方构造），所有构造/解析方一律引用本常量，不写字面量； 定义在公共协议底座，业务面/管理面与外部消费方（SDK 消费侧）共用同一份。
 *
 * <p>键语义：
 *
 * <ul>
 *   <li>{@link #USER_ID}：操作人 id，必填（对齐 OAuth2 sub claim）；签收人/发起人/审批人判定依据
 *   <li>{@link #GROUPS}：操作人所属组 id 列表；候选审批组匹配 + admin 白名单
 *   <li>{@link #FIRST_NAME}/{@link #LAST_NAME}：姓名（审计/导出显示，不参与校验）
 *   <li>{@link #EMAIL}：邮箱（审计透传，不参与校验）
 *   <li>{@link #TENANT_ID}：所属租户 id（审计透传，不参与校验）
 * </ul>
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface UserMetadataConstant {

  /** 操作人 id（必填，OAuth2 sub claim 对齐） */
  String USER_ID = "userId";

  /** 操作人所属组 id 列表（候选审批组匹配 + admin 白名单） */
  String GROUPS = "groups";

  /** 操作人名（审计/导出显示） */
  String FIRST_NAME = "firstName";

  /** 操作人姓（审计/导出显示） */
  String LAST_NAME = "lastName";

  /** 操作人邮箱（审计透传） */
  String EMAIL = "email";

  /** 操作人所属租户 id（审计透传） */
  String TENANT_ID = "tenantId";
}
