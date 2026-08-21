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
package com.anyilanxin.kunpeng.utils;

/**
 * 关闭时不抛出受检异常的资源接口。
 *
 * <p>继承 {@link AutoCloseable} 并将 {@link #close()} 声明为不抛出任何受检异常， 资源可直接用于 try-with-resources
 * 语句而无需强制捕获关闭异常。 实现类应在内部消化关闭过程中出现的错误(如记录日志)，保证关闭操作对外始终安全。
 *
 * @author zxuanhong
 */
public interface CloseableSilently extends AutoCloseable {
  @Override
  void close();
}
