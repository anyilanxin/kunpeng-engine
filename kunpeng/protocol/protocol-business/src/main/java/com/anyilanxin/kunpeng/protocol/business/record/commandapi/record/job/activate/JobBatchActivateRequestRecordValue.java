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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.activate;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.RequestRecordValue;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;
import java.util.Collection;
import org.agrona.DirectBuffer;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface JobBatchActivateRequestRecordValue extends RequestRecordValue, JsonSerializable {
  JobBatchActivateRequestRecordValue setWorker(final String worker);

  JobBatchActivateRequestRecordValue setWorker(final DirectBuffer worker);

  JobBatchActivateRequestRecordValue setJobType(final String jobType);

  JobBatchActivateRequestRecordValue setJobType(final DirectBuffer jobType);

  JobBatchActivateRequestRecordValue setMaxJobsActivate(final int maxJobsActivate);

  JobBatchActivateRequestRecordValue setTimeout(final long timeout);

  JobBatchActivateRequestRecordValue setTenantIds(final Collection<String> tenantIds);
}
