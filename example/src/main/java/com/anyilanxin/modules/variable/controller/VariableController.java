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
package com.anyilanxin.modules.variable.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.modules.variable.controller.dto.VariableRemoveDto;
import com.anyilanxin.modules.variable.controller.dto.VariableUpdateDto;
import com.anyilanxin.modules.variable.service.IVariableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = " Variable", description = "变量变更")
@MessageMapping(value = "/variable")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class VariableController extends AnYiBaseController {
  private final IVariableService service;

  @Operation(
      summary = "添加或修改变量",
      tags = {"v1.0.0"},
      description = "添加或修改变量")
  @PostMapping(value = "/update")
  public AnYiResult<String> update(@RequestBody @Valid final VariableUpdateDto dto) {
    service.update(dto);
    return ok("变量变更成功");
  }

  @Operation(
      summary = "移除变量",
      tags = {"v1.0.0"},
      description = "移除变量")
  @PostMapping(value = "/remove")
  public AnYiResult<String> remove(@RequestBody @Valid final VariableRemoveDto dto) {
    service.remove(dto);
    return ok("变量删除成功");
  }
}
