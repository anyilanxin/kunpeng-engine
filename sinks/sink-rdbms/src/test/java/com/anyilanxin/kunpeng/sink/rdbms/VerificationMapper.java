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
package com.anyilanxin.kunpeng.sink.rdbms;

import java.util.List;
import java.util.Map;

/** 测试断言用的只读查询；语句见 test resources 的 mapper/test/Verify.xml。 */
public interface VerificationMapper {

  long totalRows();

  Map<String, Object> instanceRow();

  List<Map<String, Object>> variableRows();

  Map<String, Object> userTaskRow();

  Map<String, Object> activityInstanceRow();

  long sinkPositionRow();
}
