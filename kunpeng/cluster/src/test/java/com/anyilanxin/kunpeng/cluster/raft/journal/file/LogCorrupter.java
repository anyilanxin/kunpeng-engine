/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** 测试用日志损坏工具 */
public final class LogCorrupter {

  private LogCorrupter() {}

  /** 损坏指定索引处的记录；返回是否成功 */
  public static boolean corruptRecord(final File logFile, final long index) {
    try (final RandomAccessFile raf = new RandomAccessFile(logFile, "rw")) {
      if (index > raf.length()) {
        return false;
      }
      raf.seek(index);
      raf.write(0xFF);
      return true;
    } catch (final IOException e) {
      return false;
    }
  }

  /** 损坏文件头的描述符区域；返回是否成功 */
  public static boolean corruptDescriptor(final File segmentFile) {
    try (final RandomAccessFile raf = new RandomAccessFile(segmentFile, "rw")) {
      raf.seek(0);
      raf.write(0xFF);
      return true;
    } catch (final IOException e) {
      return false;
    }
  }
}
