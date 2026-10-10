/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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

package com.anyilanxin.kunpeng.engine.dmn;

import com.anyilanxin.kunpeng.engine.dmn.hitpolicy.handler.DmnHitPolicyLogger;
import com.anyilanxin.kunpeng.engine.dmn.util.BaseLogger;

public class DmnLogger extends BaseLogger {

  public static final String PROJECT_CODE = "DMN";
  public static final String PROJECT_LOGGER = "org.camunda.bpm.dmn";

  public static DmnEngineLogger ENGINE_LOGGER =
      createLogger(DmnEngineLogger.class, PROJECT_CODE, PROJECT_LOGGER, "01");

  public static DmnHitPolicyLogger HIT_POLICY_LOGGER =
      createLogger(DmnHitPolicyLogger.class, PROJECT_CODE, PROJECT_LOGGER + ".hitPolicy", "03");
}
