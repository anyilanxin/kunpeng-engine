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
package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import com.anyilanxin.kunpeng.sink.rdbms.model.UserTaskDbModel;
import org.apache.ibatis.annotations.Param;

/** 用户任务表语句（XML：mapper/UserTaskMapper.xml）。 */
public interface UserTaskMapper {

  int insert(UserTaskDbModel model);

  int update(UserTaskDbModel model);

  int cleanupHistory(@Param("processInstanceId") long processInstanceId);

  int removePartition(@Param("resourceId") int resourceId);

  int clearAll();
}
