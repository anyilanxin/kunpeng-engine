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
package com.anyilanxin.kunpeng.protocol.admin;

/**
 * 管理协议 Record 映射索引号池。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AdminRecordMappingIndex {
  short RECORD_INDEX_0 = 0;
  short RECORD_INDEX_1 = 1;
  short RECORD_INDEX_2 = 2;
  short RECORD_INDEX_3 = 3;
  short RECORD_INDEX_4 = 4;
  short RECORD_INDEX_5 = 5;
  short RECORD_INDEX_6 = 6;
  short RECORD_INDEX_7 = 7;
  short RECORD_INDEX_8 = 8;
  short RECORD_INDEX_9 = 9;
  short RECORD_INDEX_10 = 10;
  short RECORD_INDEX_11 = 11;
  short RECORD_INDEX_12 = 12;
  short RECORD_INDEX_13 = 13;
  short RECORD_INDEX_14 = 14;
  short RECORD_INDEX_15 = 15;
  short RECORD_INDEX_16 = 16;
  short RECORD_INDEX_17 = 17;
  short RECORD_INDEX_18 = 18;
  short RECORD_INDEX_19 = 19;
  short RECORD_INDEX_20 = 20;
  short RECORD_INDEX_21 = 21;
  short RECORD_INDEX_22 = 22;
  short RECORD_INDEX_23 = 23;
  short RECORD_INDEX_24 = 24;
  short RECORD_INDEX_25 = 25;
  short RECORD_INDEX_26 = 26;

  static short size() {
    return RECORD_INDEX_26 + 1;
  }
}
