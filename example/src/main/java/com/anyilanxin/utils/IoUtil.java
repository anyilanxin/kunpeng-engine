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
package com.anyilanxin.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author Tom Baeyens
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class IoUtil {

  public static byte[] readInputStream(
      final InputStream inputStream, final String inputStreamName) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final byte[] buffer = new byte[16 * 1024];
    try {
      int bytesRead = inputStream.read(buffer);
      while (bytesRead != -1) {
        outputStream.write(buffer, 0, bytesRead);
        bytesRead = inputStream.read(buffer);
      }
    } catch (final Exception e) {

    }
    return outputStream.toByteArray();
  }
}
