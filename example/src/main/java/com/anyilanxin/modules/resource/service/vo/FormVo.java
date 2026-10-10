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
package com.anyilanxin.modules.resource.service.vo;

import com.anyilanxin.modules.resource.entity.FormEntity;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Schema
@AutoMappers({@AutoMapper(target = FormEntity.class)})
public class FormVo implements Serializable {
  @Serial private static final long serialVersionUID = -51611499087752128L;

  /** 表单 key */
  private String formKey;

  /** 表单 id */
  private String formId;

  /** 租户 id */
  private String tenantId;

  /** 表单元数据 */
  private String schema;

  /** 数据版本 */
  private String version;

  /** 是否删除 */
  private Integer isDeleted;
}
