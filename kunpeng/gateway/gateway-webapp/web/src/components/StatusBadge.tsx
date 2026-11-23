/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import { Tag } from 'antd';

const STATE_COLOR: Record<string, string> = {
  SUCCEED: 'success',
  COMPLETED: 'success',
  EXECUTED: 'processing',
  EXECUTING: 'processing',
  WAIT: 'default',
  FAILED: 'error',
  PLAN_CREATED: 'cyan',
  PLAN_DELAYED: 'warning',
  PLAN_CREATING: 'cyan',
  COMPLETING: 'cyan',
};

/** 调度计划/执行明细状态标签 */
export function StateTag({ state }: { state: string }) {
  return <Tag color={STATE_COLOR[state] ?? 'default'}>{state}</Tag>;
}
