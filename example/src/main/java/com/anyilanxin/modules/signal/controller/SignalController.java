/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
