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
package com.anyilanxin.modules.message.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.modules.message.controller.dto.MessageCorrelationDto;
import com.anyilanxin.modules.message.service.IMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@Tag(name = "Message", description = "消息相关")
@RequestMapping(value = "/message")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MessageController extends AnYiBaseController {
  private final IMessageService service;

  @Operation(
      summary = "关联消息",
      tags = {"v1.0.0"},
      description = "关联消息")
  @PostMapping(value = "/create")
  public AnYiResult<String> messageCorrelation(
      @RequestBody @Valid final MessageCorrelationDto dto) {
    return ok(service.messageCorrelation(dto), "关联消息成功");
  }
}
