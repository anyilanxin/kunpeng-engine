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
package com.anyilanxin.modules.signal.controller;

import com.anyilanxin.core.AnYiBaseController;
import com.anyilanxin.core.AnYiResult;
import com.anyilanxin.modules.signal.controller.dto.SignalCorrelationDto;
import com.anyilanxin.modules.signal.service.ISignalService;
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
@Tag(name = "Signal", description = "信号相关")
@RequestMapping(value = "/signal")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SignalController extends AnYiBaseController {
  private final ISignalService service;

  @Operation(
      summary = "关联信号",
      tags = {"v1.0.0"},
      description = "关联信号")
  @PostMapping(value = "/create")
  public AnYiResult<String> signalCorrelation(@RequestBody @Valid final SignalCorrelationDto dto) {
    return ok(service.signalCorrelation(dto), "关联信号成功");
  }
}
